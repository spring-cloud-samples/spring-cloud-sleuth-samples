package com.example.sleuthsamples;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.SocketUtils;

// Uncomment the properties to rebuild the projects
// @formatter:off
@SpringBootTest(
//		properties = {"spring.cloud.sleuth.samples.rebuild-projects=true", "spring.cloud.sleuth.samples.project-root=${user.home}/repo/spring-cloud-sleuth-samples", "maven.home=${user.home}/.sdkman/candidates/maven/current"}
)
// @formatter:on
class AcceptanceTests extends AcceptanceTestsBase {

	@Test
	void should_pass_tracing_context_from_rest_template_to_mvc(TestInfo testInfo) {
		// given
		int port = SocketUtils.findAvailableTcpPort();
		String producerId = waitUntilStarted(() -> deployWebApp(testInfo, "mvc", port));

		// when
		String consumerId = deploy(testInfo, "resttemplate", Map.of("url", "http://localhost:" + port));

		// then
		assertThatTraceIdGotPropagated(producerId, consumerId);
	}

	@Test
	void should_pass_tracing_context_from_web_client_to_webflux(TestInfo testInfo) {
		// given
		int port = SocketUtils.findAvailableTcpPort();
		String producerId = waitUntilStarted(() -> deployWebApp(testInfo, "webflux", port));

		// when
		String consumerId = deploy(testInfo, "webclient", Map.of("url", "http://localhost:" + port));

		// then
		assertThatTraceIdGotPropagated(producerId, consumerId);
	}

	@Test
	void should_pass_tracing_context_from_openfeign_to_mvc(TestInfo testInfo) {
		// given
		int port = SocketUtils.findAvailableTcpPort();
		String producerId = waitUntilStarted(() -> deployWebApp(testInfo, "mvc", port));

		// when
		String consumerId = deploy(testInfo, "openfeign", Map.of("url", "http://localhost:" + port));

		// then
		assertThatTraceIdGotPropagated(producerId, consumerId);
	}

	@Test
	void should_pass_tracing_context_from_gateway_to_mvc(TestInfo testInfo) {
		// given
		int port = SocketUtils.findAvailableTcpPort();
		String producerId = waitUntilStarted(() -> deployWebApp(testInfo, "mvc", port));

		// when
		String consumerId = deploy(testInfo, "gateway", Map.of("url", "http://localhost:" + port));

		// then
		assertThatTraceIdGotPropagated(producerId, consumerId);
	}

	@Test
	void should_pass_tracing_context_with_spring_integration(TestInfo testInfo) {
		// when
		String appId = deploy(testInfo, "integration");

		// then
		assertThatTraceIdGotPropagated(appId);
	}

	@Test
	void should_pass_tracing_context_with_circuit_breaker(TestInfo testInfo) {
		// when
		String appId = deploy(testInfo, "circuitbreaker");

		// then
		assertThatTraceIdGotPropagated(appId);
	}

	@Test
	void should_pass_tracing_context_with_reactive_circuit_breaker(TestInfo testInfo) {
		// when
		String appId = deploy(testInfo, "circuitbreaker-reactive");

		// then
		assertThatTraceIdGotPropagated(appId);
	}
}
