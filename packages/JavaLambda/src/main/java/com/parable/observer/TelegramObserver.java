package com.parable.observer;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;

import lombok.AllArgsConstructor;
import lombok.Builder;

@AllArgsConstructor
@Builder
public class TelegramObserver implements Observer {
     private final Long chatId;
     private final TelegramBot bot;

    @Override
    public void update(String message) {
        bot.execute(new SendMessage(chatId, message));
    }

}
