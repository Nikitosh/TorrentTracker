package ru.spbau.mit.model;

public class PeerToPeerServer extends AbstractServer {
    public PeerToPeerServer(ClientState clientState) {
        super(0);
        setHandlerFactory(new PeerToPeerClientHandlerFactory(clientState));
    }
}
