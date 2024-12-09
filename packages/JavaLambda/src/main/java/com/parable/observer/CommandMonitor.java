package com.parable.observer;

import java.util.List;

import com.parable.observer.Observer.MessageTemplate;

import lombok.Builder;

@Builder
public class CommandMonitor implements EventSubject {
    // Need to prevent this from messaging other users if/when we support more than me.
    // Could use map of list of observers, where key is userid
    private final List<Observer> observers;
    @Override
    public void registerObserver(Observer observer) {
        observers.add(observer);
    }

    @Override
    public void notifyObservers(MessageTemplate messageTemplate) {
        observers.forEach(observer -> observer.update(messageTemplate));
    }

    @Override
    public void notifyObservers(String message) {
        observers.forEach(observer -> observer.update(message));
    }

}
