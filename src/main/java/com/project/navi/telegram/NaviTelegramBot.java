package com.project.navi.telegram;

import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;

public class NaviTelegramBot implements SpringLongPollingBot {

    private final String botToken;
    private final HabitReplyUpdateConsumer updatesConsumer;

    public NaviTelegramBot(String botToken, HabitReplyUpdateConsumer updatesConsumer) {
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
