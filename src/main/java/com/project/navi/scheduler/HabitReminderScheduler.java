package com.project.navi.scheduler;

import com.project.navi.domain.Habit;
import com.project.navi.domain.HabitReminderMessage;
import com.project.navi.domain.User;
import com.project.navi.progress.HabitProgressCalculator;
import com.project.navi.quote.MotivationalQuoteProvider;
import com.project.navi.quote.Quote;
import com.project.navi.repository.HabitReminderMessageRepository;
import com.project.navi.repository.HabitRepository;
import com.project.navi.repository.UserRepository;
import com.project.navi.telegram.TelegramMessagePinner;
import com.project.navi.telegram.TelegramReplySender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class HabitReminderScheduler {

    private final HabitRepository habitRepository;
    private final UserRepository userRepository;
    private final HabitReminderMessageRepository habitReminderMessageRepository;
    private final HabitProgressCalculator habitProgressCalculator;
    private final MotivationalQuoteProvider motivationalQuoteProvider;
    private final HabitReminderMessageFormatter messageFormatter;
    private final TelegramReplySender telegramReplySender;
    private final TelegramMessagePinner telegramMessagePinner;
    private final Clock clock;
    private final String groupChatId;

    public HabitReminderScheduler(HabitRepository habitRepository,
                                   UserRepository userRepository,
                                   HabitReminderMessageRepository habitReminderMessageRepository,
                                   HabitProgressCalculator habitProgressCalculator,
                                   MotivationalQuoteProvider motivationalQuoteProvider,
                                   HabitReminderMessageFormatter messageFormatter,
                                   TelegramReplySender telegramReplySender,
                                   TelegramMessagePinner telegramMessagePinner,
                                   Clock clock,
                                   @Value("${TELEGRAM_GROUP_CHAT_ID:}") String groupChatId) {
        this.habitRepository = habitRepository;
        this.userRepository = userRepository;
        this.habitReminderMessageRepository = habitReminderMessageRepository;
        this.habitProgressCalculator = habitProgressCalculator;
        this.motivationalQuoteProvider = motivationalQuoteProvider;
        this.messageFormatter = messageFormatter;
        this.telegramReplySender = telegramReplySender;
        this.telegramMessagePinner = telegramMessagePinner;
        this.clock = clock;
        this.groupChatId = groupChatId;
    }

    @Scheduled(cron = "0 30 7 * * *", zone = "America/Sao_Paulo")
    public void sendMorningReminders() {
        if (groupChatId.isBlank()) {
            return;
        }

        LocalDate today = LocalDate.now(clock);

        habitReminderMessageRepository.findByReferenceDate(today.minusDays(1))
                .forEach(reminder -> telegramMessagePinner.unpin(chatId(), reminder.getTelegramMessageId().intValue()));

        for (Habit habit : habitRepository.findAll()) {
            telegramReplySender.reply(chatId(), null, messageFormatter.morningReminder(habit))
                    .ifPresent(messageId -> {
                        habitReminderMessageRepository.save(HabitReminderMessage.builder()
                                .habit(habit)
                                .telegramMessageId(messageId.longValue())
                                .referenceDate(today)
                                .build());
                        telegramMessagePinner.pin(chatId(), messageId);
                    });
        }
    }

    @Scheduled(cron = "0 0 17 * * *", zone = "America/Sao_Paulo")
    public void sendReinforcementReminders() {
        if (groupChatId.isBlank()) {
            return;
        }

        LocalDate today = LocalDate.now(clock);
        List<Habit> habits = habitRepository.findAll();

        List<UserPendingHabits> pending = new ArrayList<>();
        for (User user : userRepository.findAll()) {
            List<Habit> pendingHabits = habits.stream()
                    .filter(habit -> !habitProgressCalculator.calculate(user, habit, today).isCompleted())
                    .toList();
            if (!pendingHabits.isEmpty()) {
                pending.add(new UserPendingHabits(user, pendingHabits));
            }
        }

        telegramReplySender.reply(chatId(), null, messageFormatter.reinforcementReminder(pending));
    }

    @Scheduled(cron = "0 0 22 * * *", zone = "America/Sao_Paulo")
    public void sendDailySummary() {
        if (groupChatId.isBlank()) {
            return;
        }

        LocalDate today = LocalDate.now(clock);
        List<Habit> habits = habitRepository.findAll();

        List<UserDailyProgress> progress = userRepository.findAll().stream()
                .map(user -> new UserDailyProgress(user, habits.stream()
                        .map(habit -> habitProgressCalculator.calculate(user, habit, today))
                        .toList()))
                .toList();

        Optional<Quote> quote = motivationalQuoteProvider.fetch();

        telegramReplySender.reply(chatId(), null, messageFormatter.dailySummary(progress, quote));
    }

    private long chatId() {
        return Long.parseLong(groupChatId);
    }
}
