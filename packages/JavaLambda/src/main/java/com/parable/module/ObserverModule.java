package com.parable.module;

import java.util.List;

import com.parable.observer.CommandMonitor;
import com.parable.observer.TelegramObserver;
import com.pengrad.telegrambot.TelegramBot;

import dagger.Module;
import dagger.Provides;

@Module
public class ObserverModule {
    public static final long TOM_CHAT_ID = Long.parseLong(System.getenv("CHAT_ID"));
    private static final String BOT_TOKEN = System.getenv("BOT_TOKEN");
    
    @Provides
    public CommandMonitor getMonitor() {
        TelegramBot budgetBot = new TelegramBot(BOT_TOKEN);
        TelegramObserver observer = new TelegramObserver(TOM_CHAT_ID, budgetBot);
        return CommandMonitor.builder().observers(List.of(observer)).build();
    }
}
