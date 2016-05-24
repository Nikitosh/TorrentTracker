package ru.spbau.mit.model;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

public class TorrentServer extends AbstractServer {
    private List<FileInfo> filesList;
    //stores ids of files, seeded by given client
    private Map<ClientInfo, Set<Integer>> clientSeededFiles;
    //stores TimerTask, which removes given client from clientSeededFiles
    private Map<ClientInfo, TimerTask> toRemoveClientTasks;

    public TorrentServer() {
        super(Constants.SERVER_PORT);
        filesList = Collections.synchronizedList(new ArrayList<>());
        clientSeededFiles = Collections.synchronizedMap(new HashMap<>());
        toRemoveClientTasks = Collections.synchronizedMap(new HashMap<>());
        setHandlerFactory(new TrackerClientHandlerFactory(filesList, clientSeededFiles, toRemoveClientTasks));
    }

    public static void main(String[] args) {
        Server server = new TorrentServer();
        server.start();
    }

    public void save() throws IOException {
        File file = Constants.TO_SAVE_PATH.toFile();
        if (!file.exists()) {
            Files.createFile(Constants.TO_SAVE_PATH);
            file = Constants.TO_SAVE_PATH.toFile();
        }
        DataOutputStream outputStream = new DataOutputStream(new FileOutputStream(file));

        outputStream.writeInt(filesList.size());
        for (FileInfo fileInfo : filesList) {
            fileInfo.write(outputStream);
        }

        outputStream.writeInt(toRemoveClientTasks.size());
        for (Map.Entry<ClientInfo, Set<Integer>> entry : clientSeededFiles.entrySet()) {
            entry.getKey().write(outputStream);
            Set<Integer> ids = entry.getValue();
            outputStream.writeInt(ids.size());
            for (int id : ids) {
                outputStream.writeInt(id);
            }
        }

        outputStream.flush();
        outputStream.close();
    }

    public void restore() throws IOException {
        File file = Constants.TO_SAVE_PATH.toFile();
        DataInputStream inputStream = new DataInputStream(new FileInputStream(file));

        filesList = Collections.synchronizedList(new ArrayList<>());
        clientSeededFiles = Collections.synchronizedMap(new HashMap<>());
        toRemoveClientTasks = Collections.synchronizedMap(new HashMap<>());

        int filesListSize = inputStream.readInt();
        for (int i = 0; i < filesListSize; i++) {
            filesList.add(FileInfo.read(inputStream));
        }

        int clientSeededFilesSize = inputStream.readInt();
        for (int i = 0; i < clientSeededFilesSize; i++) {
            ClientInfo clientInfo = ClientInfo.read(inputStream);
            int idsSize = inputStream.readInt();
            Set<Integer> ids = new HashSet<>();
            for (int j = 0; j < idsSize; j++) {
                ids.add(inputStream.readInt());
            }
            clientSeededFiles.put(clientInfo, ids);
        }

        inputStream.close();
        setHandlerFactory(new TrackerClientHandlerFactory(filesList, clientSeededFiles, toRemoveClientTasks));
    }
}
