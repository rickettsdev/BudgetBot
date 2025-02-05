package com.parable.provider;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.parable.model.PurchaseModel;

import lombok.Builder;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

@Builder
public class DynamoDBPurchaseRecordProvider implements Function<DynamoDBPurchaseRecordProvider.TimeSince, List<PurchaseModel>> {

    public enum TimeSince {
        TODAY, THISWEEK, THISMONTH,
    }

    private static final ZoneId EST_ZONE = ZoneId.of("America/New_York"); // TODO: consider using request zone.
    private final DynamoDbEnhancedClient client;
    private final String tableName;
    private final String chatId;

    @Override
    public List<PurchaseModel> apply(TimeSince timeSince) {
        long time = getTimestamp(timeSince);

        DynamoDbTable<PurchaseModel> mappedTable = client.table(tableName, TableSchema.fromClass(PurchaseModel.class));

        QueryConditional queryConditional = QueryConditional
            .sortGreaterThanOrEqualTo(Key.builder().partitionValue(chatId).sortValue(time).build());

        QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
            .queryConditional(queryConditional)
            .build();

        try {
            return mappedTable.query(queryRequest).items().stream().collect(Collectors.toList());
        } catch (Exception e) {
            // consider better logging solution than lambda context.
            throw e;
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
