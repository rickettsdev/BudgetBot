package com.parable.module;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parable.command.CommandFactory;
import com.parable.command.CommandInvoker;
import com.parable.provider.DynamoDBPurchaseRecordProvider;

import dagger.Module;
import dagger.Provides;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

@Module(includes = {AWSModule.class, ObserverModule.class})
public class CommandModule {
    public static final String PURCHASE_TABLE_NAME = System.getenv("PURCHASE_TABLE_NAME");
    public static final String SUBSCRIBER_TABLE_NAME = System.getenv("SUBSCRIBER_TABLE_NAME");

    @Provides
    public CommandFactory getCommandFactory(DynamoDbEnhancedClient enhancedClient,
     BedrockRuntimeClient bedrockClient, ObjectMapper mapper, DynamoDBPurchaseRecordProvider provider) {
        return CommandFactory.builder()
                .bedrockClient(bedrockClient)
                .client(enhancedClient)
                .purchaseTableName(PURCHASE_TABLE_NAME)
                .subscriberTableName(SUBSCRIBER_TABLE_NAME)
                .provider(provider)
                .build();
    }

    @Provides
    public CommandInvoker getCommandInvoker() {
        return new CommandInvoker();
    }
}
