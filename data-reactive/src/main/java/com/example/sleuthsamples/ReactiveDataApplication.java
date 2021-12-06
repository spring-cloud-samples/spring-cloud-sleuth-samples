package com.example.sleuthsamples;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import io.r2dbc.proxy.ProxyConnectionFactory;
import io.r2dbc.proxy.callback.ProxyConfig;
import io.r2dbc.proxy.core.MethodExecutionInfo;
import io.r2dbc.proxy.core.QueryExecutionInfo;
import io.r2dbc.proxy.core.QueryInfo;
import io.r2dbc.proxy.listener.ProxyExecutionListener;
import io.r2dbc.spi.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
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

}
