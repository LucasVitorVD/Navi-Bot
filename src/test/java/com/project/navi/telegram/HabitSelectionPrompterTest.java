package com.project.navi.telegram;

import com.project.navi.domain.Habit;
import com.project.navi.domain.HabitType;
import com.project.navi.domain.PendingHabitSelection;
import com.project.navi.repository.HabitRepository;
import com.project.navi.repository.PendingHabitSelectionRepository;
import com.project.navi.time.AppZone;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HabitSelectionPrompterTest {

    @Mock
    private HabitRepository habitRepository;

    @Mock
    private PendingHabitSelectionRepository pendingHabitSelectionRepository;

    @Mock
    private TelegramReplySender telegramReplySender;

    private final Habit water = Habit.builder().id(1L).name("Água").type(HabitType.CUMULATIVE).unit("ml").target(3000).build();
    private final Habit goodFood = Habit.builder().id(4L).name("Alimentação boa").type(HabitType.BINARY).build();

    private final org.telegram.telegrambots.meta.api.objects.User sender =
            org.telegram.telegrambots.meta.api.objects.User.builder().id(42L).firstName("Lucas").isBot(false).build();

    private final Clock clock = Clock.fixed(Instant.parse("2026-07-27T10:00:00Z"), AppZone.ID);

    private HabitSelectionPrompter prompter() {
        return new HabitSelectionPrompter(habitRepository, pendingHabitSelectionRepository, telegramReplySender, clock);
    }

    @Test
    void savesPendingSelectionAndSendsOneButtonPerHabit() {
        when(habitRepository.findAll()).thenReturn(List.of(water, goodFood));
        when(pendingHabitSelectionRepository.save(any(PendingHabitSelection.class)))
                .thenAnswer(invocation -> {
                    PendingHabitSelection arg = invocation.getArgument(0);
                    arg.setId(77L);
                    return arg;
                });

        prompter().prompt(999L, 700, sender, "file-id", "bebi água");

        ArgumentCaptor<PendingHabitSelection> pendingCaptor = ArgumentCaptor.forClass(PendingHabitSelection.class);
        verify(pendingHabitSelectionRepository).save(pendingCaptor.capture());
        PendingHabitSelection saved = pendingCaptor.getValue();
        assertThat(saved.getTelegramChatId()).isEqualTo(999L);
        assertThat(saved.getTelegramUserId()).isEqualTo(42L);
        assertThat(saved.getTelegramPhotoFileId()).isEqualTo("file-id");
        assertThat(saved.getCaptionText()).isEqualTo("bebi água");
        assertThat(saved.getOriginalMessageId()).isEqualTo(700L);

        ArgumentCaptor<InlineKeyboardMarkup> keyboardCaptor = ArgumentCaptor.forClass(InlineKeyboardMarkup.class);
        verify(telegramReplySender).reply(eq(999L), eq(700), anyString(), keyboardCaptor.capture());

        List<InlineKeyboardRow> rows = keyboardCaptor.getValue().getKeyboard();
        List<InlineKeyboardButton> buttons = rows.stream().flatMap(List::stream).toList();
        assertThat(buttons).hasSize(2);
        assertThat(buttons).extracting(InlineKeyboardButton::getCallbackData)
                .containsExactlyInAnyOrder("habit-select:77:1", "habit-select:77:4");
    }
}
