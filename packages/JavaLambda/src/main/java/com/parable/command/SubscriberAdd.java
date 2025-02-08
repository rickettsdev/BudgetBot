package com.parable.command;

import java.util.UUID;

import com.amazonaws.services.lambda.runtime.Context;
import com.parable.model.SubscriberModel;
import com.parable.observer.CommandMonitor;
import com.parable.observer.TelegramMessageConstants;

import lombok.Builder;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Builder
public class SubscriberAdd implements Command {
    private final DynamoDbEnhancedClient client;
    private final CommandMonitor monitor;
    private final String subscriberId;
    private final String ownerId;
    private final String subscriberBotToken;
    private final String tableName;
    private final Context context;

    @Override
    public void execute() {
        String token = UUID.randomUUID().toString(); // probably should handle elsewhere, and make easier to test.
        SubscriberModel model = SubscriberModel.builder()
                                    .botToken(subscriberBotToken)
                                    .ownerId(ownerId)
                                    .subscriber(subscriberId)
                                    .token(token)
                                .build();
        DynamoDbTable<SubscriberModel> table = client.table(tableName, TableSchema.fromClass(SubscriberModel.class));
        try {
        table.putItem(model);
        } catch (Exception e) {
            context.getLogger().log("model: " + model.toString());
            context.getLogger().log("e: " + e.getMessage());
            context.getLogger().log("tableName: " + tableName);
            throw e;
        }
        monitor.notifyObservers(String.format(TelegramMessageConstants.SUBSCRIBER_ADDED, token));
    }

}
