package com.example.sleuthsamples;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.boot.test.context.SpringBootTest;

// Uncomment the properties to rebuild the projects
// @formatter:off
@SpringBootTest(
//		properties = {"spring.cloud.sleuth.samples.rebuild-projects=true", "spring.cloud.sleuth.samples.project-root=${user.home}/repo/spring-cloud-sleuth-samples", "maven.home=${user.home}/.sdkman/candidates/maven/current"}
)
@Testcontainers
// @formatter:on
class MongoDbAcceptanceTests extends AcceptanceTestsBase {

	@Container
	static MongoDBContainer mongo = new MongoDBContainer("mongo:4.4.7");

	@Test
	void should_pass_tracing_context_with_mongo(TestInfo testInfo) {
		// when
		String producerId = deploy(testInfo, "mongodb-reactive", replicaSetUri());

		// then
		assertThatTraceIdGotPropagated(producerId);
	}

	private Map<String, String> replicaSetUri() {
		return Map.of("spring.data.mongodb.uri", mongo.getReplicaSetUrl());
	}

}
