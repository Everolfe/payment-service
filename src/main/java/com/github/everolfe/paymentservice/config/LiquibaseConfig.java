package com.github.everolfe.paymentservice.config;

import liquibase.command.CommandScope;
import liquibase.command.CommandResults;
import liquibase.exception.LiquibaseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LiquibaseConfig {

    @Value("${spring.liquibase.change-log}")
    private String changeLog;

    @Value("${ROOT_USERNAME}")
    private String username;

    @Value("${ROOT_PASSWORD}")
    private String password;

    @Value("${DB_NAME}")
    private String database;

    @Bean
    @ConditionalOnProperty(name = "spring.liquibase.enabled", havingValue = "true", matchIfMissing = false)
    public CommandResults runLiquibase() {
        try {
            return new CommandScope("update")
                    .addArgumentValue("changelogFile", changeLog)
                    .addArgumentValue(
                            "url",
                            "mongodb://" + username + ":" + password +
                                    "@mongo-db:27017/" + database + "?authSource=admin"
                    )
                    .execute();
        } catch (LiquibaseException e) {
            throw new RuntimeException(e);
        }
    }
}