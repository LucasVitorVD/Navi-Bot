package com.project.navi.telegram;

import com.project.navi.domain.Habit;
import com.project.navi.domain.HabitType;
import com.project.navi.reminder.HabitIdentificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.photo.PhotoSize;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HabitReplyUpdateConsumerTest {

    @Mock
    private HabitIdentificationService habitIdentificationService;

    @Mock
    private HabitRegistrationService habitRegistrationService;

    @Mock
    private HabitSelectionPrompter habitSelectionPrompter;

    private final Habit water = Habit.builder().id(1L).name("Água").type(HabitType.CUMULATIVE).unit("ml").target(3000).build();

    private final org.telegram.telegrambots.meta.api.objects.User sender =
            org.telegram.telegrambots.meta.api.objects.User.builder().id(42L).firstName("Lucas").isBot(false).build();

    private HabitReplyUpdateConsumer consumer() {
        return new HabitReplyUpdateConsumer(habitIdentificationService, habitRegistrationService, habitSelectionPrompter);
    }

    private Message photoMessage(Message repliedTo, String caption) {
        return Message.builder()
                .messageId(600)
                .chat(Chat.builder().id(999L).type("group").build())
                .from(sender)
                .caption(caption)
                .replyToMessage(repliedTo)
                .photo(List.of(PhotoSize.builder().fileId("small").width(90).height(90).build(),
                        PhotoSize.builder().fileId("large").width(800).height(800).build()))
                .build();
    }

    @Test
    void delegatesToRegistrationServiceWhenReplyMatchesKnownReminder() {
        Message repliedTo = Message.builder().messageId(555).build();
        Message message = photoMessage(repliedTo, "bebi um copo");
        Update update = new Update();
        update.setMessage(message);

        when(habitIdentificationService.identifyHabit(555L)).thenReturn(Optional.of(water));

        consumer().consume(update);

        verify(habitRegistrationService).register(sender, water, "bebi um copo", "large", 999L, 600L);
        verify(habitSelectionPrompter, never()).prompt(any(), any(), any(), any(), any());
    }

    @Test
    void doesNothingWhenReplyDoesNotMatchAnyKnownReminder() {
        Message repliedTo = Message.builder().messageId(555).build();
        Message message = photoMessage(repliedTo, "oi");
        Update update = new Update();
        update.setMessage(message);

        when(habitIdentificationService.identifyHabit(555L)).thenReturn(Optional.empty());

        consumer().consume(update);

        verify(habitRegistrationService, never()).register(any(), any(), any(), any(), any(), any());
        verify(habitSelectionPrompter, never()).prompt(any(), any(), any(), any(), any());
    }

    @Test
    void promptsForHabitWhenPhotoIsNotAReply() {
        Message message = photoMessage(null, "bebi água");
        Update update = new Update();
        update.setMessage(message);

        consumer().consume(update);

        verify(habitSelectionPrompter).prompt(999L, 600, sender, "large", "bebi água");
        verify(habitIdentificationService, never()).identifyHabit(any());
        verify(habitRegistrationService, never()).register(any(), any(), any(), any(), any(), any());
    }

    @Test
    void ignoresUpdateWithoutMessage() {
        consumer().consume(new Update());

        verify(habitRegistrationService, never()).register(any(), any(), any(), any(), any(), any());
        verify(habitSelectionPrompter, never()).prompt(any(), any(), any(), any(), any());
    }

    @Test
    void ignoresMessageWithoutPhoto() {
        Message message = Message.builder()
                .messageId(600)
                .from(sender)
                .text("oi pessoal")
                .build();
        Update update = new Update();
        update.setMessage(message);

        consumer().consume(update);

        verify(habitRegistrationService, never()).register(any(), any(), any(), any(), any(), any());
        verify(habitSelectionPrompter, never()).prompt(any(), any(), any(), any(), any());
    }
}
