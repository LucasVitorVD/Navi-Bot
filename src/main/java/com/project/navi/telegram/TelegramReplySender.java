package com.project.navi.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.Optional;

@Component
public class TelegramReplySender {

    private static final Logger log = LoggerFactory.getLogger(TelegramReplySender.class);

    private final TelegramClient telegramClient;

    public TelegramReplySender(Optional<TelegramClient> telegramClient) {
        this.telegramClient = telegramClient.orElse(null);
    }

    public void reply(Long chatId, Integer replyToMessageId, String text) {
        if (telegramClient == null) {
            log.warn("Bot do Telegram não configurado (TELEGRAM_BOT_TOKEN ausente); mensagem não enviada: {}", text);
            return;
        }

        try {
            telegramClient.execute(SendMessage.builder()
                    .chatId(chatId.toString())
                    .replyToMessageId(replyToMessageId)
                    .text(text)
                    .build());
        } catch (TelegramApiException e) {
            log.warn("Falha ao enviar mensagem via Telegram", e);
        }
    }
}
