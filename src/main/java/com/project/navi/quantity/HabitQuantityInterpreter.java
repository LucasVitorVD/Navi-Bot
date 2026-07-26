package com.project.navi.quantity;

import com.project.navi.domain.Habit;
import com.project.navi.domain.User;
import com.project.navi.domain.UserHabitConfig;
import com.project.navi.repository.UserHabitConfigRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class HabitQuantityInterpreter {

    private static final String WATER_UNIT = "ml";

    private final UserHabitConfigRepository userHabitConfigRepository;
    private final GeminiMinutesExtractor geminiMinutesExtractor;

    public HabitQuantityInterpreter(UserHabitConfigRepository userHabitConfigRepository,
                                     GeminiMinutesExtractor geminiMinutesExtractor) {
        this.userHabitConfigRepository = userHabitConfigRepository;
        this.geminiMinutesExtractor = geminiMinutesExtractor;
    }

    public Optional<Integer> interpret(User user, Habit habit, String captionText) {
        if (isWater(habit)) {
            return userHabitConfigRepository.findByUserAndHabit(user, habit)
                    .map(UserHabitConfig::getPersonalUnitValue);
        }
        return geminiMinutesExtractor.extractMinutes(captionText);
    }

    public String failureMessageFor(Habit habit) {
        if (isWater(habit)) {
            return "Ainda não configurei o tamanho da sua garrafa de água. "
                    + "Responda com /config água <valor>ml (ex: /config água 500ml) e envie a foto de novo.";
        }
        return "Não consegui entender a quantidade de " + habit.getName().toLowerCase()
                + ". Pode reformular a legenda e enviar de novo?";
    }

    private boolean isWater(Habit habit) {
        return WATER_UNIT.equals(habit.getUnit());
    }
}
