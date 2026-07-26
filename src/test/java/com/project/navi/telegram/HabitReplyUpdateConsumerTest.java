package com.project.navi.telegram;

import com.project.navi.domain.Habit;
import com.project.navi.domain.HabitRecord;
import com.project.navi.domain.HabitType;
import com.project.navi.domain.User;
import com.project.navi.quantity.HabitQuantityInterpreter;
import com.project.navi.reminder.HabitIdentificationService;
import com.project.navi.repository.HabitRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
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
    private HabitQuantityInterpreter habitQuantityInterpreter;

    @Mock
    private TelegramReplySender telegramReplySender;

    @Mock
    private HabitRecordRepository habitRecordRepository;

    private final User user = User.builder().id(10L).telegramUserId(42L).name("Lucas").build();

    private final Habit water = Habit.builder().id(1L).name("Água").type(HabitType.CUMULATIVE).unit("ml").target(3000).build();
    private final Habit goodFood = Habit.builder().id(4L).name("Alimentação boa").type(HabitType.BINARY).build();

    private HabitReplyUpdateConsumer consumer() {
        return new HabitReplyUpdateConsumer(habitIdentificationService, telegramUserResolver,
                habitQuantityInterpreter, telegramReplySender, habitRecordRepository);
    }

    private Update replyWithPhotoUpdate(long repliedToMessageId, long chatId, String caption) {
        Message repliedTo = Message.builder()
                .messageId((int) repliedToMessageId)
                .build();

        Message message = Message.builder()
                .messageId(600)
                .chat(Chat.builder().id(chatId).type("group").build())
                .from(org.telegram.telegrambots.meta.api.objects.User.builder().id(42L).firstName("Lucas").isBot(false).build())
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
    void savesRecordForBinaryHabitWithoutCallingInterpreter() {
        when(habitIdentificationService.identifyHabit(555L)).thenReturn(Optional.of(goodFood));
        when(telegramUserResolver.resolve(any())).thenReturn(user);

        consumer().consume(replyWithPhotoUpdate(555L, 999L, null));

        ArgumentCaptor<HabitRecord> captor = ArgumentCaptor.forClass(HabitRecord.class);
        verify(habitRecordRepository).save(captor.capture());
        assertThat(captor.getValue().getExtractedQuantity()).isNull();
        assertThat(captor.getValue().getHabit()).isEqualTo(goodFood);

        verify(habitQuantityInterpreter, never()).interpret(any(), any(), any());
        verify(telegramReplySender, never()).reply(any(), any(), any());
    }

    @Test
    void savesRecordWithInterpretedQuantityForCumulativeHabit() {
        when(habitIdentificationService.identifyHabit(555L)).thenReturn(Optional.of(water));
        when(telegramUserResolver.resolve(any())).thenReturn(user);
        when(habitQuantityInterpreter.interpret(user, water, "bebi um copo")).thenReturn(Optional.of(500));

        consumer().consume(replyWithPhotoUpdate(555L, 999L, "bebi um copo"));

        ArgumentCaptor<HabitRecord> captor = ArgumentCaptor.forClass(HabitRecord.class);
        verify(habitRecordRepository).save(captor.capture());
        assertThat(captor.getValue().getExtractedQuantity()).isEqualTo(500);

        verify(telegramReplySender, never()).reply(any(), any(), any());
    }

    @Test
    void doesNotSaveAndSendsFailureMessageWhenQuantityCannotBeInterpreted() {
        when(habitIdentificationService.identifyHabit(555L)).thenReturn(Optional.of(water));
        when(telegramUserResolver.resolve(any())).thenReturn(user);
        when(habitQuantityInterpreter.interpret(user, water, "bebi um copo")).thenReturn(Optional.empty());
        when(habitQuantityInterpreter.failureMessageFor(water)).thenReturn("Configure sua garrafa");

        consumer().consume(replyWithPhotoUpdate(555L, 999L, "bebi um copo"));

        verify(habitRecordRepository, never()).save(any());
        verify(telegramReplySender).reply(999L, 600, "Configure sua garrafa");
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

        consumer().consume(replyWithPhotoUpdate(999L, 1L, "oi"));

        verify(habitRecordRepository, never()).save(any());
        verify(telegramUserResolver, never()).resolve(any());
    }
}
