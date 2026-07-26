package com.project.navi.repository;

import com.project.navi.domain.Habit;
import com.project.navi.domain.HabitReminderMessage;
import com.project.navi.domain.HabitType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class HabitReminderMessageRepositoryTest {

    @Autowired
    private HabitRepository habitRepository;

    @Autowired
    private HabitReminderMessageRepository habitReminderMessageRepository;

    @Test
    void savesAndFindsReminderMessageByTelegramMessageId() {
        Habit habit = habitRepository.save(Habit.builder()
                .name("Água")
                .type(HabitType.CUMULATIVE)
                .unit("ml")
                .target(3000)
                .build());

        habitReminderMessageRepository.save(HabitReminderMessage.builder()
                .habit(habit)
                .telegramMessageId(555L)
                .referenceDate(LocalDate.of(2026, 7, 27))
                .build());

        Optional<HabitReminderMessage> found = habitReminderMessageRepository.findByTelegramMessageId(555L);

        assertThat(found).isPresent();
        assertThat(found.get().getHabit().getId()).isEqualTo(habit.getId());
        assertThat(found.get().getReferenceDate()).isEqualTo(LocalDate.of(2026, 7, 27));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void habitIsFullyInitializedEvenAfterRepositoryCallEnds() {
        // Reproduz o cenário de produção: HabitReplyUpdateConsumer roda numa thread de
        // long polling comum, fora de qualquer sessão/transação HTTP. Cada chamada de
        // repositório abre e fecha sua própria transação. Sem NOT_SUPPORTED, o @DataJpaTest
        // manteria tudo numa única transação de teste e esconderia o bug (LazyInitializationException).
        Habit habit = habitRepository.save(Habit.builder()
                .name("Água")
                .type(HabitType.CUMULATIVE)
                .unit("ml")
                .target(3000)
                .build());

        habitReminderMessageRepository.save(HabitReminderMessage.builder()
                .habit(habit)
                .telegramMessageId(4242L)
                .referenceDate(LocalDate.of(2026, 7, 27))
                .build());

        try {
            Optional<HabitReminderMessage> found = habitReminderMessageRepository.findByTelegramMessageId(4242L);

            assertThat(found).isPresent();
            assertThatCode(() -> found.get().getHabit().getType()).doesNotThrowAnyException();
            assertThat(found.get().getHabit().getType()).isEqualTo(HabitType.CUMULATIVE);
        } finally {
            // NOT_SUPPORTED significa que não há rollback automático de transação de teste;
            // como o contexto (e o banco) é cacheado e reutilizado entre classes de teste,
            // é preciso limpar manualmente para não vazar dados para outros testes.
            habitReminderMessageRepository.deleteAll();
            habitRepository.deleteAll();
        }
    }

    @Test
    void findByTelegramMessageIdReturnsEmptyWhenNoReminderMatches() {
        assertThat(habitReminderMessageRepository.findByTelegramMessageId(999L)).isEmpty();
    }

    @Test
    void findsAllRemindersForAGivenReferenceDate() {
        Habit water = habitRepository.save(Habit.builder()
                .name("Água").type(HabitType.CUMULATIVE).unit("ml").target(3000).build());
        Habit study = habitRepository.save(Habit.builder()
                .name("Estudo").type(HabitType.CUMULATIVE).unit("min").target(180).build());

        habitReminderMessageRepository.save(HabitReminderMessage.builder()
                .habit(water).telegramMessageId(1001L).referenceDate(LocalDate.of(2026, 7, 26)).build());
        habitReminderMessageRepository.save(HabitReminderMessage.builder()
                .habit(study).telegramMessageId(1002L).referenceDate(LocalDate.of(2026, 7, 26)).build());
        habitReminderMessageRepository.save(HabitReminderMessage.builder()
                .habit(water).telegramMessageId(1003L).referenceDate(LocalDate.of(2026, 7, 27)).build());

        List<HabitReminderMessage> found = habitReminderMessageRepository.findByReferenceDate(LocalDate.of(2026, 7, 26));

        assertThat(found).extracting(HabitReminderMessage::getTelegramMessageId)
                .containsExactlyInAnyOrder(1001L, 1002L);
    }

    @Test
    void rejectsDuplicateTelegramMessageId() {
        Habit habit = habitRepository.save(Habit.builder()
                .name("Estudo")
                .type(HabitType.CUMULATIVE)
                .unit("min")
                .target(180)
                .build());

        habitReminderMessageRepository.saveAndFlush(HabitReminderMessage.builder()
                .habit(habit)
                .telegramMessageId(777L)
                .referenceDate(LocalDate.of(2026, 7, 27))
                .build());

        assertThatThrownBy(() -> habitReminderMessageRepository.saveAndFlush(HabitReminderMessage.builder()
                .habit(habit)
                .telegramMessageId(777L)
                .referenceDate(LocalDate.of(2026, 7, 27))
                .build()))
                .isInstanceOf(DataAccessException.class);
    }
}
