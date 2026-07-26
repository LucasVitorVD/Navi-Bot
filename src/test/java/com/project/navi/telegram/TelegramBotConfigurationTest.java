package com.project.navi.telegram;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class TelegramBotConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class, TelegramBotConfiguration.class);

    @Test
    void registersBotWhenTokenIsConfigured() {
        contextRunner.withPropertyValues("TELEGRAM_BOT_TOKEN=some-real-token")
                .run(context -> assertThat(context).hasSingleBean(NaviTelegramBot.class));
    }

    @Test
    void doesNotRegisterBotWhenTokenIsBlank() {
        contextRunner.withPropertyValues("TELEGRAM_BOT_TOKEN=")
                .run(context -> assertThat(context).doesNotHaveBean(NaviTelegramBot.class));
    }

    @Test
    void doesNotRegisterBotWhenTokenIsMissing() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(NaviTelegramBot.class));
    }

    @Test
    void registersTelegramClientWhenTokenIsConfigured() {
        contextRunner.withPropertyValues("TELEGRAM_BOT_TOKEN=some-real-token")
                .run(context -> assertThat(context).hasSingleBean(TelegramClient.class));
    }

    @Test
    void doesNotRegisterTelegramClientWhenTokenIsBlank() {
        contextRunner.withPropertyValues("TELEGRAM_BOT_TOKEN=")
                .run(context -> assertThat(context).doesNotHaveBean(TelegramClient.class));
    }

    @Configuration
    static class TestConfig {
        @Bean
        TelegramUpdateDispatcher telegramUpdateDispatcher() {
            return mock(TelegramUpdateDispatcher.class);
        }
    }
}
