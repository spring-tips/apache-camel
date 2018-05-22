package com.example.camel;

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
import org.springframework.integration.dsl.support.GenericHandler;
import org.springframework.integration.transformer.GenericTransformer;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Component;

import javax.jms.ConnectionFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.UUID;
import java.util.stream.Collectors;

// http://camel.apache.org/spring-boot.html

@SpringBootApplication
public class CamelApplication {

		public static void main(String[] args) {
				SpringApplication.run(CamelApplication.class, args);
		}
}

@Configuration
class CamelJmsConfiguration   /*, CamelContextAware*/ {

		@Component
		static class MyJmsComponentCustomizer implements ComponentCustomizer<JmsComponent> {

				private final javax.jms.ConnectionFactory connectionFactory;

				public MyJmsComponentCustomizer(ConnectionFactory connectionFactory) {
						this.connectionFactory = connectionFactory;
				}

				@Override
				public void customize(JmsComponent component) {
						component.setConnectionFactory(this.connectionFactory);
				}
		}

		@Bean
		RoutesBuilder myRouter() {
				return new RouteBuilder() {

						@Override
						public void configure() {

								// http://camel.apache.org/components.html

								from("file:{{user.home}}/Desktop/to-si")
									.routeId("files-to-si")
									.to("spring-integration:incoming");

								from("file:{{user.home}}/Desktop/to-jms")
									.routeId("files-to-amq")
									.transform()
									.body(GenericFile.class, gf -> {
											try (BufferedReader in = new BufferedReader(new InputStreamReader(
												new FileInputStream(File.class.cast(gf.getFile()))))) {
													return in.lines().collect(Collectors.joining());
											}
											catch (Exception e) {
													throw new RuntimeException(e);
											}
									})
									.to("jms:queue:files");

								from("jms:queue:files")
									.routeId("jms-to-file")
									.setHeader("CamelFileName", () -> UUID.randomUUID().toString() + ".txt")
									.to("file://{{user.home}}/Desktop/from-jms");
						}
				};
		}
}

@Configuration
class IntegrationFlowConfiguration {

		@Bean
		MessageChannel incoming() {
				return MessageChannels.direct().get();
		}

		@Bean
		IntegrationFlow process() {
				Log log = LogFactory.getLog(getClass());
				return IntegrationFlows
					.from(incoming())
					.transform((GenericTransformer<GenericFile, File>) file -> File.class.cast(file.getFile()))
					.handle((GenericHandler<File>) (file, headers) -> {
							log.info("spring integration handler: " + file.getAbsolutePath());
							return null;
					})
					.get();
		}
}
