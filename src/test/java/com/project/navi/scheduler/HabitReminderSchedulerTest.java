package com.project.navi.scheduler;

import com.project.navi.domain.Habit;
import com.project.navi.domain.HabitReminderMessage;
import com.project.navi.domain.HabitType;
import com.project.navi.domain.User;
import com.project.navi.progress.HabitProgress;
import com.project.navi.progress.HabitProgressCalculator;
import com.project.navi.quote.MotivationalQuoteProvider;
import com.project.navi.quote.Quote;
import com.project.navi.repository.HabitRepository;
import com.project.navi.repository.HabitReminderMessageRepository;
import com.project.navi.repository.UserRepository;
import com.project.navi.telegram.TelegramMessagePinner;
import com.project.navi.telegram.TelegramReplySender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HabitReminderSchedulerTest {

    @Mock
    private HabitRepository habitRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HabitReminderMessageRepository habitReminderMessageRepository;

    @Mock
    private HabitProgressCalculator habitProgressCalculator;

    @Mock
    private MotivationalQuoteProvider motivationalQuoteProvider;

    @Mock
    private TelegramReplySender telegramReplySender;

    @Mock
    private TelegramMessagePinner telegramMessagePinner;

    private final HabitReminderMessageFormatter formatter = new HabitReminderMessageFormatter();

    private final Habit water = Habit.builder().id(1L).name("Água").type(HabitType.CUMULATIVE).unit("ml").target(3000).build();
    private final Habit study = Habit.builder().id(2L).name("Estudo").type(HabitType.CUMULATIVE).unit("min").target(180).build();

    private final User lucas = User.builder().id(1L).telegramUserId(42L).name("Lucas").build();

    private final Clock clock = Clock.fixed(Instant.parse("2026-07-27T10:30:00Z"), com.project.navi.time.AppZone.ID);

    private HabitReminderScheduler scheduler(String groupChatId) {
        return new HabitReminderScheduler(habitRepository, userRepository, habitReminderMessageRepository,
                habitProgressCalculator, motivationalQuoteProvider, formatter, telegramReplySender,
                telegramMessagePinner, clock, groupChatId);
    }

    @Test
    void sendsOneMorningReminderPerHabitPersistsAndPinsIt() {
        when(habitRepository.findAll()).thenReturn(List.of(water, study));
        when(habitReminderMessageRepository.findByReferenceDate(LocalDate.of(2026, 7, 26))).thenReturn(List.of());
        when(telegramReplySender.reply(eq(999L), eq(null), any())).thenReturn(Optional.of(501), Optional.of(502));

        scheduler("999").sendMorningReminders();

        verify(telegramReplySender, times(2)).reply(eq(999L), eq(null), any());

        ArgumentCaptor<HabitReminderMessage> captor = ArgumentCaptor.forClass(HabitReminderMessage.class);
        verify(habitReminderMessageRepository, times(2)).save(captor.capture());

        List<HabitReminderMessage> saved = captor.getAllValues();
        assertThat(saved).extracting(HabitReminderMessage::getTelegramMessageId).containsExactly(501L, 502L);
        assertThat(saved).extracting(m -> m.getHabit().getId()).containsExactly(1L, 2L);
        assertThat(saved).allSatisfy(m -> assertThat(m.getReferenceDate()).isEqualTo(LocalDate.of(2026, 7, 27)));

        verify(telegramMessagePinner).pin(999L, 501);
        verify(telegramMessagePinner).pin(999L, 502);
    }

    @Test
    void unpinsYesterdaysRemindersBeforeSendingNewOnes() {
        HabitReminderMessage yesterdayReminder = HabitReminderMessage.builder()
                .habit(water).telegramMessageId(400L).referenceDate(LocalDate.of(2026, 7, 26)).build();
        when(habitReminderMessageRepository.findByReferenceDate(LocalDate.of(2026, 7, 26)))
                .thenReturn(List.of(yesterdayReminder));
        when(habitRepository.findAll()).thenReturn(List.of());

        scheduler("999").sendMorningReminders();

        verify(telegramMessagePinner).unpin(999L, 400);
    }

    @Test
    void doesNotSaveOrPinReminderMessageWhenSendFails() {
        when(habitRepository.findAll()).thenReturn(List.of(water));
        when(habitReminderMessageRepository.findByReferenceDate(any())).thenReturn(List.of());
        when(telegramReplySender.reply(eq(999L), eq(null), any())).thenReturn(Optional.empty());

        scheduler("999").sendMorningReminders();

        verify(habitReminderMessageRepository, never()).save(any());
        verify(telegramMessagePinner, never()).pin(any(), any());
    }

    @Test
    void doesNothingWhenGroupChatIdIsBlank() {
        scheduler("").sendMorningReminders();

        verify(habitRepository, never()).findAll();
        verify(telegramReplySender, never()).reply(anyLong(), any(), any());
        verify(telegramMessagePinner, never()).pin(any(), any());
        verify(telegramMessagePinner, never()).unpin(any(), any());
    }

    @Test
    void reinforcementRemindersOnlyMentionPendingHabits() {
        when(habitRepository.findAll()).thenReturn(List.of(water, study));
        when(userRepository.findAll()).thenReturn(List.of(lucas));
        when(habitProgressCalculator.calculate(lucas, water, LocalDate.of(2026, 7, 27)))
                .thenReturn(new HabitProgress(water, 100));
        when(habitProgressCalculator.calculate(lucas, study, LocalDate.of(2026, 7, 27)))
                .thenReturn(new HabitProgress(study, 40));

        scheduler("999").sendReinforcementReminders();

        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        verify(telegramReplySender).reply(eq(999L), eq(null), textCaptor.capture());

        assertThat(textCaptor.getValue()).contains("Lucas").contains("Estudo").doesNotContain("Água:");
    }

    @Test
    void dailySummaryIncludesProgressForEveryUserAndQuoteWhenAvailable() {
        when(habitRepository.findAll()).thenReturn(List.of(water));
        when(userRepository.findAll()).thenReturn(List.of(lucas));
        when(habitProgressCalculator.calculate(lucas, water, LocalDate.of(2026, 7, 27)))
                .thenReturn(new HabitProgress(water, 80));
        when(motivationalQuoteProvider.fetch()).thenReturn(Optional.of(new Quote("Believe it!", "Naruto", "Naruto")));

        scheduler("999").sendDailySummary();

        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        verify(telegramReplySender).reply(eq(999L), eq(null), textCaptor.capture());

        assertThat(textCaptor.getValue()).contains("Lucas").contains("80%").contains("Believe it!");
    }
}
