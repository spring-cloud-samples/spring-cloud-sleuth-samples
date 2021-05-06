package com.example.sleuthsamples;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.SpanAndScope;
import org.springframework.cloud.sleuth.ThreadLocalSpan;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

@SpringBootApplication
public class DataApplication {

	private static final Logger log = LoggerFactory.getLogger(DataApplication.class);

	public static void main(String... args) {
		new SpringApplicationBuilder(DataApplication.class).web(WebApplicationType.NONE).run(args);
	}

	@Bean
	TracePlatformTransactionManagerBeanPostProcessor tracePlatformTransactionManagerBeanPostProcessor(BeanFactory beanFactory) {
		return new TracePlatformTransactionManagerBeanPostProcessor(beanFactory);
	}

	@Bean
	public CommandLineRunner demo(MyService myService) {
		return (args) -> {
			try {
				myService.foo();
			}
			catch (Exception e) {
				log.info("Expected to throw an exception so that we see if rollback works");
			}
		};
	}
}


class TracePlatformTransactionManagerBeanPostProcessor implements BeanPostProcessor {
	private final BeanFactory beanFactory;

	TracePlatformTransactionManagerBeanPostProcessor(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof PlatformTransactionManager && !(bean instanceof TracePlatformTransactionManager)) {
			return new TracePlatformTransactionManager((PlatformTransactionManager) bean, this.beanFactory);
		}
		return bean;
	}
}

class TracePlatformTransactionManager implements PlatformTransactionManager {

	private static final Log log = LogFactory.getLog(TracePlatformTransactionManager.class);

	private final PlatformTransactionManager delegate;

	private final BeanFactory beanFactory;

	private Tracer tracer;

	private ThreadLocalSpan threadLocalSpan;

	// TODO: This looks bad
	private static final Map<Thread, AtomicInteger> txCounter = new ConcurrentHashMap<>();

	TracePlatformTransactionManager(PlatformTransactionManager delegate, BeanFactory beanFactory) {
		this.delegate = delegate;
		this.beanFactory = beanFactory;
	}

	@Override
	public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
		if (this.threadLocalSpan == null) {
			this.threadLocalSpan = new ThreadLocalSpan(tracer());
		}
		AtomicInteger counter = txCounter.computeIfAbsent(Thread.currentThread(), thread -> new AtomicInteger(-1));
		SpanAndScope spanAndScope = this.threadLocalSpan.get();
		Span currentSpan = spanAndScope != null ? spanAndScope.getSpan() : tracer().currentSpan();
		Span span = tracer().nextSpan().name("tx").start();
		try {
			TransactionDefinition def = (definition != null ? definition : TransactionDefinition.withDefaults());
			TransactionStatus status = this.delegate.getTransaction(definition);
			span = taggedSpan(currentSpan, span, def, status);
			if (StringUtils.hasText(def.getName())) {
				int newCounter = counter.incrementAndGet();
				span.tag("tx.name[" + newCounter + "]", def.getName());
				// TODO: This looks bad
				txCounter.put(Thread.currentThread(), new AtomicInteger(newCounter));
			}
			return status;
		}
		catch (Exception e) {
			if (log.isDebugEnabled()) {
				log.debug("Exception occurred while trying to get a transaction, will mark the span with error and report it");
			}
			span.error(e);
			span.end();
			throw e;
		}
	}

	private Span taggedSpan(Span currentSpan, Span span, TransactionDefinition def, TransactionStatus status) {
		if (status.isNewTransaction() || currentSpan == null) {
			log.info("Creating new span cause a new transaction is started");
			this.threadLocalSpan.set(span);
			span.tag("tx.transaction-manager", ClassUtils.getQualifiedName(this.delegate.getClass()));
			span.tag("tx.read-only", String.valueOf(def.isReadOnly()));
			span.tag("tx.propagation-level", propagationLevel(def));
			span.tag("tx.isolation-level", isolationLevel(def));
			if (def.getTimeout() > 0) {
				span.tag("tx.timeout", String.valueOf(def.getTimeout()));
			}
		}
		else {
			span = currentSpan;
		}
		return span;
	}

	private String propagationLevel(TransactionDefinition def) {
		switch (def.getPropagationBehavior()) {
		case 0:
			return "PROPAGATION_REQUIRED";
		case 1:
			return "PROPAGATION_SUPPORTS";
		case 2:
			return "PROPAGATION_MANDATORY";
		case 3:
			return "PROPAGATION_REQUIRES_NEW";
		case 4:
			return "PROPAGATION_NOT_SUPPORTED";
		case 5:
			return "PROPAGATION_NEVER";
		case 6:
			return "PROPAGATION_NESTED";
		default:
			return String.valueOf(def.getPropagationBehavior());
		}
	}

	private String isolationLevel(TransactionDefinition def) {
		switch (def.getIsolationLevel()) {
		case -1:
			return "ISOLATION_DEFAULT";
		case 1:
			return "ISOLATION_READ_UNCOMMITTED";
		case 2:
			return "ISOLATION_READ_COMMITTED";
		case 4:
			return "ISOLATION_REPEATABLE_READ";
		case 8:
			return "ISOLATION_SERIALIZABLE";
		default:
			return String.valueOf(def.getIsolationLevel());
		}
	}

	@Override
	public void commit(TransactionStatus status) throws TransactionException {
		SpanAndScope spanAndScope = this.threadLocalSpan.get();
		if (spanAndScope == null) {
			if (log.isDebugEnabled()) {
				log.debug("No span and scope found - this shouldn't happen, sth is wrong");
			}
			this.delegate.commit(status);
			return;
		}
		Exception ex = null;
		Span span = spanAndScope.getSpan();
		try {
			if (log.isDebugEnabled()) {
				log.debug("Wrapping commit");
			}
			this.delegate.commit(status);
		}
		catch (Exception e) {
			ex = e;
			span.error(e);
			throw e;
		}
		finally {
			span.event("tx commit");
			span.end();
			if (ex == null) {
				if (log.isDebugEnabled()) {
					log.debug("No exception was found - will clear thread local span");
				}
				this.threadLocalSpan.remove();
				// TODO: This looks bad
				txCounter.put(Thread.currentThread(), new AtomicInteger(-1));
			}
		}
	}

	@Override
	public void rollback(TransactionStatus status) throws TransactionException {
		SpanAndScope spanAndScope = this.threadLocalSpan.get();
		if (spanAndScope == null) {
			if (log.isDebugEnabled()) {
				log.debug("No span and scope found - this shouldn't happen, sth is wrong");
			}
			this.delegate.rollback(status);
			return;
		}
		Span span = spanAndScope.getSpan();
		try {
			if (log.isDebugEnabled()) {
				log.debug("Wrapping rollback");
			}
			this.delegate.rollback(status);
		}
		catch (Exception e) {
			span.error(e);
			throw e;
		}
		finally {
			span.event("tx rollback");
			span.end();
			this.threadLocalSpan.remove();
			// TODO: This looks bad
			txCounter.put(Thread.currentThread(), new AtomicInteger(-1));
		}
	}

	private Tracer tracer() {
		if (this.tracer == null) {
			this.tracer = this.beanFactory.getBean(Tracer.class);
		}
		return this.tracer;
	}
}
