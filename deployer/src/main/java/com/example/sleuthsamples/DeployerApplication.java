package com.example.sleuthsamples;

import java.time.Duration;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.deployer.resource.maven.MavenResource;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.util.SocketUtils;

@SpringBootApplication
public class DeployerApplication implements CommandLineRunner {

	private static final Log log = LogFactory.getLog(DeployerApplication.class);

	public static void main(String... args) {
		new SpringApplicationBuilder(DeployerApplication.class).web(WebApplicationType.NONE).run(args);
	}

	@Autowired
	Tracer tracer;

	@Autowired
	AppDeployer appDeployer;

	@Override
	public void run(String... args) throws Exception {
		deployerNonReactive();
		deployerReactive();
	}

	private void deployerReactive() {
		Span nextSpan = this.tracer.nextSpan().name("deployerReactive");
		Mono.just(nextSpan)
				.doOnNext(span -> this.tracer.withSpan(span.start()))
					.map(span -> this.appDeployer.deploy(appRequest()))
					.flatMap(id -> this.appDeployer.statusReactive(id))
					.map(appStatus -> this.appDeployer.getLog(appStatus.getDeploymentId()))
					.doFinally(signalType -> nextSpan.end())
					.block();
	}

	private void deployerNonReactive() throws InterruptedException {
		Span deployer = this.tracer.nextSpan().name("deployer");
		try (Tracer.SpanInScope spanInScope = this.tracer.withSpan(deployer.start())) {
			String id = this.appDeployer.deploy(appRequest());
			log.info("Deploying app with id [" + id + "]");
			boolean deployed = waitUntilStatusIsSet(id, DeploymentState.deployed);
			log.info("App logs \n" + this.appDeployer.getLog(id) + "\n");
			if (!deployed) {
				throw new IllegalStateException("The app with id [" + id + "] was not successfully deployed");
			}
			log.info("App successfully deployed");
			this.appDeployer.undeploy(id);
		}
		finally {
			deployer.end();
		}
	}

	private boolean waitUntilStatusIsSet(String id, DeploymentState expectedState) throws InterruptedException {
		int counter = 10;
		boolean deployed = this.appDeployer.status(id).getState() == expectedState;
		while (!deployed && counter >= 0) {
			Thread.sleep(1000);
			DeploymentState state = this.appDeployer.status(id).getState();
			log.info("App state is [" + state + "]");
			deployed = state == expectedState;
			counter = counter - 1;
			log.info("Remaining attempts [" + counter + "]");
		}
		return deployed;
	}

	private AppDeploymentRequest appRequest() {
		AppDefinition appDefinition = new AppDefinition("mvc", Map.of("server.port", String.valueOf(SocketUtils.findAvailableTcpPort())));
		return deploymentRequest(appDefinition);
	}

	private AppDeploymentRequest deploymentRequest(AppDefinition appDefinition) {
		return new AppDeploymentRequest(appDefinition, new MavenResource.Builder()
				.groupId("com.example.sleuthsamples")
				.artifactId("mvc")
				.version("1.0.0-SLEUTH")
				.build());
	}
}
