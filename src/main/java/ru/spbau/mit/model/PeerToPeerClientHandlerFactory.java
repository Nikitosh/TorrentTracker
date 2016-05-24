package ru.spbau.mit.model;

import java.net.Socket;
import java.util.function.Function;

public class PeerToPeerClientHandlerFactory implements Function<Socket, Runnable> {
    private final ClientState clientState;

    public PeerToPeerClientHandlerFactory(ClientState clientState) {
        this.clientState = clientState;
    }

    @Override
    public Runnable apply(Socket socket) {
        return new PeerToPeerClientHandler(socket, clientState);
    }
}
