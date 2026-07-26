package com.project.navi.telegram;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TelegramUpdateDispatcherTest {

    @Mock
    private HabitConfigCommandConsumer habitConfigCommandConsumer;

    @Mock
    private HabitReplyUpdateConsumer habitReplyUpdateConsumer;

    private TelegramUpdateDispatcher dispatcher() {
        return new TelegramUpdateDispatcher(habitConfigCommandConsumer, habitReplyUpdateConsumer);
    }

    private Update textUpdate(String text) {
        Message message = Message.builder().messageId(1).text(text).build();
        Update update = new Update();
        update.setMessage(message);
        return update;
    }

    @Test
    void delegatesToConfigConsumerWhenTextStartsWithConfigCommand() {
        Update update = textUpdate("/config água 500ml");

        dispatcher().consume(update);

        verify(habitConfigCommandConsumer).consume(update);
        verify(habitReplyUpdateConsumer, never()).consume(update);
    }

    @Test
    void delegatesToReplyConsumerForOtherMessages() {
        Update update = textUpdate("oi pessoal");

        dispatcher().consume(update);

        verify(habitReplyUpdateConsumer).consume(update);
        verify(habitConfigCommandConsumer, never()).consume(update);
    }

    @Test
    void delegatesToReplyConsumerWhenUpdateHasNoMessage() {
        Update update = new Update();

        dispatcher().consume(update);

        verify(habitReplyUpdateConsumer).consume(update);
        verify(habitConfigCommandConsumer, never()).consume(update);
    }
}
