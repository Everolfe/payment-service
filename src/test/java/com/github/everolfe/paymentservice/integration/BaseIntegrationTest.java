package com.github.everolfe.paymentservice.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureWireMock(port = 0)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = {
        "ROOT_USERNAME=test_user",
        "ROOT_PASSWORD=test",
        "spring.liquibase.enabled=false",
        "spring.data.mongodb.host=localhost",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration,liquibase.ext.mongodb.spring.boot.autoconfigure.LiquibaseMongodbAutoConfiguration"
        })
@ActiveProfiles("test")
public class BaseIntegrationTest {

    @Container
    static final MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:6.0"));

    @Container
    static final KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @MockBean
    private JwtDecoder jwtDecoder;

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {

        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);

        registry.add("random.api.url",
                () -> "http://localhost:${wiremock.server.port}/integers/?num=1&min=1&max=100&col=1&base=10&format=plain&rnd=new");

        registry.add("PRODUCER_TOPIC", () -> "create-payment");
        registry.add("GROUP_ID", () -> "payment-service-group");

        registry.add("APP_PORT", () -> "8085");
        registry.add("MONGO_URI", mongoDBContainer::getReplicaSetUrl);
        registry.add("BOOTSTRAP_SERVER", kafkaContainer::getBootstrapServers);
    }
}