package com.parable.command;

import com.parable.observer.CommandMonitor;
import com.parable.observer.TelegramMessageConstants;

import lombok.Builder;


@Builder
public class CommandList implements Command {
    private final CommandMonitor monitor;

    @Override
    public void execute() {
        monitor.notifyObservers(TelegramMessageConstants.HELP);
    }

}
