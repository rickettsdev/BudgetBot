package com.parable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parable.command.Command;
import com.parable.command.CommandFactory;
import com.parable.command.CommandInvoker;
import com.parable.component.DaggerAppComponent;
import com.parable.model.SubscriberModel;
import com.parable.model.TelegramUpdate;
import com.parable.observer.CommandMonitor;
import com.parable.observer.Observer;
import com.parable.observer.TelegramMessageConstants;
import com.parable.observer.TelegramObserver;
import com.parable.provider.SubscriberRecordProvider;
import com.pengrad.telegrambot.TelegramBot;

import lombok.AllArgsConstructor;

@AllArgsConstructor(onConstructor = @__(@Inject))
public final class App implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    @Inject
    ObjectMapper mapper;
    @Inject
    CommandInvoker invoker;
    @Inject
    CommandMonitor adminMonitor;
    @Inject
    CommandFactory commandFactory;
    @Inject
    SubscriberRecordProvider subscriberRecordProvider;

    public App(){
        // Leaking 'this' in constructor is warning when this may not have been fully intialized,
        // which makes it dangerous. However, we are using this to initialize dependencies. Still scares me.
        DaggerAppComponent.create().inject(this);
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        context.getLogger().log("Input: " + input);
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setHeaders(Collections.singletonMap("Content-Type", "application/json"));
        response.setStatusCode(200);
        String token = input.getQueryStringParameters().getOrDefault("token", ""); // for distinguishing budget to update
        commandFactory.setContext(context);

        try {
            TelegramUpdate update = mapper.readValue(input.getBody(), new TypeReference<TelegramUpdate>(){});
            if (updateFieldsExist(update)) {
                attemptCommand(update, context, token);
            } else {
                // Shouldn't ever happen
                adminMonitor.notifyObservers(TelegramMessageConstants.ERROR);
                context.getLogger().log("Some fields that were expcected were not provided.");
                return response;
            }
        } catch (Exception e) {
            adminMonitor.notifyObservers(TelegramMessageConstants.ERROR);
            context.getLogger().log("exception " + Arrays.toString(e.getStackTrace()));
            // Need to look into updating response code when we want to have telegram retry from their end.
        }
        return response;
    }

    private boolean updateFieldsExist(TelegramUpdate update) {
        return update.getMessage() != null && update.getMessage().getFrom() != null && 
        update.getMessage().getFrom().getIs_bot() == false;
    }

    private void attemptCommand(TelegramUpdate update, Context context, String tokenSecret) {
        String[] tokens = update.getMessage().getText().split("\\s+");
        String userId = update.getMessage().getFrom().getId().toString();

        List<SubscriberModel> models = subscriberRecordProvider.apply(userId, tokenSecret);
        
        if (models.isEmpty()) {
            context.getLogger().log("UNAUTHORIZED: UserId + token secret does not match.");
            adminMonitor.notifyObservers(TelegramMessageConstants.UNAUTHORIZED_ACCESS_ATTEMPT);
            return;
        }

        SubscriberModel model = models.get(0);
        String primaryId = model.getOwnerId();
        String secondaryId = model.getSubscriber();

        CommandMonitor localMonitor = CommandMonitor.builder().observers(generateLocalObservers(model)).build();
        Command command = commandFactory.createCommand(tokens, primaryId, secondaryId, update.getMessage().getDate(), localMonitor);
        invoker.executeCommand(command);
    }

    // This would have been much cleaner with SQL :,)
    private List<Observer> generateLocalObservers(SubscriberModel model) {
        // Should only ever be a single entry for subscriberId/token query.
        String subscriberId = model.getSubscriber();
        String ownerId = model.getOwnerId();
        List<Observer> localObservers = List.of(TelegramObserver.builder()
                                                    .bot(new TelegramBot(model.getBotToken()))
                                                    .chatId(Long.valueOf(subscriberId))
                                                .build());
        if (!subscriberId.equals(ownerId)) {
            List<SubscriberModel> ownerModels = subscriberRecordProvider.apply(ownerId, "");
            for (SubscriberModel current: ownerModels) {
                if (current.getSubscriber().equals(current.getOwnerId())) { 
                    localObservers.add(TelegramObserver.builder()
                    .bot(new TelegramBot(current.getBotToken()))
                    .chatId(Long.valueOf(current.getOwnerId()))
                    .build());
                    break;
                }
            }
        }
        return localObservers;
    }
}
