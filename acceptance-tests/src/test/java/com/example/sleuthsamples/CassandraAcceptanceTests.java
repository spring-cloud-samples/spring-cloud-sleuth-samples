package com.example.sleuthsamples;

import java.util.Map;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.testcontainers.containers.CassandraContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.vault.VaultContainer;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;

// Uncomment the properties to rebuild the projects
// @formatter:off
@SpringBootTest(
//		properties = {"spring.cloud.sleuth.samples.rebuild-projects=true", "spring.cloud.sleuth.samples.project-root=${user.home}/repo/spring-cloud-sleuth-samples", "maven.home=${user.home}/.sdkman/candidates/maven/current"}
)
@Testcontainers
// @formatter:on
class CassandraAcceptanceTests extends AcceptanceTestsBase {

	@Container
	static CassandraContainer cassandra = new CassandraContainer("cassandra:3.11.2");

	@BeforeAll
	static void setup() {
		Cluster cluster = cassandra.getCluster();
		try (Session session = cluster.connect()) {
			session.execute("CREATE KEYSPACE IF NOT EXISTS example WITH replication = \n" +
					"{'class':'SimpleStrategy','replication_factor':'1'};");
		}
	}

	@Test
	void should_pass_tracing_context_with_cassandra(TestInfo testInfo) {
		// when
		String producerId = deploy(testInfo, "cassandra", port());

		// then
		assertThatLogsContainPropagatedIdAtLeastXNumberOfTimes(producerId, "cassandra", 7);
	}

	@Test
	void should_pass_tracing_context_with_cassandra_reactive(TestInfo testInfo) {
		// when
		String producerId = deploy(testInfo, "cassandra-reactive", port());

		// then
		assertThatLogsContainPropagatedIdAtLeastXNumberOfTimes(producerId, "cassandra-reactive", 7);
	}

	private Map<String, String> port() {
		return Map.of("spring.data.cassandra.contact-points", cassandra.getContainerIpAddress() + ":" + cassandra.getFirstMappedPort());
	}

}
