package com.example.sleuthsamples;

import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;

@SpringBootApplication
@EnableKafka
public class KafkaConsumerApplication implements CommandLineRunner {

	public static void main(String... args) {
		new SpringApplicationBuilder(KafkaConsumerApplication.class).web(WebApplicationType.NONE).run(args);
	}

	@Override
	public void run(String... args) throws Exception {

	}

	@Bean
	NewTopic myTopic() {
		return new NewTopic("mytopic", 1, (short) 1);
	}

	@Bean
	MyKafkaListener myKafkaListener(Tracer tracer) {
		return new MyKafkaListener(tracer);
	}
}

class MyKafkaListener {

	private static final Logger log = LoggerFactory.getLogger(MyKafkaListener.class);

	private final Tracer tracer;

	MyKafkaListener(Tracer tracer) {
		this.tracer = tracer;
	}

	@KafkaListener(topics = "mytopic", groupId = "group")
	void onMessage(String message) {
		log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from consumer", tracer.currentSpan().context().traceId());
		log.info("Got message <{}>", message);
	}

}
