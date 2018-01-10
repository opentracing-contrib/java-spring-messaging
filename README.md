[![Build Status][ci-img]][ci] [![Released Version][maven-img]][maven]

# OpenTracing Spring Messaging

This repository provides OpenTracing instrumentation for various frameworks that use Spring Messaging (e.g. Spring Cloud Stream). It can be used with any OpenTracing compatible implementation. It implements Spring Messaging ChannelInterceptor interface and registers as a global channel interceptor.

## Configuration

> **Note**: make sure that an `io.opentracing.Tracer` bean is available. It is not provided by this library.

### Spring Boot
Add the following starter dependency to your pom.xml:
```xml
<dependency>
  <groupId>io.opentracing.contrib</groupId>
  <artifactId>opentracing-spring-messaging-starter</artifactId>
</dependency>
```

### Spring
Add the following dependency to your pom.xml:
```xml
<dependency>
  <groupId>io.opentracing.contrib</groupId>
  <artifactId>opentracing-spring-messaging</artifactId>
</dependency>
```

And register an interceptor bean:
```java
@Bean
@GlobalChannelInterceptor
public OpenTracingChannelInterceptor openTracingChannelInterceptor(Tracer tracer) {
  return new OpenTracingChannelInterceptor(tracer);
}
```

## Development
Maven checkstyle plugin is used to maintain consistent code style based on [Google Style Guides](https://github.com/google/styleguide)
```shell
./mvnw clean install
```

## Release
Follow instructions in [RELEASE](RELEASE.md)

[ci-img]: https://travis-ci.org/opentracing-contrib/java-spring-messaging.svg?branch=master
[ci]: https://travis-ci.org/opentracing-contrib/java-spring-messaging
[maven-img]: https://img.shields.io/maven-central/v/io.opentracing.contrib/opentracing-spring-messaging.svg?maxAge=2592000
[maven]: http://search.maven.org/#search%7Cga%7C1%7Copentracing-spring-messaging
