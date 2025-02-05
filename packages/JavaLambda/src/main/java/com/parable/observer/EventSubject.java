package com.parable.observer;

public interface EventSubject {
    public void registerObserver(Observer observer);
    public void notifyObservers(String message);
}
