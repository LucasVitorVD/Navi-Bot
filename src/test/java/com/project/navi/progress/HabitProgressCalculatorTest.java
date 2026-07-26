package com.project.navi.progress;

import com.project.navi.domain.Habit;
import com.project.navi.domain.HabitRecord;
import com.project.navi.domain.HabitType;
import com.project.navi.domain.User;
import com.project.navi.repository.HabitRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HabitProgressCalculatorTest {

    @Mock
    private HabitRecordRepository habitRecordRepository;

    private final User user = User.builder().id(1L).telegramUserId(42L).name("Lucas").build();
    private final Habit water = Habit.builder().id(1L).name("Água").type(HabitType.CUMULATIVE).unit("ml").target(3000).build();
    private final Habit goodFood = Habit.builder().id(4L).name("Alimentação boa").type(HabitType.BINARY).build();
    private final LocalDate today = LocalDate.of(2026, 7, 27);

    private HabitProgressCalculator calculator() {
        return new HabitProgressCalculator(habitRecordRepository);
    }

    private HabitRecord recordWith(Integer quantity) {
        return HabitRecord.builder().user(user).habit(water).referenceDate(today).extractedQuantity(quantity).build();
    }

    @Test
    void isZeroPercentWhenNoRecordsForCumulativeHabit() {
        when(habitRecordRepository.findByUserAndHabitAndReferenceDate(user, water, today)).thenReturn(List.of());

        HabitProgress progress = calculator().calculate(user, water, today);

        assertThat(progress.percentage()).isEqualTo(0);
        assertThat(progress.isCompleted()).isFalse();
    }

    @Test
    void sumsMultipleRecordsForCumulativeHabit() {
        when(habitRecordRepository.findByUserAndHabitAndReferenceDate(user, water, today))
                .thenReturn(List.of(recordWith(500), recordWith(1000)));

        HabitProgress progress = calculator().calculate(user, water, today);

        assertThat(progress.percentage()).isEqualTo(50);
    }

    @Test
    void capsAtHundredPercentWhenRecordsExceedTarget() {
        when(habitRecordRepository.findByUserAndHabitAndReferenceDate(user, water, today))
                .thenReturn(List.of(recordWith(2000), recordWith(2000)));

        HabitProgress progress = calculator().calculate(user, water, today);

        assertThat(progress.percentage()).isEqualTo(100);
        assertThat(progress.isCompleted()).isTrue();
    }

    @Test
    void isZeroPercentForBinaryHabitWithoutRecord() {
        when(habitRecordRepository.findByUserAndHabitAndReferenceDate(user, goodFood, today)).thenReturn(List.of());

        HabitProgress progress = calculator().calculate(user, goodFood, today);

        assertThat(progress.percentage()).isEqualTo(0);
    }

    @Test
    void isHundredPercentForBinaryHabitWithAnyRecord() {
        HabitRecord record = HabitRecord.builder().user(user).habit(goodFood).referenceDate(today).build();
        when(habitRecordRepository.findByUserAndHabitAndReferenceDate(user, goodFood, today)).thenReturn(List.of(record));

        HabitProgress progress = calculator().calculate(user, goodFood, today);

        assertThat(progress.percentage()).isEqualTo(100);
        assertThat(progress.isCompleted()).isTrue();
    }

    @Test
    void remainingIsTargetMinusSumWhenUnderTarget() {
        when(habitRecordRepository.findByUserAndHabitAndReferenceDate(user, water, today))
                .thenReturn(List.of(recordWith(500), recordWith(1000)));

        assertThat(calculator().remaining(user, water, today)).isEqualTo(1500);
    }

    @Test
    void remainingIsZeroNotNegativeWhenTargetExceeded() {
        when(habitRecordRepository.findByUserAndHabitAndReferenceDate(user, water, today))
                .thenReturn(List.of(recordWith(2000), recordWith(2000)));

        assertThat(calculator().remaining(user, water, today)).isEqualTo(0);
    }

    @Test
    void remainingEqualsTargetWhenNoRecordsYet() {
        when(habitRecordRepository.findByUserAndHabitAndReferenceDate(user, water, today)).thenReturn(List.of());

        assertThat(calculator().remaining(user, water, today)).isEqualTo(3000);
    }
}
