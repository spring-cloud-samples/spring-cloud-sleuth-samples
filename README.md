![Java CI with Maven](https://github.com/spring-cloud-samples/spring-cloud-sleuth-samples/workflows/Java%20CI%20with%20Maven/badge.svg)

# Spring Cloud Sleuth Samples

This repository contains isolated samples showing various integrations with Spring Cloud Sleuth.

You can read more about the details of the instrumentation logic in each of the samples.

## Turning on Zipkin support

Build the apps with the `zipkin` profile turned on. If you want to run the project from IDE remember to tick the `zipkin` profile there too.

. All projects

```bash
$ ./mvnw clean install -Pzipkin
```

. Build one project

```bash
$ ./mvnw clean install -Pzipkin -pl task
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
