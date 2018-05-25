package com.example.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.file.GenericFile;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.spi.ComponentCustomizer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
	private final Logger logger = LoggerFactory.getLogger(CamelApplication.this.getClass());

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
						public void configure() {

								from("file://{{user.home}}/Desktop/in")
									.routeId("in-to-out")
									.to("file://{{user.home}}/Desktop/out?autoCreate=false");

								from("file://{{user.home}}/Desktop/to-jms")
									.routeId("file-to-jms")
									.convertBodyTo(String.class)
									.log(LoggingLevel.INFO, logger, "the string body is ${body}")
									//@formatter:off
									.choice()
										.when(body().contains("hello"))
											.to("jms:queue:hello")
										.otherwise()
											.to("jms:queue:files")
									.endChoice();
									//@formatter:on

								from("jms:queue:hello")
									.to("spring-integration:incoming");

								from("jms:queue:files")
									.routeId("jms-to-file")
									.setHeader(Exchange.FILE_NAME, () -> UUID.randomUUID().toString() + ".txt")
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