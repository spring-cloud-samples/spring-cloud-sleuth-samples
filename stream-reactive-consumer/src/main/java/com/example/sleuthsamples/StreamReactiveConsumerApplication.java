package com.example.sleuthsamples;

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class StreamReactiveConsumerApplication implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(StreamReactiveConsumerApplication.class);

	public static void main(String... args) {
		new SpringApplicationBuilder(StreamReactiveConsumerApplication.class).web(WebApplicationType.NONE).run(args);
	}

	@Override
	public void run(String... args) throws Exception {
		log.warn("Remember about setting <spring.cloud.sleuth.integration.enabled=true> property for Stream Reactive and Sleuth to work");
		log.warn("Remember about calling <.subscribe()> at the end of your Consumer<Flux> bean!");
	}

	@Bean
	Consumer<Flux<String>> channel(Tracer tracer) {
		// For the reactive consumer remember to call "subscribe()" at the end, otherwise
		// you'll get the "Dispatcher has no subscribers" error
		return i -> i.doOnNext(s ->
				log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from consumer", tracer.currentSpan().context().traceId())).subscribe();
	}
}