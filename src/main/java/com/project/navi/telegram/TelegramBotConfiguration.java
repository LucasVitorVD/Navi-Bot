package com.project.navi.telegram;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TelegramBotConfiguration {

    @Bean
    @ConditionalOnExpression("!'${TELEGRAM_BOT_TOKEN:}'.isBlank()")
    public NaviTelegramBot naviTelegramBot(@Value("${TELEGRAM_BOT_TOKEN:}") String botToken,
                                            HabitReplyUpdateConsumer habitReplyUpdateConsumer) {
        return new NaviTelegramBot(botToken, habitReplyUpdateConsumer);
    }
}
