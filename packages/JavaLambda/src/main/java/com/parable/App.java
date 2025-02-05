package com.parable;

import java.util.Arrays;
import java.util.Collections;

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
import com.parable.model.TelegramUpdate;
import com.parable.module.ObserverModule;
import com.parable.observer.CommandMonitor;
import com.parable.observer.TelegramMessageConstants;

import lombok.AllArgsConstructor;

@AllArgsConstructor(onConstructor = @__(@Inject))
public final class App implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    @Inject
    ObjectMapper mapper;
    @Inject
    CommandInvoker invoker;
    @Inject
    CommandMonitor monitor;
    @Inject
    CommandFactory commandFactory;

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
        commandFactory.setContext(context);

        try {
            TelegramUpdate update = mapper.readValue(input.getBody(), new TypeReference<TelegramUpdate>(){});
            if (updateFieldsExist(update)) {
                attemptCommand(update, context);
            } else {
                // Shouldn't ever happen
                monitor.notifyObservers(TelegramMessageConstants.ERROR);
                context.getLogger().log("Some fields that were expcected were not provided.");
                return response;
            }
        } catch (Exception e) {
            monitor.notifyObservers(TelegramMessageConstants.ERROR);
            context.getLogger().log("exception " + Arrays.toString(e.getStackTrace()));
            // Need to look into updating response code when we want to have telegram retry from their end.
        }
        return response;
    }

    private boolean updateFieldsExist(TelegramUpdate update) {
        return update.getMessage() != null && update.getMessage().getFrom() != null && 
        update.getMessage().getFrom().getIs_bot() == false;
    }

    private void attemptCommand(TelegramUpdate update, Context context) {
        String[] tokens = update.getMessage().getText().split("\\s+");
        String userId = update.getMessage().getFrom().getId().toString();

        if (!userId.equals(String.valueOf(ObserverModule.TOM_CHAT_ID))) { // will refactor to support many chat ids
            context.getLogger().log("Unrecognized userID.");
            monitor.notifyObservers(TelegramMessageConstants.HELP);
            return;
        }

        Command command = commandFactory.createCommand(tokens, userId, update.getMessage().getDate());
        invoker.executeCommand(command);
    }
}
