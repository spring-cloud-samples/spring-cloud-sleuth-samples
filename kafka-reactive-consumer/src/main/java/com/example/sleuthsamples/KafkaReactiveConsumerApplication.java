package com.example.sleuthsamples;

import java.util.Collections;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.SpanAndScope;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.instrument.kafka.TracingKafkaConsumerFactory;
import org.springframework.cloud.sleuth.propagation.Propagator;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class KafkaReactiveConsumerApplication {

	public static void main(String... args) {
		new SpringApplicationBuilder(KafkaReactiveConsumerApplication.class).web(WebApplicationType.NONE).run(args);
	}

	@Bean
	NewTopic myTopic() {
		return new NewTopic("mytopic2", 1, (short) 1);
	}

	@Bean
	MyKafkaListener myKafkaListener(Tracer tracer, KafkaReceiver<String, String> kafkaReceiver, Propagator.Getter<ConsumerRecord<?, ?>> extractor, Propagator propagator) {
		return new MyKafkaListener(tracer, kafkaReceiver, extractor, propagator);
	}

	@Bean
	ReceiverOptions<String, String> kafkaReceiverOptions(KafkaProperties kafkaProperties) {
		ReceiverOptions<String, String> basicReceiverOptions = ReceiverOptions.create(kafkaProperties.buildConsumerProperties());
		return basicReceiverOptions.subscription(Collections.singletonList("mytopic2"));
	}

	@Bean
	KafkaReceiver<String, String> reactiveKafkaReceiver(TracingKafkaConsumerFactory tracingKafkaConsumerFactory, ReceiverOptions kafkaReceiverOptions) {
		return KafkaReceiver.create(tracingKafkaConsumerFactory, kafkaReceiverOptions);
	}

	@Bean
	MyRunner myRunner(MyKafkaListener myKafkaListener) {
		return new MyRunner(myKafkaListener);
	}
}

class MyRunner implements CommandLineRunner {

	private final MyKafkaListener myKafkaListener;

	MyRunner(MyKafkaListener myKafkaListener) {
		this.myKafkaListener = myKafkaListener;
	}

	@Override
	public void run(String... args) throws Exception {
		this.myKafkaListener.onMessage().subscribe();
	}
}

class MyKafkaListener {

	private static final Logger log = LoggerFactory.getLogger(MyKafkaListener.class);

	private final Tracer tracer;

	private final KafkaReceiver<String, String> kafkaReceiver;

	// for putting a span in scope
	private final Propagator.Getter<ConsumerRecord<?, ?>> extractor;

	// for putting a span in scope
	private final Propagator propagator;

	MyKafkaListener(Tracer tracer, KafkaReceiver<String, String> kafkaReceiver, Propagator.Getter<ConsumerRecord<?, ?>> extractor, Propagator propagator) {
		this.tracer = tracer;
		this.kafkaReceiver = kafkaReceiver;
		this.extractor = extractor;
		this.propagator = propagator;
	}

	Flux<String> onMessage() {
		return kafkaReceiver
				.receiveAutoAck()
				.flatMap(consumerRecord -> consumerRecord)
				// for putting a span in scope
				.flatMap(consumerRecord -> {
					Span.Builder builder = propagator.extract(consumerRecord, extractor);
					Span childSpan = builder.name("on-message").start();
					SpanAndScope spanAndScope = new SpanAndScope(childSpan, tracer.withSpan(childSpan));
					return Flux.just(Tuples.of(consumerRecord.value(), spanAndScope))
							.doOnNext(consRecord -> log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from consumer", tracer.currentSpan().context().traceId())) // for tests - you don't need this in your code
							.doOnNext(objects -> log.info("Got message <{}>", objects.getT1()))
							.doOnError(throwable -> log.error("Exception occurred", throwable))
							.doFinally(signalType -> spanAndScope.close());
				})
				.map(Tuple2::getT1);

	}

}
