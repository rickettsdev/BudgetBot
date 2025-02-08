package com.parable.provider;

import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import com.parable.model.SubscriberModel;

import lombok.Builder;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;


/**
 * BiFunction<subscriberId, token, List of SubscriberModel>
 */
@Builder
public class SubscriberRecordProvider implements BiFunction<String, String, List<SubscriberModel>>{
    private final DynamoDbEnhancedClient client;
    private final String tableName;
    @Override
    public List<SubscriberModel> apply(String subscriberId, String token) {
         DynamoDbTable<SubscriberModel> mappedTable = client.table(tableName, TableSchema.fromClass(SubscriberModel.class));

        Key.Builder keyBuilder = Key.builder().partitionValue(subscriberId);
        if (!token.isEmpty()) {
            keyBuilder.sortValue(token);
        }
        QueryConditional queryConditional = QueryConditional.keyEqualTo(keyBuilder.build()); 

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

    
}
