package com.parable.module;

import com.parable.command.CommandFactory;
import com.parable.command.CommandInvoker;
import com.parable.observer.CommandMonitor;

import dagger.Module;
import dagger.Provides;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;

@Module(includes = {ObserverModule.class})
public class CommandModule {
    private static final String TABLE_NAME = System.getenv("TABLE_NAME");

    @Provides
    public CommandFactory getCommandFactory(DynamoDbEnhancedClient enhancedClient, CommandMonitor monitor) {
        return CommandFactory.builder()
                .client(enhancedClient)
                .tableName(TABLE_NAME)
                .monitor(monitor)
                .build();
    }

    @Provides
    public CommandInvoker getCommandInvoker() {
        return new CommandInvoker();
    }
}
