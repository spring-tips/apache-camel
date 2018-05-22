package com.example.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.file.GenericFile;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.spi.ComponentCustomizer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.channel.MessageChannels;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Component;

import javax.jms.ConnectionFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.UUID;
import java.util.stream.Collectors;

@SpringBootApplication
public class CamelApplication {

		private final CamelContext camelContext;

		public CamelApplication(CamelContext camelContext) {
				this.camelContext = camelContext;
//				this.camelContext.addComponent("my-jms", JmsComponent.jmsComponent(cf));
		}


		@Component
		static class DefaultJmsComponentCustomizer implements ComponentCustomizer<JmsComponent> {

				private final ConnectionFactory connectionFactory;

				DefaultJmsComponentCustomizer(ConnectionFactory connectionFactory) {
						this.connectionFactory = connectionFactory;
				}

				@Override
				public void customize(JmsComponent component) {
						component.setConnectionFactory(this.connectionFactory);
				}
		}


		@Bean
		RoutesBuilder routes() {
				return new RouteBuilder() {

						@Override
						public void configure() throws Exception {

								from("file://{{user.home}}/Desktop/in")
									.routeId("in-to-out")
									.to("file://{{user.home}}/Desktop/out?autoCreate=false");

								from("file://{{user.home}}/Desktop/to-jms")
									.routeId("file-to-jms")
									.transform()
									.body(GenericFile.class, gf -> {
											File actualFile = File.class.cast(gf.getFile());
											try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(actualFile)))) {
													return in.lines().collect(Collectors.joining());
											}
											catch (Exception e) {
													throw new RuntimeException(e);
											}
									})
									.process()
									.body(String.class, string -> LogFactory.getLog(CamelApplication.this.getClass()).info("the string body is " + string))
									/*.exchange(exchange -> {
											Message in = exchange.getIn();
											String body = in.getBody(String.class);
											LogFactory.getLog(getClass()).info("body is " + body);
									})*/
									//@formatter:off
									.choice()
											.when(exchange -> exchange.getIn().getBody(String.class).contains("hello"))
											.to("jms:queue:hello")
									.otherwise()
											.to("jms:queue:files")
									.endChoice();
									//@formatter:on

								from("jms:queue:hello")
									.to("spring-integration:incoming");

								from("jms:queue:files")
									.routeId("jms-to-file")
									.setHeader("CamelFIleName", () -> UUID.randomUUID().toString() + ".txt")
									.to("file://{{user.home}}/Desktop/from-jms");
						}
				};
		}

		public static void main(String[] args) {
				SpringApplication.run(CamelApplication.class, args);
		}
}


@Configuration
class IntegrationFlowConfiguration {

		@Bean
		MessageChannel incoming() {
				return MessageChannels.direct().get();
		}

		@Bean
		IntegrationFlow flow() {
				Log log = LogFactory.getLog(getClass());
				return IntegrationFlows
					.from(this.incoming())
					.handle((o, map) -> {
							log.info("new message! " + o.toString());
							return null;
					})
					.get();
		}

}