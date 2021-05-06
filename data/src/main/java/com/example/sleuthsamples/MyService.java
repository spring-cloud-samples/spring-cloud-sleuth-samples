package com.example.sleuthsamples;

import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;

@Service
public class MyService {

	private static final Logger log = LoggerFactory.getLogger(MyService.class);

	private final CustomerRepository repository;

	private final MyService2 myService2;

	public MyService(CustomerRepository repository, MyService2 myService2) {
		this.repository = repository;
		this.myService2 = myService2;
	}

	@Transactional
	public void foo() {
		// save a few customers
		repository.save(new Customer("Jack", "Bauer"));
		repository.save(new Customer("Chloe", "O'Brian"));
		repository.save(new Customer("Kim", "Bauer"));
		repository.save(new Customer("David", "Palmer"));
		repository.save(new Customer("Michelle", "Dessler"));

		// fetch all customers
		log.info("Customers found with findAll():");
		log.info("-------------------------------");
		for (Customer customer : repository.findAll()) {
			log.info(customer.toString());
		}
		log.info("");

		this.myService2.foo2();
	}
}
