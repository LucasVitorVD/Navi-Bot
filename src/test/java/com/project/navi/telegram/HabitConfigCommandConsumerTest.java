package com.project.navi.telegram;

import com.project.navi.domain.Habit;
import com.project.navi.domain.HabitType;
import com.project.navi.domain.User;
import com.project.navi.domain.UserHabitConfig;
import com.project.navi.repository.HabitRepository;
import com.project.navi.repository.UserHabitConfigRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HabitConfigCommandConsumerTest {

    @Mock
    private TelegramUserResolver telegramUserResolver;

    @Mock
    private HabitRepository habitRepository;

    @Mock
    private UserHabitConfigRepository userHabitConfigRepository;

    @Mock
    private TelegramReplySender telegramReplySender;

    private final User user = User.builder().id(1L).telegramUserId(42L).name("Lucas").build();
    private final Habit water = Habit.builder().id(1L).name("Água").type(HabitType.CUMULATIVE).unit("ml").target(3000).build();

    private HabitConfigCommandConsumer consumer() {
        return new HabitConfigCommandConsumer(telegramUserResolver, habitRepository, userHabitConfigRepository, telegramReplySender);
    }

    private Update commandUpdate(String text) {
        Message message = Message.builder()
                .messageId(700)
                .chat(Chat.builder().id(999L).type("group").build())
                .from(org.telegram.telegrambots.meta.api.objects.User.builder().id(42L).firstName("Lucas").isBot(false).build())
                .text(text)
                .build();
        Update update = new Update();
        update.setMessage(message);
        return update;
    }

    @Test
    void createsNewConfigWhenNotYetConfigured() {
        when(habitRepository.findByName("Água")).thenReturn(Optional.of(water));
        when(telegramUserResolver.resolve(any())).thenReturn(user);
        when(userHabitConfigRepository.findByUserAndHabit(user, water)).thenReturn(Optional.empty());
        when(userHabitConfigRepository.save(any(UserHabitConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        consumer().consume(commandUpdate("/config água 500ml"));

        ArgumentCaptor<UserHabitConfig> captor = ArgumentCaptor.forClass(UserHabitConfig.class);
        verify(userHabitConfigRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isEqualTo(user);
        assertThat(captor.getValue().getHabit()).isEqualTo(water);
        assertThat(captor.getValue().getPersonalUnitValue()).isEqualTo(500);

        verify(telegramReplySender).reply(org.mockito.ArgumentMatchers.eq(999L), org.mockito.ArgumentMatchers.eq(700),
                org.mockito.ArgumentMatchers.contains("500"));
    }

    @Test
    void updatesExistingConfigWhenAlreadyConfigured() {
        UserHabitConfig existing = UserHabitConfig.builder().id(9L).user(user).habit(water).personalUnitValue(300).build();

        when(habitRepository.findByName("Água")).thenReturn(Optional.of(water));
        when(telegramUserResolver.resolve(any())).thenReturn(user);
        when(userHabitConfigRepository.findByUserAndHabit(user, water)).thenReturn(Optional.of(existing));
        when(userHabitConfigRepository.save(any(UserHabitConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        consumer().consume(commandUpdate("/config água 750ml"));

        ArgumentCaptor<UserHabitConfig> captor = ArgumentCaptor.forClass(UserHabitConfig.class);
        verify(userHabitConfigRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(9L);
        assertThat(captor.getValue().getPersonalUnitValue()).isEqualTo(750);
    }

    @Test
    void repliesWithUsageWhenHabitTokenIsMissing() {
        consumer().consume(commandUpdate("/config"));

        verify(userHabitConfigRepository, never()).save(any());
        verify(telegramReplySender).reply(org.mockito.ArgumentMatchers.eq(999L), org.mockito.ArgumentMatchers.eq(700),
                org.mockito.ArgumentMatchers.contains("/config água"));
    }

    @Test
    void repliesWithUsageWhenHabitTokenIsNotWater() {
        consumer().consume(commandUpdate("/config estudo 500"));

        verify(userHabitConfigRepository, never()).save(any());
        verify(telegramReplySender).reply(org.mockito.ArgumentMatchers.eq(999L), org.mockito.ArgumentMatchers.eq(700),
                org.mockito.ArgumentMatchers.contains("/config água"));
        verify(habitRepository, never()).findByName(any());
    }

    @Test
    void repliesWithErrorWhenValueIsNotANumber() {
        consumer().consume(commandUpdate("/config água muito"));

        verify(userHabitConfigRepository, never()).save(any());
        verify(telegramReplySender).reply(org.mockito.ArgumentMatchers.eq(999L), org.mockito.ArgumentMatchers.eq(700),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void ignoresNonConfigTextMessages() {
        consumer().consume(commandUpdate("oi pessoal"));

        verify(userHabitConfigRepository, never()).save(any());
        verify(telegramReplySender, never()).reply(any(), any(), any());
    }

    @Test
    void ignoresUpdateWithoutMessage() {
        consumer().consume(new Update());

        verify(userHabitConfigRepository, never()).save(any());
    }
}
