package com.project.navi.scheduler;

import com.project.navi.domain.Habit;
import com.project.navi.domain.HabitType;
import com.project.navi.progress.HabitProgress;
import com.project.navi.quote.Quote;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class HabitReminderMessageFormatter {

    private static final Map<String, String> HABIT_EMOJIS = Map.of(
            "Água", "💧",
            "Estudo", "📚",
            "Cardio", "🏃",
            "Alimentação boa", "🥗"
    );

    public String morningReminder(Habit habit) {
        String emoji = emojiFor(habit);
        return switch (habit.getName()) {
            case "Água" -> emoji + " Bora beber água! Meta de hoje: " + habit.getTarget()
                    + "ml. Responda esta mensagem com uma foto a cada garrafa 🫗";
            case "Estudo" -> emoji + " Hora de estudar! Meta de hoje: " + habit.getTarget()
                    + "min. Responda com foto + quanto tempo você estudou 🧠";
            case "Cardio" -> emoji + " Cardio chamando! Meta de hoje: " + habit.getTarget()
                    + "min. Foto + tempo aqui na resposta 🔥";
            case "Alimentação boa" -> emoji + " Comeu bem hoje? Responda esta mensagem com uma foto"
                    + " quando rolar uma refeição boa 🍽️";
            default -> emoji + " " + habit.getName() + " — bora lá! Responda esta mensagem com uma foto.";
        };
    }

    public String reinforcementReminder(List<UserPendingHabits> pendingByUser) {
        if (pendingByUser.isEmpty()) {
            return "🎉 Mandaram muito bem! Todo mundo já bateu os hábitos de hoje.";
        }

        StringBuilder message = new StringBuilder("⏰ Reforço da tarde! Ainda dá tempo:\n\n");
        for (UserPendingHabits entry : pendingByUser) {
            String habits = entry.pendingHabits().stream()
                    .map(habit -> emojiFor(habit) + " " + habit.getName())
                    .collect(Collectors.joining(", "));
            message.append("👤 ").append(entry.user().getName()).append(": ").append(habits).append("\n");
        }
        message.append("\nBora não deixar pra última hora 😉");
        return message.toString();
    }

    public String dailySummary(List<UserDailyProgress> progressByUser, Optional<Quote> quote) {
        StringBuilder message = new StringBuilder("📊 Resumo do dia:\n\n");
        for (UserDailyProgress entry : progressByUser) {
            String line = entry.progress().stream()
                    .map(this::formatProgress)
                    .collect(Collectors.joining(" "));
            message.append("👤 ").append(entry.user().getName()).append(": ").append(line).append("\n");
        }

        quote.ifPresent(q -> {
            message.append("\n💬 \"").append(q.content()).append("\"");
            if (q.character() != null) {
                message.append(" — ").append(q.character());
                if (q.source() != null) {
                    message.append(" (").append(q.source()).append(")");
                }
            }
        });

        return message.toString();
    }

    private String formatProgress(HabitProgress progress) {
        String emoji = emojiFor(progress.habit());
        if (progress.habit().getType() == HabitType.BINARY) {
            return emoji + (progress.isCompleted() ? "✅" : "❌");
        }
        return emoji + progress.percentage() + "%";
    }

    private String emojiFor(Habit habit) {
        return HABIT_EMOJIS.getOrDefault(habit.getName(), "✅");
    }
}
