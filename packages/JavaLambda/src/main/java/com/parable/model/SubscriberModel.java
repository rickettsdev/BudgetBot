package com.parable.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@Jacksonized
@DynamoDbImmutable(builder = SubscriberModel.SubscriberModelBuilder.class)
@Value
@Builder
public class SubscriberModel {
    @Getter(onMethod = @__({@DynamoDbPartitionKey}))
    String subscriber;
    @Getter(onMethod = @__({@DynamoDbSortKey}))
    String token;
    @Getter(onMethod = @__({@DynamoDbAttribute("ownerId")}))
    String ownerId;
    @Getter(onMethod = @__({@DynamoDbAttribute("botToken")}))
    String botToken;
}
