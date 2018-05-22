* `@Bean RoutesBuilder`
* Actuator shows health of individual routes
* Actuator lets you stop/start routes
* there's a camel component that forwards to SI channels
* `CamelContextAware`
* property sources in SIMPLE : "{{ a.b.c }}" references `application.properties`
* interesting properties like:  `camel.springboot.main-run-controller=true`, `spring.application.name=bootiful-apache-camel`, `camel.springboot.name=${spring.application.name}`
* the TONS of Camel endpoints & components
* you can even route from camel to a Spring INtegration channel which opens up opportunities for SI itself, but also Spring Cloud Stream, and Spring Cloud Data Flow, WebSockets, etc.
* https://blog.switchbit.io/camel-spring-cloud-stream/
* connectors: https://github.com/apache/camel/tree/master/connectors/examples/twitter-salesforce-example
* there's a nice IntelliJ plugin demonstrating how to use some of the components

# He wants to see
* start.spring.io
* add starters
* CamelRoute
* filtering and other EIP patterns
*


#Components:
* typically a thing that can integrate to other systems. you have four APIs.
* Endpoints (e.g: `jms:`), Consumer, Producer, Component. Component is a factory for Endpoint.
* Data Formats: message is a container for payload and body.
* Exchange encapsulates the entire lifecycle of a message in Camel.
