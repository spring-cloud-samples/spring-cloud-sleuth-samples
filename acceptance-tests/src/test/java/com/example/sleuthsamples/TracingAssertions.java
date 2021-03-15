package com.example.sleuthsamples;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.awaitility.Awaitility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;

import static org.assertj.core.api.BDDAssertions.then;

@Component
class TracingAssertions {

	private static final Logger log = LoggerFactory.getLogger(TracingAssertions.class);

	private final ProjectDeployer projectDeployer;

	TracingAssertions(ProjectDeployer projectDeployer) {
		this.projectDeployer = projectDeployer;
	}

	void assertThatTraceIdGotPropagated(String... appIds) {
		Pattern tracePattern = Pattern.compile("^.*<ACCEPTANCE_TEST> <TRACE:(.*)> .*$");
		Awaitility.await().pollInterval(1, TimeUnit.SECONDS)
				.atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
			List<String> traceIds = Arrays.stream(appIds).map(this.projectDeployer::getLog)
					.flatMap(s -> Arrays.stream(s.split(System.lineSeparator())))
					.filter(s -> s.contains("ACCEPTANCE_TEST")).map(s -> {
						Matcher matcher = tracePattern.matcher(s);
						if (matcher.matches()) {
							return matcher.group(1);
						}
						return null;
					}).filter(Objects::nonNull)
					.distinct()
					.collect(Collectors.toList());
			log.info("Found the following trace id {}", traceIds);
			then(traceIds).as("TraceId should have only one value").hasSize(1);
		});
	}
}
