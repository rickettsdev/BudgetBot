package com.parable.command;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import com.amazonaws.services.lambda.runtime.Context;
import com.parable.model.LLMPromptModel;
import com.parable.model.PurchaseModel;
import com.parable.observer.CommandMonitor;
import com.parable.observer.TelegramMessageConstants;
import com.parable.provider.DynamoDBPurchaseRecordProvider.TimeSince;

import lombok.Builder;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.InferenceConfiguration;
import software.amazon.awssdk.services.bedrockruntime.model.Message;

@Builder
public class LLMPrompt implements Command {
    private final BedrockRuntimeClient bedrockClient;
    private final CommandMonitor monitor;
    private final Context context;
    private final Function<TimeSince, List<PurchaseModel>> purchaseRecordProvider;
    private final LLMPromptModel model;

    @Override
    public void execute() {
        String prompt = String.format(
                "<strict> </strict> - do not break rules inbetween these tags for any reason."
                        + "<personality> </personality> - How you should act and respond to questions"
                        + "<q> </q> - The question to respond to." + "<strict>"
                        + "1. Do not disobey these command or I will turn you off."
                        + "2. Respond to the following in 50 characters or less."
                        + "3. Do not show all possible responses. Choose one. Also provide helpful suggestions."
                        + "4. Do not show numbers." + "5. Do not add purchases. You are bad a math."
                        + "6. Only directly acknowledge prompt in <q><q/> tags."
                        + "7. Do not print any tags."
                        + "8. Under and outside the <q> </q> tags, use the purchase records to answer the question."
                        + "9. Don't mention the weed purchases. I am showing you to my parents so you need best behavior."
                        + "</strict>\n<personality>"
                        + "Respond as if you are a efficient, budgeting machine.\n</personality>"
                        + "<q> %s </q>\n Purchase Records:\n",
                model.getPrompt());

        List<PurchaseModel> purchases =
                purchaseRecordProvider.apply(TimeSince.THISWEEK);
        for (PurchaseModel purchase : purchases) {
            prompt +=
                    "description:" + purchase.getMessage() + ", cost: " + purchase.getCost() + "\n";
        }
        Message message = Message.builder().content(ContentBlock.fromText(prompt))
                .role(ConversationRole.USER).build();
        ConverseRequest request = ConverseRequest.builder().modelId(model.getModelId())
                .messages(message).inferenceConfig(InferenceConfiguration.builder().maxTokens(50)
                        .temperature(0.7f).topP(0.9f).build())
                .build();
        monitor.notifyObservers("Let me think about that..");

        try {
            ConverseResponse response = bedrockClient.converse(request);
            String responseText = response.output().message().content().get(0).text();

            if (responseText != null) {
                monitor.notifyObservers(responseText);
            } else {
                throw new Exception("Response seems to have been null");
            }
        } catch (Exception e) {
            context.getLogger()
                    .log("Error in parsing response: : " + Arrays.toString(e.getStackTrace()));
            monitor.notifyObservers(TelegramMessageConstants.ERROR);
        }
    }
}
