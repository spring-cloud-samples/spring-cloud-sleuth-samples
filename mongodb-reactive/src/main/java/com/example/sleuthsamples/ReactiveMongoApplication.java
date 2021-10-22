package com.example.sleuthsamples;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.TraceContext;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.context.annotation.Bean;

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
			Mono.just(nextSpan)
					.doOnNext(span -> tracer.withSpan(nextSpan.start()))
				.flatMap(span -> {
					log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from producer", tracer.currentSpan().context().traceId());
					return basicUserRepository.save(new User("foo", "bar", "baz", null))
							.flatMap(user -> basicUserRepository.findUserByUsername("foo"));
				})
				.contextWrite(context -> context.put(Span.class, nextSpan).put(TraceContext.class, nextSpan.context()))
				.doFinally(signalType -> nextSpan.end()).block(Duration.ofMinutes(1));
		};
	}
}
