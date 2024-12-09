package com.parable.command;

import java.util.Arrays;

import com.amazonaws.services.lambda.runtime.Context;
import com.parable.command.GetTotalSinceTime.TimeSince;
import com.parable.model.PurchaseModel;
import com.parable.observer.CommandMonitor;

import lombok.Builder;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;

@Builder
public class CommandFactory {
    private final DynamoDbEnhancedClient client;
    private final CommandMonitor monitor;
    private final String tableName;
    @Setter
    private Context context;

    public Command createCommand(String[] tokens, String userId, Long timestamp) {
        String commandString = tokens.length > 0 ? tokens[0] : "";
        Command command;
        switch (commandString) {
            case "/add" : 
                command = tokens.length > 1 ? AddCost.builder()
                    .client(client)
                    .model(PurchaseModel.builder()
                            .id(userId)
                            .timestamp(timestamp)
                            .message(Arrays.toString(tokens))
                            .cost(Double.valueOf(tokens[1]))
                            .build())
                    .monitor(monitor)
                    .tableName(tableName)
                    .build() : null;
                break;
            case "/getTotalToday" : 
                command = getTotalTimeSince(TimeSince.TODAY, userId);
                break;
            case "/getTotalWeek" : 
                command = getTotalTimeSince(TimeSince.THISWEEK, userId);
                break;
            case "/getTotalMonth" : 
                command = getTotalTimeSince(TimeSince.THISMONTH, userId);
                break;
            default :
                command = CommandList.builder()
                    .monitor(monitor)
                    .build();
        }
        return command;
    }

    private GetTotalSinceTime getTotalTimeSince(TimeSince time, String userId) {
        return GetTotalSinceTime.builder()
            .client(client)
            .monitor(monitor)
            .tableName(tableName)
            .timeSince(time)
            .chatId(userId)
            .context(context)
            .build();
    }
}
