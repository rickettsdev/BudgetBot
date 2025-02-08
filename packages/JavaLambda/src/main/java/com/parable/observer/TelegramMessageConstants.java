package com.parable.observer;

public class TelegramMessageConstants {

    public static String ERROR = "I'm having trouble with that command. Please try again later.";
    public static String HELP = "Here are the valid commands you can run:\n/add <cost> <message>\n/ask <question>\n"
            + "/getTotalToday\n/getTotalWeek\n/getTotalMonth\n/subscriberAdd <subscriberId> <subscriberBotToken>";
    public static String MESSAGE_SENT = "Cost %.2f added to your budget for purchase %s.";
    public static String SUBSCRIBER_ADDED =
            "Added subscriber. Please ask them to add the token %s to the end of the endpoint they register.";
    public static String UNAUTHORIZED_ACCESS_ATTEMPT = "Someone managed to invoke the api gateway endpoint who shouldn't have.";
}
