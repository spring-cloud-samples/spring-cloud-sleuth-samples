package com.example.sleuthsamples;

import java.time.Duration;
import java.util.Map;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.sleuth.CurrentTraceContext;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.TraceContext;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.Tracer.SpanInScope;
import org.springframework.cloud.sleuth.instrument.reactor.ReactorSleuth;
import org.springframework.context.annotation.Bean;

import com.mongodb.RequestContext;
import com.mongodb.event.CommandListener;
import com.mongodb.event.CommandSucceededEvent;

import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

@SpringBootApplication
public class ReactiveMongoApplication {

	private static final Logger log = LoggerFactory.getLogger(ReactiveMongoApplication.class);

	public static void main(String... args) {
		new SpringApplicationBuilder(ReactiveMongoApplication.class).web(WebApplicationType.NONE).run(args);
	}

	@Bean
	public CommandLineRunner demo(BasicUserRepository basicUserRepository, Tracer tracer) {
		return (args) -> {
			Span nextSpan = tracer.nextSpan().name("mongo-reactive-app");
			Mono.just(nextSpan).doOnNext(span -> tracer.withSpan(nextSpan.start())).flatMap(span -> {
				log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from consumer",
						tracer.currentSpan().context().traceId());
				return basicUserRepository.save(new User("foo", "bar", "baz", null))
						.flatMap(user -> basicUserRepository.findUserByUsername("foo"));
			}).contextWrite(context -> context.put(Span.class, nextSpan).put(TraceContext.class, nextSpan.context()))
					.doFinally(signalType -> nextSpan.end()).block(Duration.ofMinutes(1));
		};
	}

	// This is for tests only. You don't need this in your production code.
	@Bean
	public MongoClientSettingsBuilderCustomizer testMongoClientSettingsBuilderCustomizer(Tracer tracer,
			CurrentTraceContext currentTraceContext) {
		return new TestMongoClientSettingsBuilderCustomizer(tracer, currentTraceContext);
	}

}

class TestMongoClientSettingsBuilderCustomizer implements MongoClientSettingsBuilderCustomizer {

	private static final Logger log = LoggerFactory.getLogger(TestMongoClientSettingsBuilderCustomizer.class);

	private final Tracer tracer;
	
	private final CurrentTraceContext currentTraceContext;
	
	public TestMongoClientSettingsBuilderCustomizer(Tracer tracer, CurrentTraceContext currentTraceContext) {
		this.tracer = tracer;
		this.currentTraceContext = currentTraceContext;
	}

	@Override
	public void customize(com.mongodb.MongoClientSettings.Builder clientSettingsBuilder) {
		clientSettingsBuilder.addCommandListener(new CommandListener() {

			@Override
			public void commandSucceeded(CommandSucceededEvent event) {
				RequestContext requestContext = event.getRequestContext();
				if (requestContext == null) {
					return;
				}
				Span parent = ReactorSleuth.spanFromContext(tracer, currentTraceContext, context(requestContext));
				try (SpanInScope withSpan = tracer.withSpan(parent)) {
					log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from producer",
							tracer.currentSpan().context().traceId());
				}
			}
		});
	}

	private ContextView context(RequestContext requestContext) {
		return new ContextView() {
			@Override
			public <T> T get(Object key) {
				return requestContext.get(key);
			}

			@Override
			public <T> T getOrDefault(Object key, T defaultValue) {
				return requestContext.getOrDefault(key, defaultValue);
			}

			@Override
			public boolean hasKey(Object key) {
				return requestContext.hasKey(key);
			}

			@Override
			public int size() {
				return requestContext.size();
			}

			@Override
			public Stream<Map.Entry<Object, Object>> stream() {
				return requestContext.stream();
			}
		};
	}

}
