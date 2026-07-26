package com.project.navi.telegram;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class NaviTelegramBotTest {

    @Mock
    private LongPollingUpdateConsumer updatesConsumer;

    @Test
    void exposesConfiguredTokenAndUpdatesConsumer() {
        NaviTelegramBot bot = new NaviTelegramBot("some-token", updatesConsumer);

        assertThat(bot.getBotToken()).isEqualTo("some-token");
        assertThat(bot.getUpdatesConsumer()).isSameAs(updatesConsumer);
    }
}
