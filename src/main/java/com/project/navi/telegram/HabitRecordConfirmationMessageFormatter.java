package com.project.navi.telegram;

import com.project.navi.domain.Habit;
import com.project.navi.domain.HabitType;
import com.project.navi.domain.User;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class HabitRecordConfirmationMessageFormatter {

    private static final Map<String, String> HABIT_EMOJIS = Map.of(
            "Água", "💧",
            "Estudo", "📚",
            "Cardio", "🏃",
            "Alimentação saudável", "🥗"
    );

    public String confirmationFor(User user, Habit habit, Integer quantityJustAdded, int remaining) {
        String emoji = HABIT_EMOJIS.getOrDefault(habit.getName(), "✅");
        String greeting = "Parabéns " + user.getName() + "! " + emoji + " " + habit.getName() + " registrado(a).";

        if (habit.getType() == HabitType.BINARY) {
            return greeting;
        }

        if (remaining <= 0) {
            return greeting + " Meta de hoje batida! 🎉";
        }

        if (isWater(habit) && quantityJustAdded != null && quantityJustAdded > 0) {
            int bottlesRemaining = (int) Math.ceil(remaining / (double) quantityJustAdded);
            return greeting + " Faltam " + remaining + "ml (~" + bottlesRemaining
                    + (bottlesRemaining == 1 ? " garrafa" : " garrafas") + ") para bater a meta de hoje.";
        }

        return greeting + " Faltam " + remaining + habit.getUnit() + " para bater a meta de hoje.";
    }

    private boolean isWater(Habit habit) {
        return "ml".equals(habit.getUnit());
    }
}
