package ru.spbau.mit.model;

import java.io.IOException;
import java.util.List;

public interface TrackerClient {
    void connect(byte[] ip, int port) throws IOException;
    void disconnect() throws IOException;
    List<FileInfo> executeList() throws IOException;
    int executeUpload(String name, long size) throws IOException;
    List<ClientInfo> executeSources(int id) throws IOException;
    boolean executeUpdate(int port, List<Integer> seededFiles) throws IOException;
}
