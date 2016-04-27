package ru.spbau.mit;

public class PeerToPeerServer extends AbstractServer {
    public PeerToPeerServer(ClientState clientState) {
        super(0);
        setHandlerFactory(new PeerToPeerClientHandlerFactory(clientState));
    }
}
