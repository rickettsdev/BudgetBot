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
@DynamoDbImmutable(builder = PurchaseModel.PurchaseModelBuilder.class)
@Value
@Builder
public class PurchaseModel {
    @Getter(onMethod = @__({@DynamoDbPartitionKey}))
    String id;
    @Getter(onMethod = @__({@DynamoDbSortKey}))
    Long timestamp;
    @Getter(onMethod = @__({@DynamoDbAttribute("cost")}))
    Double cost;
    @Getter(onMethod = @__({@DynamoDbAttribute("message")}))
    String message;
}
