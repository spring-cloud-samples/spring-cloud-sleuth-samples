package com.example.sleuthsamples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class SecurityWebFluxApplication {

	public static void main(String... args) {
		new SpringApplication(SecurityWebFluxApplication.class).run(args);
	}
}

@RestController
class WebFluxController {

	private static final Logger log = LoggerFactory.getLogger(WebFluxController.class);

	private final Tracer tracer;

	WebFluxController(Tracer tracer) {
		this.tracer = tracer;
	}

	@RequestMapping("/")
	public Mono<String> span() {
		String traceId = tracer.currentSpan().context().traceId();
		log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from producer", traceId);
		return Mono.just("Hello");
	}
}

// For testing - I can't make this work
// @Configuration(proxyBeanMethods = false)
class MySecurityFilter {

	private static final Logger log = LoggerFactory.getLogger(MySecurityFilter.class);

	private final Tracer tracer;

	MySecurityFilter(Tracer tracer) {
		this.tracer = tracer;
	}

	@Bean
	SecurityWebFilterChain mySecurityFilterChain(ServerHttpSecurity http) throws Exception {
		return http.addFilterAfter((exchange, chain) -> {
			String traceId = tracer.currentSpan().context().traceId();
			log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from producer", traceId);
			return chain.filter(exchange);
		}, SecurityWebFiltersOrder.FIRST).build();
	}
}
