package ru.spbau.mit.model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.*;

public class TrackerClientHandler implements Runnable {
    private static final Logger LOGGER = LogManager.getLogger(TrackerClientHandler.class);

    private final Socket socket;
    private final List<FileInfo> filesList;
    private final Map<ClientInfo, Set<Integer>> clientSeededFiles;
    private final Map<ClientInfo, TimerTask> toRemoveClientTasks;
    private final Timer toRemoveClientTimer;

    public TrackerClientHandler(Socket socket, List<FileInfo> filesList, Map<ClientInfo,
            Set<Integer>> clientSeededFiles, Map<ClientInfo, TimerTask> toRemoveClientTasks) {
        this.socket = socket;
        this.filesList = filesList;
        this.clientSeededFiles = clientSeededFiles;
        this.toRemoveClientTasks = toRemoveClientTasks;
        toRemoveClientTimer = new Timer();
    }

    @Override
    public void run() {
        while (!socket.isClosed()) {
            DataOutputStream outputStream;
            DataInputStream inputStream;
            try {
                outputStream = new DataOutputStream(socket.getOutputStream());
                inputStream = new DataInputStream(socket.getInputStream());
            } catch (IOException e) {
                LOGGER.error("Failed to get streams from socket: " + e.getMessage());
                return;
            }
            int requestType;
            try {
                try {
                    requestType = inputStream.readInt();
                } catch (EOFException ignored) {
                    return;
                }
                switch (requestType) {
                    case Constants.LIST_REQUEST:
                        handleList(inputStream, outputStream);
                        break;
                    case Constants.UPLOAD_REQUEST:
                        handleUpload(inputStream, outputStream);
                        break;
                    case Constants.SOURCES_REQUEST:
                        handleSources(inputStream, outputStream);
                        break;
                    case Constants.UPDATE_REQUEST:
                        handleUpdate(inputStream, outputStream);
                        break;
                    default:
                        throw new UnsupportedOperationException();
                }
            } catch (IOException e) {
                LOGGER.warn(e.getMessage());
            }
        }
    }

    private void handleList(DataInputStream inputStream, DataOutputStream outputStream) throws IOException {
        outputStream.writeInt(filesList.size());
        synchronized (filesList) {
            for (FileInfo fileInfo : filesList) {
                fileInfo.write(outputStream);
            }
        }
        outputStream.flush();
    }

    private void handleUpload(DataInputStream inputStream, DataOutputStream outputStream) throws IOException {
        FileInfo fileInfo = FileInfo.readWithoutId(inputStream);
        int id = IdProvider.getInstance().getNextId();
        fileInfo.setId(id);
        filesList.add(fileInfo);
        outputStream.writeInt(id);
        outputStream.flush();
    }

    private void handleSources(DataInputStream inputStream, DataOutputStream outputStream) throws IOException {
        int id = inputStream.readInt();
        List<ClientInfo> seedingClientsList = new ArrayList<>();
        synchronized (clientSeededFiles) {
            for (Map.Entry<ClientInfo, Set<Integer>> entry : clientSeededFiles.entrySet()) {
                Set<Integer> seededFiles = entry.getValue();
                if (seededFiles.contains(id)) {
                    seedingClientsList.add(entry.getKey());
                }
            }
        }
        outputStream.writeInt(seedingClientsList.size());
        for (ClientInfo clientInfo : seedingClientsList) {
            clientInfo.write(outputStream);
        }
        outputStream.flush();
    }

    private void handleUpdate(DataInputStream inputStream, DataOutputStream outputStream) throws IOException {
        try {
            byte[] ip = socket.getInetAddress().getAddress();
            int port = inputStream.readInt();
            int count = inputStream.readInt();
            Set<Integer> seededFiles = new HashSet<>();
            for (int i = 0; i < count; i++) {
                seededFiles.add(inputStream.readInt());
            }
            ClientInfo clientInfo = new ClientInfo(ip, port);

            synchronized (toRemoveClientTasks) {
                if (toRemoveClientTasks.containsKey(clientInfo)) {
                    toRemoveClientTasks.get(clientInfo).cancel();
                }
            }
            synchronized (clientSeededFiles) {
                clientSeededFiles.put(clientInfo, seededFiles);
            }
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    synchronized (clientSeededFiles) {
                        clientSeededFiles.remove(clientInfo);
                    }
                }
            };
            synchronized (toRemoveClientTasks) {
                toRemoveClientTasks.put(clientInfo, task);
                toRemoveClientTimer.schedule(task, Constants.UPDATE_REQUEST_PERIOD);
            }
        } catch (IOException e) {
            LOGGER.warn("Exception during handling update request: " + e.getMessage());
            outputStream.writeBoolean(false);
            outputStream.flush();
            return;
        }
        outputStream.writeBoolean(true);
        outputStream.flush();
    }
}
