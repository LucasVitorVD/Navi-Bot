package com.project.navi.telegram;

import com.project.navi.domain.Habit;
import com.project.navi.domain.User;
import com.project.navi.domain.UserHabitConfig;
import com.project.navi.repository.HabitRepository;
import com.project.navi.repository.UserHabitConfigRepository;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class HabitConfigCommandConsumer {

    private static final String USAGE_MESSAGE = "Use: /config água <valor>ml (ex: /config água 500ml)";
    private static final Pattern DIGITS = Pattern.compile("\\d+");

    private final TelegramUserResolver telegramUserResolver;
    private final HabitRepository habitRepository;
    private final UserHabitConfigRepository userHabitConfigRepository;
    private final TelegramReplySender telegramReplySender;

    public HabitConfigCommandConsumer(TelegramUserResolver telegramUserResolver,
                                       HabitRepository habitRepository,
                                       UserHabitConfigRepository userHabitConfigRepository,
                                       TelegramReplySender telegramReplySender) {
        this.telegramUserResolver = telegramUserResolver;
        this.habitRepository = habitRepository;
        this.userHabitConfigRepository = userHabitConfigRepository;
        this.telegramReplySender = telegramReplySender;
    }

    public void consume(Update update) {
        Message message = update.getMessage();
        if (message == null || !message.hasText() || !isConfigCommand(message.getText())) {
            return;
        }

        String[] parts = message.getText().trim().split("\\s+");
        if (parts.length < 3 || !isWaterToken(parts[1])) {
            telegramReplySender.reply(message.getChatId(), message.getMessageId(), USAGE_MESSAGE);
            return;
        }

        Optional<Integer> value = parseMilliliters(parts[2]);
        if (value.isEmpty()) {
            telegramReplySender.reply(message.getChatId(), message.getMessageId(),
                    "Não entendi o valor. " + USAGE_MESSAGE);
            return;
        }

        Optional<Habit> waterHabit = habitRepository.findByName("Água");
        if (waterHabit.isEmpty()) {
            telegramReplySender.reply(message.getChatId(), message.getMessageId(),
                    "Não encontrei o hábito Água cadastrado.");
            return;
        }

        Habit habit = waterHabit.get();
        User user = telegramUserResolver.resolve(message.getFrom());

        UserHabitConfig config = userHabitConfigRepository.findByUserAndHabit(user, habit)
                .orElseGet(() -> UserHabitConfig.builder().user(user).habit(habit).build());
        config.setPersonalUnitValue(value.get());
        userHabitConfigRepository.save(config);

        telegramReplySender.reply(message.getChatId(), message.getMessageId(),
                "Configurado! " + value.get() + "ml por garrafa de Água.");
    }

    private boolean isConfigCommand(String text) {
        String firstToken = text.trim().split("\\s+")[0].toLowerCase();
        return firstToken.equals("/config") || firstToken.startsWith("/config@");
    }

    private boolean isWaterToken(String token) {
        return token.equalsIgnoreCase("água") || token.equalsIgnoreCase("agua");
    }

    private Optional<Integer> parseMilliliters(String token) {
        Matcher matcher = DIGITS.matcher(token);
        if (!matcher.find()) {
            return Optional.empty();
        }
        int value = Integer.parseInt(matcher.group());
        return value > 0 ? Optional.of(value) : Optional.empty();
    }
}
