package com.example.sleuthsamples;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.apache.maven.shared.invoker.SystemOutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
class ProjectRebuilder {

	private static final Logger log = LoggerFactory.getLogger(ProjectRebuilder.class);

	private final boolean rebuildProjects;

	private final String mavenHome;

	private final String projectRoot;

	ProjectRebuilder(@Value("${spring.cloud.sleuth.samples.rebuild-projects:false}") boolean rebuildProjects,
			@Value("${maven.home:}") String mavenHome,
			@Value("${spring.cloud.sleuth.samples.project-root:}") String projectRoot) {
		this.rebuildProjects = rebuildProjects;
		this.mavenHome = mavenHome;
		this.projectRoot = projectRoot;
	}

	void rebuildProjectBeforeDeployment(String appName) {
		if (!this.rebuildProjects) {
			log.info("The flag [spring.cloud.sleuth.samples.rebuild-projects] was set to false - won't rebuild the projects");
			return;
		}
		else if (!StringUtils.hasText(mavenHome)) {
			log.warn("The flag to rebuild was set to [true], however no MAVEN_HOME or maven.home was set, can't rebuild the projects");
			return;
		}
		File rootPom = new File(this.projectRoot, "pom.xml");
		try {
			invoker().execute(invocationRequest(rootPom, appName));
		}
		catch (MavenInvocationException e) {
			throw new IllegalStateException(e);
		}
	}


	private Invoker invoker() {
		Invoker invoker = new DefaultInvoker();
		invoker.setMavenHome(new File(this.mavenHome));
		invoker.setOutputHandler(new SystemOutHandler()); // not interested in Maven output itself
		return invoker;
	}

	private InvocationRequest invocationRequest(File pom, String appName) {
		InvocationRequest request = new DefaultInvocationRequest();
		request.setReactorFailureBehavior(InvocationRequest.ReactorFailureBehavior.FailFast);
		request.setPomFile(pom);
		request.setProjects(Collections.singletonList(appName));
		request.setProfiles(Collections.singletonList("notest"));
		request.setGoals(Arrays.asList("clean", "install"));
		Properties properties = new Properties();
		request.setProperties(properties);
		return request;
	}
}
