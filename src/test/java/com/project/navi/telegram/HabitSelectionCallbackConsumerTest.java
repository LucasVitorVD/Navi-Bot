package com.project.navi.telegram;

import com.project.navi.domain.Habit;
import com.project.navi.domain.HabitType;
import com.project.navi.domain.PendingHabitSelection;
import com.project.navi.repository.HabitRepository;
import com.project.navi.repository.PendingHabitSelectionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HabitSelectionCallbackConsumerTest {

    @Mock
    private PendingHabitSelectionRepository pendingHabitSelectionRepository;

    @Mock
    private HabitRepository habitRepository;

    @Mock
    private HabitRegistrationService habitRegistrationService;

    @Mock
    private TelegramClient telegramClient;

    private final Habit water = Habit.builder().id(1L).name("Água").type(HabitType.CUMULATIVE).unit("ml").target(3000).build();

    private final User tapper = User.builder().id(42L).firstName("Lucas").isBot(false).build();

    private final PendingHabitSelection pending = PendingHabitSelection.builder()
            .id(77L)
            .telegramChatId(999L)
            .telegramUserId(42L)
            .telegramPhotoFileId("file-id")
            .captionText("bebi água")
            .originalMessageId(700L)
            .createdAt(Instant.parse("2026-07-27T10:00:00Z"))
            .build();

    private HabitSelectionCallbackConsumer consumer() {
        return new HabitSelectionCallbackConsumer(pendingHabitSelectionRepository, habitRepository,
                habitRegistrationService, Optional.of(telegramClient));
    }

    private Update callbackUpdate(String data, User from) {
        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setId("cbq-1");
        callbackQuery.setData(data);
        callbackQuery.setFrom(from);
        Update update = new Update();
        update.setCallbackQuery(callbackQuery);
        return update;
    }

    @Test
    void registersHabitAndDeletesPendingSelectionWhenCallbackMatches() throws TelegramApiException {
        when(pendingHabitSelectionRepository.findById(77L)).thenReturn(Optional.of(pending));
        when(habitRepository.findById(1L)).thenReturn(Optional.of(water));

        consumer().consume(callbackUpdate("habit-select:77:1", tapper));

        verify(habitRegistrationService).register(tapper, water, "bebi água", "file-id", 999L, 700L);
        verify(pendingHabitSelectionRepository).deleteById(77L);

        ArgumentCaptor<AnswerCallbackQuery> captor = ArgumentCaptor.forClass(AnswerCallbackQuery.class);
        verify(telegramClient).execute(captor.capture());
        assertThat(captor.getValue().getCallbackQueryId()).isEqualTo("cbq-1");
    }

    @Test
    void ignoresCallbackWithUnrelatedData() {
        consumer().consume(callbackUpdate("something-else:1:2", tapper));

        verify(habitRegistrationService, never()).register(any(), any(), any(), any(), any(), any());
        verify(pendingHabitSelectionRepository, never()).deleteById(any());
    }

    @Test
    void ignoresUpdateWithoutCallbackQuery() {
        assertThatCode(() -> consumer().consume(new Update())).doesNotThrowAnyException();

        verify(habitRegistrationService, never()).register(any(), any(), any(), any(), any(), any());
    }

    @Test
    void answersWithAlertWhenPendingSelectionNoLongerExists() throws TelegramApiException {
        when(pendingHabitSelectionRepository.findById(77L)).thenReturn(Optional.empty());

        consumer().consume(callbackUpdate("habit-select:77:1", tapper));

        verify(habitRegistrationService, never()).register(any(), any(), any(), any(), any(), any());
        verify(pendingHabitSelectionRepository, never()).deleteById(any());

        ArgumentCaptor<AnswerCallbackQuery> captor = ArgumentCaptor.forClass(AnswerCallbackQuery.class);
        verify(telegramClient).execute(captor.capture());
        assertThat(captor.getValue().getText()).isNotBlank();
    }

    @Test
    void answersWithAlertWhenHabitNoLongerExists() throws TelegramApiException {
        when(pendingHabitSelectionRepository.findById(77L)).thenReturn(Optional.of(pending));
        when(habitRepository.findById(1L)).thenReturn(Optional.empty());

        consumer().consume(callbackUpdate("habit-select:77:1", tapper));

        verify(habitRegistrationService, never()).register(any(), any(), any(), any(), any(), any());
        verify(pendingHabitSelectionRepository, never()).deleteById(any());
    }

    @Test
    void answersWithAlertAndDoesNotRegisterWhenWrongPersonTaps() throws TelegramApiException {
        User someoneElse = User.builder().id(99L).firstName("Outra Pessoa").isBot(false).build();
        when(pendingHabitSelectionRepository.findById(77L)).thenReturn(Optional.of(pending));
        when(habitRepository.findById(1L)).thenReturn(Optional.of(water));

        consumer().consume(callbackUpdate("habit-select:77:1", someoneElse));

        verify(habitRegistrationService, never()).register(any(), any(), any(), any(), any(), any());
        verify(pendingHabitSelectionRepository, never()).deleteById(any());

        ArgumentCaptor<AnswerCallbackQuery> captor = ArgumentCaptor.forClass(AnswerCallbackQuery.class);
        verify(telegramClient).execute(captor.capture());
        assertThat(captor.getValue().getText()).isNotBlank();
    }

    @Test
    void ignoresMalformedCallbackData() {
        consumer().consume(callbackUpdate("habit-select:not-a-number:1", tapper));

        verify(habitRegistrationService, never()).register(any(), any(), any(), any(), any(), any());
        verify(pendingHabitSelectionRepository, never()).deleteById(any());
    }

    @Test
    void doesNothingWhenTelegramClientIsNotConfigured() {
        HabitSelectionCallbackConsumer consumer = new HabitSelectionCallbackConsumer(pendingHabitSelectionRepository,
                habitRepository, habitRegistrationService, Optional.empty());
        when(pendingHabitSelectionRepository.findById(77L)).thenReturn(Optional.of(pending));
        when(habitRepository.findById(1L)).thenReturn(Optional.of(water));

        assertThatCode(() -> consumer.consume(callbackUpdate("habit-select:77:1", tapper))).doesNotThrowAnyException();

        verify(habitRegistrationService).register(tapper, water, "bebi água", "file-id", 999L, 700L);
    }
}
