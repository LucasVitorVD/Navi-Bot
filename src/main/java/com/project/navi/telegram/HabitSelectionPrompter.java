package com.project.navi.telegram;

import com.project.navi.domain.PendingHabitSelection;
import com.project.navi.repository.HabitRepository;
import com.project.navi.repository.PendingHabitSelectionRepository;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Fallback para quando uma foto chega sem ser reply a nenhum lembrete conhecido (ex: a
 * mensagem original ficou inacessível para quem enviou). Pergunta qual hábito é, por botão,
 * em vez de exigir que a pessoa encontre e responda à mensagem certa.
 */
@Component
public class HabitSelectionPrompter {

    private static final String PROMPT_TEXT = "Não consegui saber a qual hábito essa foto se refere. Qual é?";

    private final HabitRepository habitRepository;
    private final PendingHabitSelectionRepository pendingHabitSelectionRepository;
    private final TelegramReplySender telegramReplySender;
    private final Clock clock;

    public HabitSelectionPrompter(HabitRepository habitRepository,
                                   PendingHabitSelectionRepository pendingHabitSelectionRepository,
                                   TelegramReplySender telegramReplySender,
                                   Clock clock) {
        this.habitRepository = habitRepository;
        this.pendingHabitSelectionRepository = pendingHabitSelectionRepository;
        this.telegramReplySender = telegramReplySender;
        this.clock = clock;
    }

    public void prompt(Long chatId, Integer originalMessageId, org.telegram.telegrambots.meta.api.objects.User sender,
                        String photoFileId, String captionText) {
        PendingHabitSelection pending = pendingHabitSelectionRepository.save(PendingHabitSelection.builder()
                .telegramChatId(chatId)
                .telegramUserId(sender.getId())
                .telegramPhotoFileId(photoFileId)
                .captionText(captionText)
                .originalMessageId(originalMessageId.longValue())
                .createdAt(Instant.now(clock))
                .build());

        List<InlineKeyboardRow> rows = habitRepository.findAll().stream()
                .map(habit -> new InlineKeyboardRow(InlineKeyboardButton.builder()
                        .text(habit.getName())
                        .callbackData("habit-select:" + pending.getId() + ":" + habit.getId())
                        .build()))
                .toList();

        telegramReplySender.reply(chatId, originalMessageId, PROMPT_TEXT,
                InlineKeyboardMarkup.builder().keyboard(rows).build());
    }
}
