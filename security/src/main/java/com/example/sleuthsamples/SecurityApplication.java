package com.example.sleuthsamples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.switchuser.SwitchUserFilter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class SecurityApplication {

	public static void main(String... args) {
		new SpringApplication(SecurityApplication.class).run(args);
	}
}

@RestController
class MvcController {

	@GetMapping("/")
	public String span() {
		return "hello";
	}
}

// For testing
@Configuration(proxyBeanMethods = false)
class MySecurityFilter {

	private static final Logger log = LoggerFactory.getLogger(MySecurityFilter.class);

	private final Tracer tracer;

	MySecurityFilter(Tracer tracer) {
		this.tracer = tracer;
	}

	@Bean
	SecurityFilterChain mySecurityFilterChain(HttpSecurity http) throws Exception {
		return http.addFilterAfter((servletRequest, servletResponse, filterChain) -> {
			String traceId = tracer.currentSpan().context().traceId();
			log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from producer", traceId);
			filterChain.doFilter(servletRequest, servletResponse);
		}, SwitchUserFilter.class).build();
	}
}
