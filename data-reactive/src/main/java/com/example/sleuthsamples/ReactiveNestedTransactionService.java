package com.example.sleuthsamples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import org.springframework.cloud.sleuth.Tracer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReactiveNestedTransactionService {

	private static final Logger log = LoggerFactory.getLogger(ReactiveNestedTransactionService.class);

	private final Tracer tracer;

	public ReactiveNestedTransactionService(Tracer tracer) {
		this.tracer = tracer;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public Mono<Void> requiresNew() {
		return Mono.fromRunnable(() -> log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from consumer requires new", tracer.currentSpan().context().traceId()))
				.then();
	}
}
