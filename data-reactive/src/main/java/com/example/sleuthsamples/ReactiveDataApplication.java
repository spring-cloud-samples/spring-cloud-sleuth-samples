package com.example.sleuthsamples;

import java.time.Duration;

import io.r2dbc.spi.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;

@SpringBootApplication
public class ReactiveDataApplication {

	private static final Logger log = LoggerFactory.getLogger(ReactiveDataApplication.class);

	public static void main(String... args) {
		new SpringApplicationBuilder(ReactiveDataApplication.class).web(WebApplicationType.NONE)
				.run(args);
	}

	@Bean
	public CommandLineRunner demo(ReactiveNewTransactionService reactiveNewTransactionService) {
		return (args) -> {
			try {
				reactiveNewTransactionService.newTransaction().block(Duration.ofSeconds(50));
			}
			catch (DataAccessException e) {
				log.info("Expected to throw an exception so that we see if rollback works", e);
			}
		};
	}

	@Bean
	ConnectionFactoryInitializer initializer(ConnectionFactory connectionFactory) {
		ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer();
		initializer.setConnectionFactory(connectionFactory);
		initializer.setDatabasePopulator(new ResourceDatabasePopulator(new ClassPathResource("schema.sql")));
		return initializer;
	}
}
