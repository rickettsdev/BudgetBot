package com.parable.observer;

import java.util.Map;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;

import lombok.AllArgsConstructor;
import lombok.Builder;

@AllArgsConstructor
@Builder
public class TelegramObserver implements Observer {
    private static final Map<Observer.MessageTemplate, String> MESSAGES = Map.of(
        Observer.MessageTemplate.ERROR, "I'm having trouble with that command. Please try again later.",
        Observer.MessageTemplate.HELP, "Here are the valid commands you can run:\n/add <cost> <message>\n/getTotalToday\n/getTotalWeek\n/getTotalMonth",
        Observer.MessageTemplate.MESSAGE_SENT, "Cost added to your budget."
     );
     private final Long chatId;
     private final TelegramBot bot;

    @Override
    public void update(MessageTemplate message) {
        bot.execute(new SendMessage(chatId, MESSAGES.get(message)));
    }

    @Override
    public void update(String message) {
        bot.execute(new SendMessage(chatId, message));
    }

}
