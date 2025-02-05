package com.parable.module;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parable.command.CommandFactory;
import com.parable.command.CommandInvoker;
import com.parable.observer.CommandMonitor;
import com.parable.provider.DynamoDBPurchaseRecordProvider;

import dagger.Module;
import dagger.Provides;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

@Module(includes = {AWSModule.class, ObserverModule.class})
public class CommandModule {
    public static final String TABLE_NAME = System.getenv("TABLE_NAME");

    @Provides
    public CommandFactory getCommandFactory(DynamoDbEnhancedClient enhancedClient, CommandMonitor monitor,
     BedrockRuntimeClient bedrockClient, ObjectMapper mapper, DynamoDBPurchaseRecordProvider provider) {
        return CommandFactory.builder()
                .bedrockClient(bedrockClient)
                .client(enhancedClient)
                .tableName(TABLE_NAME)
                .monitor(monitor)
                .provider(provider)
                .build();
    }

    @Provides
    public CommandInvoker getCommandInvoker() {
        return new CommandInvoker();
    }
}
