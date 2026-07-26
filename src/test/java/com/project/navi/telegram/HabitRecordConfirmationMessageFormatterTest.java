package com.project.navi.telegram;

import com.project.navi.domain.Habit;
import com.project.navi.domain.HabitType;
import com.project.navi.domain.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HabitRecordConfirmationMessageFormatterTest {

    private final HabitRecordConfirmationMessageFormatter formatter = new HabitRecordConfirmationMessageFormatter();

    private final User lucas = User.builder().id(1L).name("Lucas").build();
    private final Habit water = Habit.builder().name("Água").type(HabitType.CUMULATIVE).unit("ml").target(3000).build();
    private final Habit study = Habit.builder().name("Estudo").type(HabitType.CUMULATIVE).unit("min").target(180).build();
    private final Habit goodFood = Habit.builder().name("Alimentação saudável").type(HabitType.BINARY).build();

    @Test
    void congratulatesByNameForBinaryHabitWithoutRemainingInfo() {
        String message = formatter.confirmationFor(lucas, goodFood, null, 0);

        assertThat(message).contains("Lucas").contains("Alimentação saudável");
        assertThat(message).doesNotContain("Faltam");
    }

    @Test
    void showsRemainingInBottlesForWater() {
        // registrou 500ml agora, e ainda faltam 1500ml pra bater a meta de 3000ml
        String message = formatter.confirmationFor(lucas, water, 500, 1500);

        assertThat(message).contains("Lucas");
        assertThat(message).contains("1500ml");
        assertThat(message).contains("3 garrafas");
    }

    @Test
    void usesSingularBottleWhenOnlyOneRemains() {
        String message = formatter.confirmationFor(lucas, water, 1000, 1000);

        assertThat(message).contains("1 garrafa").doesNotContain("1 garrafas");
    }

    @Test
    void showsRemainingInUnitForNonWaterCumulativeHabit() {
        String message = formatter.confirmationFor(lucas, study, 40, 140);

        assertThat(message).contains("Lucas").contains("140min");
        assertThat(message).doesNotContain("garrafa");
    }

    @Test
    void celebratesWhenGoalIsAlreadyReached() {
        String message = formatter.confirmationFor(lucas, water, 500, 0);

        assertThat(message.toLowerCase()).contains("meta").contains("bat");
        assertThat(message).doesNotContain("Faltam");
    }
}
