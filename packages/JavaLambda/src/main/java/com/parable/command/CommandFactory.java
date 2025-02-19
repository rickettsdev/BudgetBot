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
    private final String purchaseTableName;
    private final String subscriberTableName;
    @Setter
    private Context context;

    public Command createCommand(String[] tokens, String ownerId, String subscriberId, Long timestamp, CommandMonitor localMonitor) {
        String commandString = tokens.length > 0 ? tokens[0] : "";
        Command command = null;
        switch (commandString) {
            case "/add" :
                command = tokens.length > 1 ? AddCost.builder()
                    .client(client)
                    .model(PurchaseModel.builder()
                            .id(ownerId)
                            .subscriber(ownerId.equals(subscriberId) ? null : subscriberId)
                            .timestamp(timestamp)
                            .message(Stream.of(tokens).skip(2).collect(Collectors.joining(" ")))
                            .cost(Double.valueOf(tokens[1]))
                            .build())
                    .monitor(localMonitor)
                    .tableName(purchaseTableName)
                    .build() : null;
                break;
            case "/getTotalToday" :
                command = getTotalTimeSince(TimeSince.TODAY, localMonitor);
                break;
            case "/getTotalWeek" :
                command = getTotalTimeSince(TimeSince.THISWEEK, localMonitor);
                break;
            case "/getTotalMonth" :
                command = getTotalTimeSince(TimeSince.THISMONTH, localMonitor);
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
                .monitor(localMonitor)
                .build();
                break;
            case "/subscriberAdd": // TODO: need to create admin commands section
                command = SubscriberAdd.builder()
                    .client(client)
                    .monitor(localMonitor)
                    .ownerId(ownerId)
                    .subscriberBotToken(tokens[2])
                    .subscriberId(tokens[1])
                    .tableName(subscriberTableName)
                    .context(context)
                .build();
                break;
            default :
                break;
        }
        return command == null ? CommandList.builder().monitor(localMonitor).build() : command;
    }

    private GetTotalSinceTime getTotalTimeSince(TimeSince time, CommandMonitor localMonitor) {
        return GetTotalSinceTime.builder()
            .monitor(localMonitor)
            .timeSince(time)
            .context(context)
            .provider(provider)
            .build();
    }
}
