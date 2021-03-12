package com.example.sleuthsamples;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.SocketUtils;

//@SpringBootTest(properties = {"spring.cloud.sleuth.samples.rebuild-projects=true", "spring.cloud.sleuth.samples.project-root=${user.home}/repo/spring-cloud-sleuth-samples", "maven.home=${user.home}/.sdkman/candidates/maven/current"})
@SpringBootTest
class AcceptanceTests extends AcceptanceTestsBase {

	@Test
	void should_pass_tracing_context_from_rest_template_to_mvc(TestInfo testInfo) {
		// given
		int port = SocketUtils.findAvailableTcpPort();
		String producerId = waitUntilStarted(() -> deployWebApp(testInfo, "mvc", port));

		//when
		String consumerId = deploy(testInfo, "resttemplate", Map.of("url", "http://localhost:" + port));

		//then
		assertThatTraceIdGotPropagated(producerId, consumerId);
	}
}
