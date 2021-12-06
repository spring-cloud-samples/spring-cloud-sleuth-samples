package com.example.sleuthsamples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class MvcApplication {

	public static void main(String... args) {
		new SpringApplication(MvcApplication.class).run(args);
	}
}

@RestController
class MvcController {
	private static final Logger log = LoggerFactory.getLogger(MvcController.class);

	private final Tracer tracer;

	MvcController(Tracer tracer) {
		this.tracer = tracer;
	}

	@GetMapping("/")
	public String span() {
		String traceId = this.tracer.currentSpan().context().traceId();
		log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from producer", traceId);
		return traceId;
	}
}
