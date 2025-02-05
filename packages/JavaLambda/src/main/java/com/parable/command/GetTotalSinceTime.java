package com.parable.command;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import com.amazonaws.services.lambda.runtime.Context;
import com.parable.model.PurchaseModel;
import com.parable.observer.CommandMonitor;
import com.parable.observer.TelegramMessageConstants;
import com.parable.provider.DynamoDBPurchaseRecordProvider.TimeSince;

import lombok.Builder;

@Builder
public class GetTotalSinceTime implements Command {
    private static final String TEMPLATE = "%s's Total: $%s";
    private final Function<TimeSince, List<PurchaseModel>> provider;
    private final CommandMonitor monitor;
    private final TimeSince timeSince;
    private final Context context;

    @Override
    public void execute() {
        Double runningTotal = 0.0;
        try {
            List<PurchaseModel> results = provider.apply(timeSince);
            for(PurchaseModel model: results) {
                runningTotal += model.getCost();
            }
            monitor.notifyObservers(String.format(TEMPLATE, timeSince.name(), String.format("%.2f", runningTotal)));
        } catch (Exception e) {
            context.getLogger().log("Exception: " + Arrays.toString(e.getStackTrace()));
            monitor.notifyObservers(TelegramMessageConstants.ERROR);
        }
    }
}
