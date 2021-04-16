package com.example.sleuthsamples;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.stereotype.Service;

@SpringBootApplication
public class ReactiveCircuitBreakerApplication implements CommandLineRunner {

	public static void main(String... args) {
		new SpringApplicationBuilder(ReactiveCircuitBreakerApplication.class).web(WebApplicationType.NONE).run(args);
	}

	@Autowired
	CircuitService circuitService;

	@Override
	public void run(String... args) throws Exception {
		this.circuitService.call().block(Duration.ofSeconds(1));
	}
}

@Service
class CircuitService {
	private static final Logger log = LoggerFactory.getLogger(CircuitService.class);

	private final ReactiveCircuitBreakerFactory factory;

	private final Tracer tracer;

	CircuitService(ReactiveCircuitBreakerFactory factory, Tracer tracer) {
		this.factory = factory;
		this.tracer = tracer;
	}

	Mono<String> call() {
		Span span = this.tracer.nextSpan();
		try (Tracer.SpanInScope ws = this.tracer.withSpan(span.start())) {
			return this.factory.create("circuit").run(Mono.defer(() -> {
				log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from consumer", this.tracer.currentSpan().context().traceId());
				return Mono.error(new IllegalStateException("BOOM"));
			}), throwable -> {
				log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from producer", this.tracer.currentSpan().context().traceId());
				return Mono.just("fallback");
			});
		}
		finally {
			span.end();
		}
	}
}