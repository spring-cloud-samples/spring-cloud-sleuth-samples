package com.example.sleuthsamples;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.SocketUtils;

// Uncomment the properties to rebuild the projects
// @formatter:off
@SpringBootTest(
//		properties = {"spring.cloud.sleuth.samples.rebuild-projects=true", "spring.cloud.sleuth.samples.project-root=${user.home}/repo/spring-cloud-sleuth-samples", "maven.home=${user.home}/.sdkman/candidates/maven/current"}
)
@Testcontainers
// @formatter:on
class RabbitAcceptanceTests extends AcceptanceTestsBase {

	@Container
	static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.7.25-management-alpine");

	@Test
	void should_pass_tracing_context_from_stream_producer_to_consumer(TestInfo testInfo) {
		// given
		String consumerId = deploy(testInfo, "stream-consumer", rabbitMqPort());

		// when
		String producerId = deploy(testInfo, "stream-producer", rabbitMqPort());

		// then
		assertThatTraceIdGotPropagated(producerId, consumerId);
	}

	@Test
	void should_pass_tracing_context_from_stream_reactive_producer_to_reactive_consumer(TestInfo testInfo) {
		// given
		String consumerId = deploy(testInfo, "stream-reactive-consumer", rabbitMqPort());

		// when
		String producerId = deploy(testInfo, "stream-reactive-producer", rabbitMqPort());

		// then
		assertThatTraceIdGotPropagated(producerId, consumerId);
	}

	private Map<String, String> rabbitMqPort() {
		return Map.of("spring.rabbitmq.port", rabbit.getAmqpPort().toString());
	}

}
