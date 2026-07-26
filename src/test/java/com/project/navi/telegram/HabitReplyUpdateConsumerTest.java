package com.project.navi.telegram;

import com.project.navi.domain.Habit;
import com.project.navi.domain.HabitRecord;
import com.project.navi.domain.HabitType;
import com.project.navi.domain.User;
import com.project.navi.reminder.HabitIdentificationService;
import com.project.navi.repository.HabitRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.photo.PhotoSize;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HabitReplyUpdateConsumerTest {

    @Mock
    private HabitIdentificationService habitIdentificationService;

    @Mock
    private TelegramUserResolver telegramUserResolver;

    @Mock
    private HabitRecordRepository habitRecordRepository;

    private final Habit habit = Habit.builder()
            .id(1L)
            .name("Água")
            .type(HabitType.CUMULATIVE)
            .unit("ml")
            .target(3000)
            .build();

    private HabitReplyUpdateConsumer consumer() {
        return new HabitReplyUpdateConsumer(habitIdentificationService, telegramUserResolver, habitRecordRepository);
    }

    private Update replyWithPhotoUpdate(long repliedToMessageId, long senderTelegramId, String firstName,
                                         String caption) {
        org.telegram.telegrambots.meta.api.objects.User sender = org.telegram.telegrambots.meta.api.objects.User.builder()
                .id(senderTelegramId)
                .firstName(firstName)
                .isBot(false)
                .build();

        Message repliedTo = Message.builder()
                .messageId((int) repliedToMessageId)
                .build();

        Message message = Message.builder()
                .messageId(600)
                .from(sender)
                .caption(caption)
                .replyToMessage(repliedTo)
                .photo(List.of(PhotoSize.builder().fileId("small").width(90).height(90).build(),
                        PhotoSize.builder().fileId("large").width(800).height(800).build()))
                .build();

        Update update = new Update();
        update.setMessage(message);
        return update;
    }

    @Test
    void savesHabitRecordUsingUserResolvedByTelegramUserResolver() {
        User resolvedUser = User.builder().id(10L).telegramUserId(42L).name("Lucas").build();

        when(habitIdentificationService.identifyHabit(555L)).thenReturn(Optional.of(habit));
        when(telegramUserResolver.resolve(any())).thenReturn(resolvedUser);

        consumer().consume(replyWithPhotoUpdate(555L, 42L, "Lucas", "bebi um copo"));

        ArgumentCaptor<HabitRecord> captor = ArgumentCaptor.forClass(HabitRecord.class);
        verify(habitRecordRepository).save(captor.capture());

        HabitRecord saved = captor.getValue();
        assertThat(saved.getUser()).isEqualTo(resolvedUser);
        assertThat(saved.getHabit()).isEqualTo(habit);
        assertThat(saved.getCaptionText()).isEqualTo("bebi um copo");
        assertThat(saved.getTelegramPhotoFileId()).isEqualTo("large");
        assertThat(saved.getTelegramMessageId()).isEqualTo(600L);
        assertThat(saved.getExtractedQuantity()).isNull();
        assertThat(saved.getLocalPhotoPath()).isNull();
    }

    @Test
    void ignoresUpdateWithoutMessage() {
        consumer().consume(new Update());

        verify(habitRecordRepository, never()).save(any());
    }

    @Test
    void ignoresMessageThatIsNotAReply() {
        Message message = Message.builder()
                .messageId(600)
                .from(org.telegram.telegrambots.meta.api.objects.User.builder().id(42L).firstName("Lucas").isBot(false).build())
                .photo(List.of(PhotoSize.builder().fileId("large").width(800).height(800).build()))
                .build();
        Update update = new Update();
        update.setMessage(message);

        consumer().consume(update);

        verify(habitRecordRepository, never()).save(any());
        verify(habitIdentificationService, never()).identifyHabit(any());
    }

    @Test
    void ignoresReplyWithoutPhoto() {
        Message repliedTo = Message.builder().messageId(555).build();
        Message message = Message.builder()
                .messageId(600)
                .from(org.telegram.telegrambots.meta.api.objects.User.builder().id(42L).firstName("Lucas").isBot(false).build())
                .replyToMessage(repliedTo)
                .build();
        Update update = new Update();
        update.setMessage(message);

        consumer().consume(update);

        verify(habitRecordRepository, never()).save(any());
        verify(habitIdentificationService, never()).identifyHabit(any());
    }

    @Test
    void ignoresReplyThatDoesNotMatchAnyKnownReminder() {
        when(habitIdentificationService.identifyHabit(999L)).thenReturn(Optional.empty());

        consumer().consume(replyWithPhotoUpdate(999L, 42L, "Lucas", "oi"));

        verify(habitRecordRepository, never()).save(any());
        verify(telegramUserResolver, never()).resolve(any());
    }
}
