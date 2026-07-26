package com.project.navi.repository;

import com.project.navi.domain.Habit;
import com.project.navi.domain.HabitType;
import com.project.navi.domain.User;
import com.project.navi.domain.UserHabitConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserHabitConfigRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HabitRepository habitRepository;

    @Autowired
    private UserHabitConfigRepository userHabitConfigRepository;

    @Test
    void savesPersonalBottleSizeConfigLinkedToUserAndHabit() {
        User user = userRepository.save(User.builder()
                .telegramUserId(42L)
                .name("Lucas")
                .createdAt(Instant.now())
                .build());

        Habit habit = habitRepository.save(Habit.builder()
                .name("Água")
                .type(HabitType.CUMULATIVE)
                .unit("ml")
                .target(3000)
                .build());

        UserHabitConfig config = UserHabitConfig.builder()
                .user(user)
                .habit(habit)
                .personalUnitValue(500)
                .build();

        UserHabitConfig saved = userHabitConfigRepository.save(config);

        Optional<UserHabitConfig> found = userHabitConfigRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getUser().getId()).isEqualTo(user.getId());
        assertThat(found.get().getHabit().getId()).isEqualTo(habit.getId());
        assertThat(found.get().getPersonalUnitValue()).isEqualTo(500);
    }

    @Test
    void findsConfigByUserAndHabit() {
        User user = userRepository.save(User.builder()
                .telegramUserId(43L)
                .name("Ana")
                .createdAt(Instant.now())
                .build());

        Habit habit = habitRepository.save(Habit.builder()
                .name("Água")
                .type(HabitType.CUMULATIVE)
                .unit("ml")
                .target(3000)
                .build());

        userHabitConfigRepository.save(UserHabitConfig.builder()
                .user(user)
                .habit(habit)
                .personalUnitValue(750)
                .build());

        Optional<UserHabitConfig> found = userHabitConfigRepository.findByUserAndHabit(user, habit);

        assertThat(found).isPresent();
        assertThat(found.get().getPersonalUnitValue()).isEqualTo(750);
    }

    @Test
    void findByUserAndHabitReturnsEmptyWhenNotConfigured() {
        User user = userRepository.save(User.builder()
                .telegramUserId(44L)
                .name("Beto")
                .createdAt(Instant.now())
                .build());

        Habit habit = habitRepository.save(Habit.builder()
                .name("Água")
                .type(HabitType.CUMULATIVE)
                .unit("ml")
                .target(3000)
                .build());

        assertThat(userHabitConfigRepository.findByUserAndHabit(user, habit)).isEmpty();
    }
}
