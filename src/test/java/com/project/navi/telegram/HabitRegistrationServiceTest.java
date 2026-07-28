package com.project.navi.telegram;

import com.project.navi.domain.Habit;
import com.project.navi.domain.HabitRecord;
import com.project.navi.domain.HabitType;
import com.project.navi.domain.User;
import com.project.navi.photo.PhotoStorage;
import com.project.navi.progress.HabitProgressCalculator;
import com.project.navi.quantity.HabitQuantityInterpreter;
import com.project.navi.repository.HabitRecordRepository;
import com.project.navi.time.AppZone;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HabitRegistrationServiceTest {

    @Mock
    private TelegramUserResolver telegramUserResolver;

    @Mock
    private HabitQuantityInterpreter habitQuantityInterpreter;

    @Mock
    private HabitProgressCalculator habitProgressCalculator;

    @Mock
    private HabitRecordConfirmationMessageFormatter confirmationMessageFormatter;

    @Mock
    private TelegramReplySender telegramReplySender;

    @Mock
    private HabitRecordRepository habitRecordRepository;

    @Mock
    private PhotoStorage photoStorage;

    private final User user = User.builder().id(10L).telegramUserId(42L).name("Lucas").build();
    private final org.telegram.telegrambots.meta.api.objects.User sender =
            org.telegram.telegrambots.meta.api.objects.User.builder().id(42L).firstName("Lucas").isBot(false).build();

    private final Habit water = Habit.builder().id(1L).name("Água").type(HabitType.CUMULATIVE).unit("ml").target(3000).build();
    private final Habit goodFood = Habit.builder().id(4L).name("Alimentação saudável").type(HabitType.BINARY).build();

    private final Clock clock = Clock.fixed(Instant.parse("2026-07-27T02:59:59Z"), AppZone.ID); // 23:59:59 em Brasília
    private final LocalDate today = LocalDate.of(2026, 7, 26);

    private HabitRegistrationService service() {
        return new HabitRegistrationService(telegramUserResolver, habitQuantityInterpreter, habitProgressCalculator,
                confirmationMessageFormatter, telegramReplySender, habitRecordRepository, photoStorage, clock);
    }

    @Test
    void registersBinaryHabitAndSendsConfirmation() {
        when(telegramUserResolver.resolve(sender)).thenReturn(user);
        when(photoStorage.download("file-id", today, 700L)).thenReturn(Optional.of("/app/fotos/2026-07-26/700.jpg"));
        when(confirmationMessageFormatter.confirmationFor(user, goodFood, null, 0, true))
                .thenReturn("Parabéns Lucas! Alimentação saudável registrado(a).");

        service().register(sender, goodFood, null, "file-id", 999L, 700L);

        ArgumentCaptor<HabitRecord> captor = ArgumentCaptor.forClass(HabitRecord.class);
        verify(habitRecordRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isEqualTo(user);
        assertThat(captor.getValue().getHabit()).isEqualTo(goodFood);
        assertThat(captor.getValue().getExtractedQuantity()).isNull();
        assertThat(captor.getValue().getLocalPhotoPath()).isEqualTo("/app/fotos/2026-07-26/700.jpg");
        assertThat(captor.getValue().getReferenceDate()).isEqualTo(today);
        assertThat(captor.getValue().getTelegramMessageId()).isEqualTo(700L);

        verify(telegramReplySender).reply(999L, 700, "Parabéns Lucas! Alimentação saudável registrado(a).");
    }

    @Test
    void registersCumulativeHabitAndSendsConfirmationWithRemaining() {
        when(telegramUserResolver.resolve(sender)).thenReturn(user);
        when(habitQuantityInterpreter.interpret(user, water, "bebi um copo")).thenReturn(Optional.of(500));
        when(habitProgressCalculator.remaining(user, water, today)).thenReturn(2500);
        when(confirmationMessageFormatter.confirmationFor(user, water, 500, 2500, true))
                .thenReturn("Parabéns Lucas! Faltam 2500ml (~5 garrafas).");

        service().register(sender, water, "bebi um copo", "file-id", 999L, 700L);

        ArgumentCaptor<HabitRecord> captor = ArgumentCaptor.forClass(HabitRecord.class);
        verify(habitRecordRepository).save(captor.capture());
        assertThat(captor.getValue().getExtractedQuantity()).isEqualTo(500);

        verify(telegramReplySender).reply(999L, 700, "Parabéns Lucas! Faltam 2500ml (~5 garrafas).");
    }

    @Test
    void doesNotSaveAndSendsFailureMessageWhenQuantityCannotBeInterpreted() {
        when(telegramUserResolver.resolve(sender)).thenReturn(user);
        when(habitQuantityInterpreter.interpret(user, water, "bebi um copo")).thenReturn(Optional.empty());
        when(habitQuantityInterpreter.failureMessageFor(water)).thenReturn("Configure sua garrafa");

        service().register(sender, water, "bebi um copo", "file-id", 999L, 700L);

        verify(habitRecordRepository, never()).save(any());
        verify(telegramReplySender).reply(999L, 700, "Configure sua garrafa");
        verify(photoStorage, never()).download(any(), any(), org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void doesNotWarnAboutDailySummaryWhenRegisteredBeforeIt() {
        Clock beforeSummary = Clock.fixed(Instant.parse("2026-07-26T20:00:00Z"), AppZone.ID); // 17:00 em Brasília
        when(telegramUserResolver.resolve(sender)).thenReturn(user);
        when(photoStorage.download(any(), any(), org.mockito.ArgumentMatchers.anyLong())).thenReturn(Optional.empty());
        when(confirmationMessageFormatter.confirmationFor(user, goodFood, null, 0, false))
                .thenReturn("Parabéns Lucas! Alimentação saudável registrado(a).");

        new HabitRegistrationService(telegramUserResolver, habitQuantityInterpreter, habitProgressCalculator,
                confirmationMessageFormatter, telegramReplySender, habitRecordRepository, photoStorage, beforeSummary)
                .register(sender, goodFood, null, "file-id", 999L, 700L);

        verify(confirmationMessageFormatter).confirmationFor(user, goodFood, null, 0, false);
    }

    @Test
    void warnsAboutDailySummaryWhenRegisteredAtOrAfterIt() {
        Clock atSummaryTime = Clock.fixed(Instant.parse("2026-07-28T02:40:00Z"), AppZone.ID); // 23:40 em Brasília
        when(telegramUserResolver.resolve(sender)).thenReturn(user);
        when(photoStorage.download(any(), any(), org.mockito.ArgumentMatchers.anyLong())).thenReturn(Optional.empty());
        when(confirmationMessageFormatter.confirmationFor(user, goodFood, null, 0, true))
                .thenReturn("Parabéns Lucas! Alimentação saudável registrado(a). ⚠️ Chegou depois do resumo.");

        new HabitRegistrationService(telegramUserResolver, habitQuantityInterpreter, habitProgressCalculator,
                confirmationMessageFormatter, telegramReplySender, habitRecordRepository, photoStorage, atSummaryTime)
                .register(sender, goodFood, null, "file-id", 999L, 700L);

        verify(confirmationMessageFormatter).confirmationFor(user, goodFood, null, 0, true);
    }

    @Test
    void savesRecordWithNullLocalPhotoPathWhenDownloadFails() {
        when(telegramUserResolver.resolve(sender)).thenReturn(user);
        when(photoStorage.download(any(), any(), org.mockito.ArgumentMatchers.anyLong())).thenReturn(Optional.empty());

        service().register(sender, goodFood, null, "file-id", 999L, 700L);

        ArgumentCaptor<HabitRecord> captor = ArgumentCaptor.forClass(HabitRecord.class);
        verify(habitRecordRepository).save(captor.capture());
        assertThat(captor.getValue().getLocalPhotoPath()).isNull();
    }
}
