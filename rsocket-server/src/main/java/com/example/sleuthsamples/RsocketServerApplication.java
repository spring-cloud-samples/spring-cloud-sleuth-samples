package com.example.sleuthsamples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class RsocketServerApplication {

	public static void main(String... args) {
		new SpringApplication(RsocketServerApplication.class).run(args);
	}
}

@Controller
class RSocketController {
	private static final Logger log = LoggerFactory.getLogger(RSocketController.class);

	private final Tracer tracer;

	RSocketController(Tracer tracer) {
		this.tracer = tracer;
	}

	@MessageMapping("foo")
	public Mono<String> span() {
		String traceId = this.tracer.currentSpan().context().traceId();
		log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from consumer", traceId);
		return Mono.just(traceId);
	}
}
