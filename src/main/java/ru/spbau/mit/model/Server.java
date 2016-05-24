package ru.spbau.mit.model;

public interface Server {
    void start();
    void stop();
    void join() throws InterruptedException;
}
