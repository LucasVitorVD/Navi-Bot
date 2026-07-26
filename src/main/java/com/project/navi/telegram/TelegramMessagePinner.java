package com.project.navi.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.pinnedmessages.PinChatMessage;
import org.telegram.telegrambots.meta.api.methods.pinnedmessages.UnpinChatMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.Optional;

@Component
public class TelegramMessagePinner {

    private static final Logger log = LoggerFactory.getLogger(TelegramMessagePinner.class);

    private final TelegramClient telegramClient;

    public TelegramMessagePinner(Optional<TelegramClient> telegramClient) {
        this.telegramClient = telegramClient.orElse(null);
    }

    public void pin(Long chatId, Integer messageId) {
        if (telegramClient == null) {
            return;
        }
        try {
            telegramClient.execute(PinChatMessage.builder()
                    .chatId(chatId)
                    .messageId(messageId)
                    .disableNotification(true)
                    .build());
        } catch (TelegramApiException e) {
            log.warn("Falha ao fixar mensagem (chatId={}, messageId={}). O bot é admin com permissão de fixar?",
                    chatId, messageId, e);
        }
    }

    public void unpin(Long chatId, Integer messageId) {
        if (telegramClient == null) {
            return;
        }
        try {
            telegramClient.execute(UnpinChatMessage.builder()
                    .chatId(chatId)
                    .messageId(messageId)
                    .build());
        } catch (TelegramApiException e) {
            log.warn("Falha ao desafixar mensagem (chatId={}, messageId={})", chatId, messageId, e);
        }
    }
}
