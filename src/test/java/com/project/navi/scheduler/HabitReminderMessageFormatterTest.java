package com.project.navi.scheduler;

import com.project.navi.domain.Habit;
import com.project.navi.domain.HabitType;
import com.project.navi.domain.User;
import com.project.navi.progress.HabitProgress;
import com.project.navi.quote.Quote;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class HabitReminderMessageFormatterTest {

    private final HabitReminderMessageFormatter formatter = new HabitReminderMessageFormatter();

    private final Habit water = Habit.builder().name("Água").type(HabitType.CUMULATIVE).unit("ml").target(3000).build();
    private final Habit study = Habit.builder().name("Estudo").type(HabitType.CUMULATIVE).unit("min").target(180).build();
    private final Habit goodFood = Habit.builder().name("Alimentação saudável").type(HabitType.BINARY).build();

    private final User lucas = User.builder().id(1L).name("Lucas").build();
    private final User ana = User.builder().id(2L).name("Ana").build();

    @Test
    void morningReminderMentionsHabitAndTarget() {
        String message = formatter.morningReminder(water);

        assertThat(message).contains("💧").contains("3000ml");
    }

    @Test
    void morningReminderHasFallbackForUnknownHabit() {
        Habit other = Habit.builder().name("Meditação").type(HabitType.BINARY).build();

        assertThat(formatter.morningReminder(other)).contains("Meditação");
    }

    @Test
    void reinforcementReminderListsPendingHabitsPerUser() {
        String message = formatter.reinforcementReminder(List.of(
                new UserPendingHabits(lucas, List.of(new HabitProgress(water, 40), new HabitProgress(study, 0))),
                new UserPendingHabits(ana, List.of(new HabitProgress(study, 75)))));

        assertThat(message).contains("Lucas").contains("Água").contains("Estudo");
        assertThat(message).contains("Ana");
    }

    @Test
    void reinforcementReminderShowsProgressForPendingHabits() {
        String message = formatter.reinforcementReminder(List.of(
                new UserPendingHabits(lucas, List.of(new HabitProgress(water, 40)))));

        assertThat(message).contains("40%");
    }

    @Test
    void reinforcementReminderShowsCrossForPendingBinaryHabit() {
        String message = formatter.reinforcementReminder(List.of(
                new UserPendingHabits(lucas, List.of(new HabitProgress(goodFood, 0)))));

        assertThat(message).contains("❌");
    }

    @Test
    void reinforcementReminderCelebratesWhenNobodyIsPending() {
        String message = formatter.reinforcementReminder(List.of());

        assertThat(message.toLowerCase()).contains("bateu").doesNotContain("Lucas");
    }

    @Test
    void dailySummaryShowsPercentageForCumulativeAndCheckForBinary() {
        String message = formatter.dailySummary(List.of(
                new UserDailyProgress(lucas, List.of(
                        new HabitProgress(water, 100),
                        new HabitProgress(study, 50),
                        new HabitProgress(goodFood, 100)))
        ), Optional.empty());

        assertThat(message).contains("Lucas");
        assertThat(message).contains("100%");
        assertThat(message).contains("50%");
        assertThat(message).contains("✅");
    }

    @Test
    void dailySummaryShowsCrossForIncompleteBinaryHabit() {
        String message = formatter.dailySummary(List.of(
                new UserDailyProgress(ana, List.of(new HabitProgress(goodFood, 0)))
        ), Optional.empty());

        assertThat(message).contains("❌");
    }

    @Test
    void dailySummaryIncludesQuoteWhenPresent() {
        String message = formatter.dailySummary(List.of(), Optional.of(
                new Quote("Believe it!", "Naruto Uzumaki", "Naruto")));

        assertThat(message).contains("Believe it!").contains("Naruto Uzumaki").contains("Naruto");
    }

    @Test
    void dailySummaryOmitsQuoteSectionWhenAbsent() {
        String message = formatter.dailySummary(List.of(
                new UserDailyProgress(lucas, List.of(new HabitProgress(water, 100)))
        ), Optional.empty());

        assertThat(message).doesNotContain("💬");
    }
}
