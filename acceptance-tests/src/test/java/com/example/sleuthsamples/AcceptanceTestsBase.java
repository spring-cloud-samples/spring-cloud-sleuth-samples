package com.example.sleuthsamples;

import java.util.Map;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.TestInfo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.deployer.spi.app.DeploymentState;

@SpringBootTest
class AcceptanceTestsBase {

	@Autowired
	ProjectDeployer projectDeployer;

	@Autowired
	TracingAssertions tracingAssertions;

	AcceptanceTestsBase() {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> projectDeployer.getIds().forEach((key, values) -> values.forEach(id -> {
			if (projectDeployer.status(id).getState() != DeploymentState.undeployed) {
				undeploy(id);
			}
		}))));
	}

	String deployWebApp(TestInfo testInfo, String appName, int port) {
		return this.projectDeployer.deployWebApp(testInfo, appName, port);
	}

	String deployWebApp(TestInfo testInfo, String appName, Map<String, String> props) {
		return this.projectDeployer.deployWebApp(testInfo, appName, props);
	}

	String deploy(TestInfo testInfo, String appName, Map<String, String> props) {
		return this.projectDeployer.deploy(testInfo, appName, props);
	}

	String waitUntilStarted(Callable<String> callable) {
		return this.projectDeployer.waitUntilStarted(callable);
	}

	private void undeploy(String id) {
		this.projectDeployer.undeploy(id);
	}

	@AfterEach
	void cleanup(TestInfo testInfo) {
		this.projectDeployer.clean(testInfo);
	}

	void assertThatTraceIdGotPropagated(String... appIds) {
		this.tracingAssertions.assertThatTraceIdGotPropagated(appIds);
	}

	@SpringBootApplication
	static class Config {

	}
}
