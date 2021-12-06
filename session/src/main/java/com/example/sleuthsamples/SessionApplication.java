package com.example.sleuthsamples;

import javax.servlet.http.HttpSession;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class SessionApplication {

	public static void main(String... args) {
		new SpringApplication(SessionApplication.class).run(args);
	}
}

@RestController
class SessionController {
	private static final Logger log = LoggerFactory.getLogger(SessionController.class);

	@GetMapping("/")
	public String span(HttpSession httpSession) {
		log.info("Session id {}", httpSession.getId());
		return httpSession.getId();
	}
}

// This is only for test purposes. In your code you won't be doing this.
@Aspect
@Component
class TestSessionRepositoryAspect {
	private static final Logger log = LoggerFactory.getLogger(TestSessionRepositoryAspect.class);

	private final Tracer tracer;

	public TestSessionRepositoryAspect(Tracer tracer) {
		this.tracer = tracer;
	}

	@Around("execution(public * org.springframework.session.SessionRepository.createSession(..))")
	public Object wrapSessionRepository(ProceedingJoinPoint pjp) throws Throwable {
		String traceId = this.tracer.currentSpan().context().traceId();
		log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from producer", traceId);
		return pjp.proceed();
	}
}
