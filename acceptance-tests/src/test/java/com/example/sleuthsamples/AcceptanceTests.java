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
	void should_pass_tracing_context_from_rest_template_to_mvc(TestInfo testInfo) throws Exception {
		// given
		int port = SocketUtils.findAvailableTcpPort();
		String producerId = waitUntilStarted(() -> deployWebApp(testInfo, "mvc", port));

		// when
		String consumerId = deploy(testInfo, "resttemplate", Map.of("url", "http://localhost:" + port));

		// then
		assertThatTraceIdGotPropagated(producerId, consumerId);
	}

	@Test
	void should_pass_tracing_context_from_web_client_to_webflux(TestInfo testInfo) throws Exception {
		// given
		int port = SocketUtils.findAvailableTcpPort();
		String producerId = waitUntilStarted(() -> deployWebApp(testInfo, "webflux", port));

		// when
		String consumerId = deploy(testInfo, "webclient", Map.of("url", "http://localhost:" + port));

		// then
		assertThatTraceIdGotPropagated(producerId, consumerId);
	}

	@Test
	void should_pass_tracing_context_from_openfeign_to_mvc(TestInfo testInfo) throws Exception {
		// given
		int port = SocketUtils.findAvailableTcpPort();
		String producerId = waitUntilStarted(() -> deployWebApp(testInfo, "mvc", port));

		// when
		String consumerId = deploy(testInfo, "openfeign", Map.of("url", "http://localhost:" + port));

		// then
		assertThatTraceIdGotPropagated(producerId, consumerId);
	}

	@Test
	void should_pass_tracing_context_from_gateway_to_mvc(TestInfo testInfo) throws Exception {
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
	void should_pass_tracing_context_with_batch(TestInfo testInfo) {
		// when
		String appId = deploy(testInfo, "batch");

		// then
		assertThatTraceIdGotPropagated(appId);
	}

	@Test
	void should_pass_tracing_context_with_data(TestInfo testInfo) {
		// when
		String appId = deploy(testInfo, "data");

		// then
		assertThatTraceIdGotPropagated(appId);
	}

	@Test
	void should_pass_tracing_context_with_data_reactive(TestInfo testInfo) {
		// when
		String appId = deploy(testInfo, "data-reactive");

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
	void should_pass_tracing_context_from_rsocket(TestInfo testInfo) throws Exception {
		// given
		int port = SocketUtils.findAvailableTcpPort();
		String producerId = waitUntilStarted(() -> deployWebApp(testInfo, "rsocket-server", port));

		// when
		String consumerId = deploy(testInfo, "rsocket-client", Map.of("url", "ws://localhost:" + port + "/rsocket"));

		// then
		assertThatTraceIdGotPropagated(producerId, consumerId);
	}

	@Test
	void should_pass_tracing_context_with_reactive_circuit_breaker(TestInfo testInfo) {
		// when
		String appId = deploy(testInfo, "circuitbreaker-reactive");

		// then
		assertThatTraceIdGotPropagated(appId);
	}

	@Test
	void should_pass_tracing_context_with_spring_cloud_task(TestInfo testInfo) {
		// when
		String appId = deploy(testInfo, "task");

		// then
		assertThatTraceIdGotPropagated(appId);
	}

	@Test
	void should_pass_tracing_context_with_config_server(TestInfo testInfo) throws Exception {
		// when
		int port = SocketUtils.findAvailableTcpPort();
		String appId = waitUntilStarted(() -> deployWebApp(testInfo, "config-server", port));

		// then
		assertThatLogsContainPropagatedIdAtLeastXNumberOfTimes(appId, "config-server", 2);
	}

	@Test
	void should_pass_tracing_context_with_deployer(TestInfo testInfo) {
		// when
		String appId = deploy(testInfo, "deployer");

		// then
		assertThatLogsContainPropagatedIdAtLeastXNumberOfTimes(appId, "deployer", 9);
	}

	@Test
	void should_pass_baggage_and_remote_fields(TestInfo testInfo) throws Exception {
		// given
		int port = SocketUtils.findAvailableTcpPort();
		String consumerId = waitUntilStarted(() -> deployWebApp(testInfo, "baggage-consumer", port));

		// when
		String producerId = deploy(testInfo, "baggage-producer", Map.of("url", "http://localhost:" + port));

		// then
		assertThatTraceIdGotPropagated(producerId, consumerId);
	}

	@Test
	void should_pass_tracing_context_from_rest_template_to_security(TestInfo testInfo) throws Exception {
		// given
		int port = SocketUtils.findAvailableTcpPort();
		String producerId = waitUntilStarted(() -> deployWebApp(testInfo, "security", port));

		// when
		String consumerId = deploy(testInfo, "resttemplate", Map.of("url", "http://localhost:" + port + "/api/hello"));

		// then
		assertThatTraceIdGotPropagated(producerId, consumerId);
	}
}
