package com.example.sleuthsamples;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.vault.VaultContainer;

import org.springframework.boot.test.context.SpringBootTest;

// Uncomment the properties to rebuild the projects
// @formatter:off
@SpringBootTest(
//		properties = {"spring.cloud.sleuth.samples.rebuild-projects=true", "spring.cloud.sleuth.samples.project-root=${user.home}/repo/spring-cloud-sleuth-samples", "maven.home=${user.home}/.sdkman/candidates/maven/current"}
)
@Testcontainers
// @formatter:on
class VaultAcceptanceTests extends AcceptanceTestsBase {

	@Container
	static VaultContainer vault = new VaultContainer("vault:1.7.0")
				.withVaultToken("vault-plaintext-root-token");

	@Test
	void should_pass_tracing_context_with_vault(TestInfo testInfo) {
		// when
		String producerId = deploy(testInfo, "vault-resttemplate", port());

		// then
		assertThatTraceIdGotPropagated(producerId);
	}

	@Test
	void should_pass_tracing_context_with_vault_reactive(TestInfo testInfo) {
		// when
		String producerId = deploy(testInfo, "vault-webclient", port());

		// then
		assertThatTraceIdGotPropagated(producerId);
	}

	private Map<String, String> port() {
		return Map.of("spring.cloud.vault.port", String.valueOf(vault.getFirstMappedPort()));
	}

}
