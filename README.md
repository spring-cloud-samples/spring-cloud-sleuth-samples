![Java CI with Maven](https://github.com/spring-cloud-samples/spring-cloud-sleuth-samples/workflows/Java%20CI%20with%20Maven/badge.svg)

# Spring Cloud Sleuth Samples

This repository contains isolated samples showing various integrations with Spring Cloud Sleuth.

You can read more about the details of the instrumentation logic in each of the samples.

## Turning on Wavefront support

Build the apps with the `default` and `wavefront` profile turned on. `default` will add Sleuth with Brave on the classpath.

. All projects and acceptance tests
```bash
$ ./mvnw clean install -Pdefault,wavefront
```

. Build one project
```bash
$ ./mvnw clean install -Pdefault,wavefront -pl task
$ # Run one app
$ java -jar task/target/task*.jar 
```

. Run one project (example for task)
```bash
$ ./mvnw -Pdefault,wavefront -pl task spring-boot:run
```

If you want to run the project from IDE remember to tick the `default` and `wavefront` profiles there too.

## Turning on Zipkin support

Run Zipkin

```bash
$ docker run -d -p 9411:9411 openzipkin/zipkin
```

Build the apps with the `default` and `zipkin` profile turned on. `default` will add Sleuth with Brave on the classpath.

. All projects and acceptance tests
```bash
$ ./mvnw clean install -Pdefault,zipkin
```

. Build one project
```bash
$ ./mvnw clean install -Pdefault,zipkin -pl task
$ # Run one app
$ java -jar task/target/task*.jar 
```

. Run one project (example for task)
```bash
$ ./mvnw -Pdefault,zipkin -pl task spring-boot:run
```

If you want to run the project from IDE remember to tick the `default` and `zipkin` profiles there too.

## Using OpenTelemetry

Instead of using the `default` profile please use the `otel` profile. So if you want to run `otel` based tracing with zipkin you need to build the
application with both `otel` and `zipkin` profiles. Example:

. All projects and acceptance tests
```bash
$ ./mvnw clean install -Potel,zipkin
```

. Build one project
```bash
$ ./mvnw clean install -Potel,zipkin -pl task
$ # Run one app
$ java -jar task/target/task*.jar 
```

. Run one project (example for task)
```bash
$ ./mvnw -Potel,zipkin -pl task spring-boot:run
```

# FAQ

## The logging text makes no sense

You can see logs like this

```java
log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from producer", tracer.currentSpan().context().traceId());
```

or this

```java
log.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from consumer", tracer.currentSpan().context().traceId());
```

even though there is no consumer / producer...

That's because in the acceptance tests we're using conventions and we're searching for exactly those entries in the logs to see if the context got properly propagated. 
