package ru.spbau.mit;

import java.net.Socket;
import java.nio.file.Path;
import java.util.BitSet;
import java.util.Map;
import java.util.function.Function;

public class PeerToPeerClientHandlerFactory implements Function<Socket, Runnable> {
    private final Map<Integer, BitSet> availableFileParts;
    private final Map<Integer, Path> filesPath;

    public PeerToPeerClientHandlerFactory(Map<Integer, BitSet> availableFileParts,
                                          Map<Integer, Path> filesPath) {
        this.availableFileParts = availableFileParts;
        this.filesPath = filesPath;
    }

    @Override
    public Runnable apply(Socket socket) {
        return new PeerToPeerClientHandler(socket, availableFileParts, filesPath);
    }
}
