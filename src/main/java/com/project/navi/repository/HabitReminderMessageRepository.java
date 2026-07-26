package com.project.navi.repository;

import com.project.navi.domain.HabitReminderMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HabitReminderMessageRepository extends JpaRepository<HabitReminderMessage, Long> {

    Optional<HabitReminderMessage> findByTelegramMessageId(Long telegramMessageId);
}
