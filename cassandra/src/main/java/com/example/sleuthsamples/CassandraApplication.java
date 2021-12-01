package com.example.sleuthsamples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.DataAccessException;

@SpringBootApplication
public class CassandraApplication {

	private static final Logger log = LoggerFactory.getLogger(CassandraApplication.class);

	public static void main(String... args) {
		new SpringApplicationBuilder(CassandraApplication.class).web(WebApplicationType.NONE).run(args);
	}

	@Bean
	public CommandLineRunner demo(BasicUserRepository basicUserRepository, Tracer tracer) {
		return (args) -> {
			Span span = tracer.nextSpan().name("cassandra-app");
			try (Tracer.SpanInScope ws = tracer.withSpan(span.start())){
				log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from producer", tracer.currentSpan().context().traceId());
				User save = basicUserRepository.save(new User("foo", "bar", "baz", 1L));
				User userByIdIn = basicUserRepository.findUserByIdIn(save.getId());
				basicUserRepository.findUserByIdIn(123123L);
			}
			catch (DataAccessException e) {
				log.info("Expected to throw an exception so that we see if rollback works", e);
			} finally {
				span.end();
			}
		};
	}
}
