package com.example.sleuthsamples;

import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class StreamReactiveProducerApplication implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(StreamReactiveProducerApplication.class);

	public static void main(String... args) {
		new SpringApplicationBuilder(StreamReactiveProducerApplication.class).web(WebApplicationType.NONE).run(args);
	}

	@Autowired
	StreamBridgeService streamBridgeService;

	@Override
	public void run(String... args) throws Exception {
		log.warn("Remember about setting <spring.cloud.sleuth.integration.enabled=true> property for Stream Reactive and Sleuth to work");
		this.streamBridgeService.call();
	}

	@Bean
	Function<Flux<String>, Flux<String>> channel(Tracer tracer) {
		return s -> s.doOnNext(i -> log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from producer", tracer.currentSpan().context().traceId()));
	}
}

@Service
class StreamBridgeService {
	private static final Logger log = LoggerFactory.getLogger(StreamBridgeService.class);

	private final StreamBridge streamBridge;

	private final Tracer tracer;

	StreamBridgeService(StreamBridge streamBridge, Tracer tracer) {
		this.streamBridge = streamBridge;
		this.tracer = tracer;
	}

	void call() {
		Span span = this.tracer.nextSpan();
		try (Tracer.SpanInScope ws = this.tracer.withSpan(span.start())) {
			log.info("Hello from stream bridge - trace <{}>", this.tracer.currentSpan().context().traceId());
			this.streamBridge.send("channel-in-0", "HELLO");
		} finally {
			span.end();
		}
	}
}