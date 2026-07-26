package com.project.navi.quantity;

import com.project.navi.domain.Habit;
import com.project.navi.domain.HabitType;
import com.project.navi.domain.User;
import com.project.navi.domain.UserHabitConfig;
import com.project.navi.repository.UserHabitConfigRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HabitQuantityInterpreterTest {

    @Mock
    private UserHabitConfigRepository userHabitConfigRepository;

    @Mock
    private GeminiMinutesExtractor geminiMinutesExtractor;

    private final User user = User.builder().id(1L).telegramUserId(42L).name("Lucas").build();

    private final Habit water = Habit.builder().id(1L).name("Água").type(HabitType.CUMULATIVE).unit("ml").target(3000).build();
    private final Habit study = Habit.builder().id(2L).name("Estudo").type(HabitType.CUMULATIVE).unit("min").target(180).build();

    private HabitQuantityInterpreter interpreter() {
        return new HabitQuantityInterpreter(userHabitConfigRepository, geminiMinutesExtractor);
    }

    @Test
    void interpretsWaterQuantityFromPersonalConfig() {
        UserHabitConfig config = UserHabitConfig.builder().user(user).habit(water).personalUnitValue(500).build();
        when(userHabitConfigRepository.findByUserAndHabit(user, water)).thenReturn(Optional.of(config));

        Optional<Integer> result = interpreter().interpret(user, water, "bebi um copo");

        assertThat(result).contains(500);
        verify(geminiMinutesExtractor, never()).extractMinutes(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void returnsEmptyWhenWaterIsNotYetConfigured() {
        when(userHabitConfigRepository.findByUserAndHabit(user, water)).thenReturn(Optional.empty());

        assertThat(interpreter().interpret(user, water, "bebi um copo")).isEmpty();
    }

    @Test
    void interpretsStudyMinutesViaGemini() {
        when(geminiMinutesExtractor.extractMinutes("estudei 40 minutos de java")).thenReturn(Optional.of(40));

        Optional<Integer> result = interpreter().interpret(user, study, "estudei 40 minutos de java");

        assertThat(result).contains(40);
        verify(userHabitConfigRepository, never()).findByUserAndHabit(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void returnsEmptyWhenGeminiCannotExtractMinutes() {
        when(geminiMinutesExtractor.extractMinutes("oi")).thenReturn(Optional.empty());

        assertThat(interpreter().interpret(user, study, "oi")).isEmpty();
    }

    @Test
    void failureMessageForWaterMentionsConfigCommand() {
        String message = interpreter().failureMessageFor(water);

        assertThat(message).contains("/config");
    }

    @Test
    void failureMessageForNonWaterAsksToReformulate() {
        String message = interpreter().failureMessageFor(study);

        assertThat(message).doesNotContain("/config");
        assertThat(message.toLowerCase()).contains("reformular");
    }
}
