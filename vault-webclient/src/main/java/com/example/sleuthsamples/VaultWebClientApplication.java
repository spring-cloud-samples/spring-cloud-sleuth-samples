package com.example.sleuthsamples;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

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
import org.springframework.vault.client.WebClientCustomizer;
import org.springframework.vault.core.ReactiveVaultTemplate;
import org.springframework.vault.support.VaultResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootApplication
public class VaultWebClientApplication implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(VaultWebClientApplication.class);

	public static void main(String... args) {
		new SpringApplicationBuilder(VaultWebClientApplication.class).web(WebApplicationType.NONE).run(args);
	}

	@Autowired
	WebClientService webClientService;

	@Override
	public void run(String... args) throws Exception {
		this.webClientService.call().block(Duration.ofSeconds(5));
		// To ensure that the spans got successfully reported
		Thread.sleep(500);
	}

	@Bean
	WebClientCustomizer testWebClientCustomizer(Tracer tracer) {
		return webClientBuilder -> webClientBuilder.filter((request, next) -> {
			log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from producer", tracer.currentSpan().context().traceId());
			return next.exchange(request);
		});
	}
}

@Service
class WebClientService {
	private static final Logger log = LoggerFactory.getLogger(WebClientService.class);

	private final ReactiveVaultTemplate reactiveVaultTemplate;

	private final Tracer tracer;

	WebClientService(ReactiveVaultTemplate reactiveVaultTemplate, Tracer tracer) {
		this.reactiveVaultTemplate = reactiveVaultTemplate;
		this.tracer = tracer;
	}

	Mono<VaultResponse> call() {
		Span nextSpan = this.tracer.nextSpan().name("client");
		return Mono.just(nextSpan)
				.doOnNext(span -> this.tracer.withSpan(span.start()))
				.flatMap(span -> {
					log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from consumer", this.tracer.currentSpan().context().traceId());
					return this.reactiveVaultTemplate.read("/secrets/foo");
				})
				.doFinally(signalType -> nextSpan.end());
	}
}
