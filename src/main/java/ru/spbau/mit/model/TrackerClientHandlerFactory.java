package ru.spbau.mit.model;

import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;
import java.util.function.Function;

public class TrackerClientHandlerFactory implements Function<Socket, Runnable> {
    private final List<FileInfo> filesList;
    private final Map<ClientInfo, Set<Integer>> clientSeededFiles;
    private final Map<ClientInfo, TimerTask> toRemoveClientTasks;

    TrackerClientHandlerFactory(List<FileInfo> filesList, Map<ClientInfo, Set<Integer>> clientSeededFiles,
                                Map<ClientInfo, TimerTask> toRemoveClientTasks) {
        this.filesList = filesList;
        this.clientSeededFiles = clientSeededFiles;
        this.toRemoveClientTasks = toRemoveClientTasks;
    }

    @Override
    public Runnable apply(Socket socket) {
        return new TrackerClientHandler(socket, filesList, clientSeededFiles, toRemoveClientTasks);
    }
}
