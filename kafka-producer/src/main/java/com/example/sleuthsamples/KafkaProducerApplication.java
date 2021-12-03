package com.example.sleuthsamples;

import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

@SpringBootApplication
public class KafkaProducerApplication implements CommandLineRunner {

	public static void main(String... args) {
		new SpringApplicationBuilder(KafkaProducerApplication.class).web(WebApplicationType.NONE).run(args);
	}

	@Autowired
	KafkaProducerService kafkaProducerService;

	@Override
	public void run(String... args) throws Exception {
		this.kafkaProducerService.call();
	}

	@Bean
	NewTopic myTopic() {
		return new NewTopic("mytopic", 1, (short) 1);
	}
}

@Service
class KafkaProducerService {
	private static final Logger log = LoggerFactory.getLogger(KafkaProducerService.class);

	private final KafkaTemplate<String, String> kafkaTemplate;

	private final Tracer tracer;

	KafkaProducerService(KafkaTemplate<String, String> kafkaTemplate, Tracer tracer) {
		this.kafkaTemplate = kafkaTemplate;
		this.tracer = tracer;
	}

	void call() throws ExecutionException, InterruptedException {
		Span span = this.tracer.nextSpan().name("kafka-producer");
		try (Tracer.SpanInScope ws = this.tracer.withSpan(span.start())) {
			log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from producer", this.tracer.currentSpan().context().traceId());
			ListenableFuture<SendResult<String, String>> future =
					kafkaTemplate.send("mytopic", "hello");
			future.addCallback(new ListenableFutureCallback<>() {

				@Override
				public void onSuccess(SendResult<String, String> result) {
					log.info("Sent <{}>", result);
					span.end();
				}

				@Override
				public void onFailure(Throwable ex) {
					log.info("Failed to send a message", ex);
					span.end();
				}
			});
			// Blocking to ensure that we push all the spans
			future.get();
		}
	}
}
