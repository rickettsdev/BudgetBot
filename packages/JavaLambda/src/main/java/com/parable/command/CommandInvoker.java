package com.parable.command;

public class CommandInvoker {
    public void executeCommand(Command command) {
        command.execute();
    }
}
