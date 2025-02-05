package com.parable.module;

import com.parable.provider.DynamoDBPurchaseRecordProvider;

import dagger.Module;
import dagger.Provides;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Module
public class AWSModule {
    
    @Provides
    public DynamoDbEnhancedClient getEnhancedClient() {
        DynamoDbClient dynamoDbClient = DynamoDbClient.builder()
            .region(Region.US_EAST_2)
            .build();
        return DynamoDbEnhancedClient.builder()
        .dynamoDbClient(dynamoDbClient)
        .build();
    }

    @Provides
    public BedrockRuntimeClient getBedrockRuntimeClient() {
        return BedrockRuntimeClient.builder()
                .region(Region.US_EAST_2)
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build();
    }

    @Provides
    public DynamoDBPurchaseRecordProvider getDBPurchaseRecordProvider(DynamoDbEnhancedClient client) {
        return DynamoDBPurchaseRecordProvider.builder()
                .chatId(Long.toString(ObserverModule.TOM_CHAT_ID))
                .client(client)
                .tableName(CommandModule.TABLE_NAME)
                .build();
    } 
}
