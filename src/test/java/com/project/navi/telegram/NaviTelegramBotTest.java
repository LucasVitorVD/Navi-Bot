package com.project.navi.telegram;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class NaviTelegramBotTest {

    @Mock
    private HabitReplyUpdateConsumer habitReplyUpdateConsumer;

    @Test
    void exposesConfiguredTokenAndUpdatesConsumer() {
        NaviTelegramBot bot = new NaviTelegramBot("some-token", habitReplyUpdateConsumer);

        assertThat(bot.getBotToken()).isEqualTo("some-token");
        assertThat(bot.getUpdatesConsumer()).isSameAs(habitReplyUpdateConsumer);
    }
}
