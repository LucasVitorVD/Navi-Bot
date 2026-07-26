package com.project.navi.telegram;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Component
public class TelegramUpdateDispatcher implements LongPollingSingleThreadUpdateConsumer {

    private final HabitConfigCommandConsumer habitConfigCommandConsumer;
    private final HabitReplyUpdateConsumer habitReplyUpdateConsumer;

    public TelegramUpdateDispatcher(HabitConfigCommandConsumer habitConfigCommandConsumer,
                                     HabitReplyUpdateConsumer habitReplyUpdateConsumer) {
        this.habitConfigCommandConsumer = habitConfigCommandConsumer;
        this.habitReplyUpdateConsumer = habitReplyUpdateConsumer;
    }

    @Override
    public void consume(Update update) {
        if (isConfigCommand(update.getMessage())) {
            habitConfigCommandConsumer.consume(update);
            return;
        }
        habitReplyUpdateConsumer.consume(update);
    }

    private boolean isConfigCommand(Message message) {
        return message != null && message.hasText()
                && message.getText().trim().toLowerCase().startsWith("/config");
    }
}
