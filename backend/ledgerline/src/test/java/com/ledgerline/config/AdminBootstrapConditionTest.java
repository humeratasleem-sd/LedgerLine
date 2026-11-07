package com.ledgerline.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

class AdminBootstrapConditionTest {

    @Test
    void bootstrapBeanIsAbsentWithTheApplicationDefault() {
        new ApplicationContextRunner()
                .withUserConfiguration(BootstrapConfiguration.class)
                .run(context -> assertThat(context).doesNotHaveBean(AdminBootstrap.class));
    }

    @Configuration(proxyBeanMethods = false)
    @PropertySource("classpath:application.properties")
    @Import(AdminBootstrap.class)
    static class BootstrapConfiguration {
    }
}
