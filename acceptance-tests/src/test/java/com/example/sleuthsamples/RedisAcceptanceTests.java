package com.example.sleuthsamples;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.SocketUtils;

// Uncomment the properties to rebuild the projects
// @formatter:off
@SpringBootTest(
//		properties = {"spring.cloud.sleuth.samples.rebuild-projects=true", "spring.cloud.sleuth.samples.project-root=${user.home}/repo/spring-cloud-sleuth-samples", "maven.home=${user.home}/.sdkman/candidates/maven/current"}
)
@Testcontainers
// @formatter:on
class RedisAcceptanceTests extends AcceptanceTestsBase {

	@Container
	static GenericContainer redis = new GenericContainer(DockerImageName.parse("redis:6.2.3-alpine"))
			.withExposedPorts(6379);

	@Test
	void should_pass_tracing_context_from_client_to_redis_based_session_app(TestInfo testInfo) throws Exception {
		// given
		int port = SocketUtils.findAvailableTcpPort();
		String producerId = waitUntilStarted(() -> deployWebApp(testInfo, "session", redisConfiguration(port)));

		// when
		String consumerId = deploy(testInfo, "resttemplate", Map.of("url", "http://localhost:" + port));

		// then
		assertThatTraceIdGotPropagated(producerId, consumerId);
	}

	private Map<String, String> redisConfiguration(int mvcPort) {
		return Map.of("spring.redis.host", redis.getHost(), "spring.redis.port", redis.getFirstMappedPort().toString(), "server.port", String.valueOf(mvcPort));
	}

}
