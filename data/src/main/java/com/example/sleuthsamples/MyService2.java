package com.example.sleuthsamples;

import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;

@Service
public class MyService2 {

	private static final Logger log = LoggerFactory.getLogger(MyService2.class);

	private final CustomerRepository repository;

	public MyService2(CustomerRepository repository) {
		this.repository = repository;
	}

	@Transactional
	public void foo2() {
		// fetch an individual customer by ID
		Customer customer = repository.findById(1L);
		log.info("Customer found with findById(1L):");
		log.info("--------------------------------");
		log.info(customer.toString());
		log.info("");

		// fetch customers by last name
		log.info("Customer found with findByLastName('Bauer'):");
		log.info("--------------------------------------------");
		repository.findByLastName("Bauer").forEach(bauer -> {
			log.info(bauer.toString());
		});
		repository.deleteById(10238L);
		log.info("");
	}
}
