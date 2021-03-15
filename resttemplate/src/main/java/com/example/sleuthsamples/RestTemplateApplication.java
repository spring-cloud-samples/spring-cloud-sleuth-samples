package com.example.sleuthsamples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class RestTemplateApplication implements CommandLineRunner {

	public static void main(String... args) {
		new SpringApplicationBuilder(RestTemplateApplication.class).web(WebApplicationType.NONE).run(args);
	}

	@Autowired
	RestTemplateService restTemplateService;

	@Value("${url:http://localhost:7100}")
	String url;

	@Override
	public void run(String... args) throws Exception {
		this.restTemplateService.call(url);
	}
}

@Configuration
class Config {
	// You must register RestTemplate as a bean!
	@Bean
	RestTemplate restTemplate() {
		return new RestTemplate();
	}
}

@Service
class RestTemplateService {
	private static final Logger log = LoggerFactory.getLogger(RestTemplateService.class);

	private final RestTemplate restTemplate;

	private final Tracer tracer;

	RestTemplateService(RestTemplate restTemplate, Tracer tracer) {
		this.restTemplate = restTemplate;
		this.tracer = tracer;
	}

	String call(String url) {
		Span span = this.tracer.nextSpan();
		try (Tracer.SpanInScope ws = this.tracer.withSpan(span.start())) {
			log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from service", this.tracer.currentSpan().context().traceId());
			return this.restTemplate.getForObject(url, String.class);
		} finally {
			span.end();
		}
	}
}