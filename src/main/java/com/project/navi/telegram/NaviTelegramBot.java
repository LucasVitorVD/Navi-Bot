package com.project.navi.telegram;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;

@Component
public class NaviTelegramBot implements SpringLongPollingBot {

    private final String botToken;
    private final HabitReplyUpdateConsumer updatesConsumer;

    public NaviTelegramBot(@Value("${TELEGRAM_BOT_TOKEN:}") String botToken,
                            HabitReplyUpdateConsumer updatesConsumer) {
        this.botToken = botToken;
        this.updatesConsumer = updatesConsumer;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return updatesConsumer;
    }
}
