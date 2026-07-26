package com.project.navi.telegram;

import com.project.navi.domain.Habit;
import com.project.navi.domain.HabitRecord;
import com.project.navi.domain.HabitType;
import com.project.navi.domain.User;
import com.project.navi.reminder.HabitIdentificationService;
import com.project.navi.repository.HabitRecordRepository;
import com.project.navi.repository.UserRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HabitReplyUpdateConsumerTest {

    @Mock
    private HabitIdentificationService habitIdentificationService;

    @Mock
    private UserRepository userRepository;

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
        return new HabitReplyUpdateConsumer(habitIdentificationService, userRepository, habitRecordRepository);
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
    void savesHabitRecordForKnownUserRepliyingToKnownReminder() {
        User existingUser = User.builder().id(10L).telegramUserId(42L).name("Lucas").build();

        when(habitIdentificationService.identifyHabit(555L)).thenReturn(Optional.of(habit));
        when(userRepository.findByTelegramUserId(42L)).thenReturn(Optional.of(existingUser));

        consumer().consume(replyWithPhotoUpdate(555L, 42L, "Lucas", "bebi um copo"));

        ArgumentCaptor<HabitRecord> captor = ArgumentCaptor.forClass(HabitRecord.class);
        verify(habitRecordRepository).save(captor.capture());

        HabitRecord saved = captor.getValue();
        assertThat(saved.getUser()).isEqualTo(existingUser);
        assertThat(saved.getHabit()).isEqualTo(habit);
        assertThat(saved.getCaptionText()).isEqualTo("bebi um copo");
        assertThat(saved.getTelegramPhotoFileId()).isEqualTo("large");
        assertThat(saved.getTelegramMessageId()).isEqualTo(600L);
        assertThat(saved.getExtractedQuantity()).isNull();
        assertThat(saved.getLocalPhotoPath()).isNull();
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createsNewUserWhenSenderIsNotYetKnown() {
        User newlyCreatedUser = User.builder().id(11L).telegramUserId(99L).name("Beto").build();

        when(habitIdentificationService.identifyHabit(555L)).thenReturn(Optional.of(habit));
        when(userRepository.findByTelegramUserId(99L)).thenReturn(Optional.empty());
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class))).thenReturn(newlyCreatedUser);

        consumer().consume(replyWithPhotoUpdate(555L, 99L, "Beto", null));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getTelegramUserId()).isEqualTo(99L);
        assertThat(userCaptor.getValue().getName()).isEqualTo("Beto");

        ArgumentCaptor<HabitRecord> recordCaptor = ArgumentCaptor.forClass(HabitRecord.class);
        verify(habitRecordRepository).save(recordCaptor.capture());
        assertThat(recordCaptor.getValue().getUser()).isEqualTo(newlyCreatedUser);
    }

    @Test
    void ignoresUpdateWithoutMessage() {
        consumer().consume(new Update());

        verify(habitRecordRepository, never()).save(org.mockito.ArgumentMatchers.any());
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

        verify(habitRecordRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(habitIdentificationService, never()).identifyHabit(org.mockito.ArgumentMatchers.any());
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

        verify(habitRecordRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(habitIdentificationService, never()).identifyHabit(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void ignoresReplyThatDoesNotMatchAnyKnownReminder() {
        when(habitIdentificationService.identifyHabit(999L)).thenReturn(Optional.empty());

        consumer().consume(replyWithPhotoUpdate(999L, 42L, "Lucas", "oi"));

        verify(habitRecordRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(userRepository, never()).findByTelegramUserId(org.mockito.ArgumentMatchers.any());
    }
}
