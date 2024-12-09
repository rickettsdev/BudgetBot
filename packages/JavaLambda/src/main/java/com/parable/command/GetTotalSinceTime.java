package com.parable.command;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.amazonaws.services.lambda.runtime.Context;
import com.parable.model.PurchaseModel;
import com.parable.observer.CommandMonitor;
import com.parable.observer.Observer.MessageTemplate;

import lombok.Builder;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

@Builder
public class GetTotalSinceTime implements Command {

    enum TimeSince {
        TODAY, THISWEEK, THISMONTH,
    }

    private static final ZoneId EST_ZONE = ZoneId.of("America/New_York");
    private static final String TEMPLATE = "%s's Total: $%s";
    private final DynamoDbEnhancedClient client;
    private final CommandMonitor monitor;
    private final String tableName;
    private final TimeSince timeSince;
    private final String chatId;
    private final Context context;

    @Override
    public void execute() {
        long time = getTimestamp(timeSince);

        DynamoDbTable<PurchaseModel> mappedTable = client.table(tableName, TableSchema.fromClass(PurchaseModel.class));

        QueryConditional queryConditional = QueryConditional
            .sortGreaterThanOrEqualTo(Key.builder().partitionValue(chatId).sortValue(time).build());

        QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
            .queryConditional(queryConditional)
            .build();

        Double runningTotal = 0.0;
        try {
            List<PurchaseModel> results = mappedTable.query(queryRequest).items().stream().collect(Collectors.toList());
            for(PurchaseModel model: results) {
                runningTotal += model.getCost();
            }
            monitor.notifyObservers(String.format(TEMPLATE, timeSince.name(), String.format("%.2f", runningTotal)));
        } catch (Exception e) {
            context.getLogger().log("Exception: " + Arrays.toString(e.getStackTrace()));
            monitor.notifyObservers(MessageTemplate.ERROR);
        }
    }

    private long getTimestamp(TimeSince timeSince) {
        switch (timeSince) {
            case TODAY:
                return LocalDate.now()
                .atStartOfDay(EST_ZONE)
                .toEpochSecond();
            case THISWEEK:
                return LocalDate.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
                .atStartOfDay(EST_ZONE)
                .toEpochSecond();
            case THISMONTH:
                return LocalDate.now()
                .withDayOfMonth(1)
                .atStartOfDay(EST_ZONE)
                .toEpochSecond();
            default:
                return -1l; // need to handle this case better. Shouldn't happen
        }
    }
    
}
