package com.parable.command;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.amazonaws.services.lambda.runtime.Context;
import com.parable.model.LLMPromptModel;
import com.parable.model.PurchaseModel;
import com.parable.observer.CommandMonitor;
import com.parable.provider.DynamoDBPurchaseRecordProvider;
import com.parable.provider.DynamoDBPurchaseRecordProvider.TimeSince;

import lombok.Builder;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

@Builder
public class CommandFactory {
    private final BedrockRuntimeClient bedrockClient;
    private final DynamoDbEnhancedClient client;
    private final DynamoDBPurchaseRecordProvider provider;
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
                            .message(Stream.of(tokens).skip(2).collect(Collectors.joining(" ")))
                            .cost(Double.valueOf(tokens[1]))
                            .build())
                    .monitor(monitor)
                    .tableName(tableName)
                    .build() : null;
                break;
            case "/getTotalToday" : 
                command = getTotalTimeSince(TimeSince.TODAY);
                break;
            case "/getTotalWeek" : 
                command = getTotalTimeSince(TimeSince.THISWEEK);
                break;
            case "/getTotalMonth" : 
                command = getTotalTimeSince(TimeSince.THISMONTH);
                break;
            case "/ask":
                command = LLMPrompt.builder()
                .bedrockClient(bedrockClient)
                .context(context)
                .model(
                    LLMPromptModel.builder()
                        .modelId("us.meta.llama3-3-70b-instruct-v1:0")
                        .prompt(Stream.of(tokens).skip(1).collect(Collectors.joining(" ")))
                    .build())
                .purchaseRecordProvider(provider)
                .monitor(monitor)
                .build();
                break;
            default :
                command = CommandList.builder()
                    .monitor(monitor)
                    .build();
        }
        return command;
    }

    private GetTotalSinceTime getTotalTimeSince(TimeSince time) {
        return GetTotalSinceTime.builder()
            .monitor(monitor)
            .timeSince(time)
            .context(context)
            .provider(provider)
            .build();
    }
}
