package com.parable.command;

import com.parable.model.PurchaseModel;
import com.parable.observer.CommandMonitor;
import com.parable.observer.TelegramMessageConstants;

import lombok.Builder;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Builder
public class AddCost implements Command {
    private final DynamoDbEnhancedClient client;
    private final CommandMonitor monitor;
    private final PurchaseModel model;
    private final String tableName;

    @Override
    public void execute() {
        DynamoDbTable<PurchaseModel> table = client.table(tableName, TableSchema.fromClass(PurchaseModel.class));
        table.putItem(model);
        monitor.notifyObservers(String.format(TelegramMessageConstants.MESSAGE_SENT, model.getCost(), model.getMessage()));
    }
}
