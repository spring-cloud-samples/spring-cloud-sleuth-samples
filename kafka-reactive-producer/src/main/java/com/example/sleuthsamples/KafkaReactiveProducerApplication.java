package com.example.sleuthsamples;

import java.util.Map;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;
import reactor.kafka.sender.SenderRecord;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.instrument.kafka.TracingKafkaProducerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

@SpringBootApplication
public class KafkaReactiveProducerApplication implements CommandLineRunner {

	public static void main(String... args) {
		new SpringApplicationBuilder(KafkaReactiveProducerApplication.class).web(WebApplicationType.NONE).run(args);
	}

	@Autowired
	KafkaProducerService kafkaProducerService;

	@Override
	public void run(String... args) throws Exception {
		this.kafkaProducerService.call().blockFirst();
	}


	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Bean
		NewTopic myTopic() {
			return new NewTopic("mytopic2", 1, (short) 1);
		}

		@Bean KafkaSender<String, String> reactiveKafkaSender(@Value("${spring.kafka.bootstrap-servers:localhost:9092}") String servers, KafkaProperties properties, BeanFactory beanFactory) {
			Map<String, Object> map = properties.getProducer().buildProperties();
			map.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
			return KafkaSender.create(new TracingKafkaProducerFactory(beanFactory), SenderOptions.create(map));
		}
	}
}

@Service
class KafkaProducerService {
	private static final Logger log = LoggerFactory.getLogger(KafkaProducerService.class);

	private final KafkaSender<String, String> kafkaSender;

	private final Tracer tracer;

	KafkaProducerService(KafkaSender<String, String> kafkaSender, Tracer tracer) {
		this.kafkaSender = kafkaSender;
		this.tracer = tracer;
	}

	Flux<Object> call() {
		return Flux.just(this.tracer.nextSpan().name("reactive-kafka-producer"))
				.doOnNext(span -> this.tracer.withSpan(span.start()))
				.doOnNext(span -> log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from producer", this.tracer.currentSpan().context().traceId()))
				.flatMap(span -> kafkaSender.send(Mono.just(SenderRecord.create(new ProducerRecord<>("mytopic2", "hello"), null)))
						.doOnComplete(() -> {
							log.info("Sent message");
							span.end();
						}).doOnError(throwable -> {
							log.info("Failed to send a message", throwable);
							span.end();
						}));
	}
}
