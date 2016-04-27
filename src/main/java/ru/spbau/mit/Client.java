package ru.spbau.mit;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface Client {
    void start(byte[] ip) throws IOException;
    void stopClient() throws IOException;
    void stopServer() throws IOException;
    List<FileInfo> getFilesList() throws IOException;
    void addFileToDownload(byte[] ip, int fileId) throws IOException;
    void upload(String path) throws IOException;
    void run(byte[] ip) throws IOException;
    void download(int fileId, Path path) throws IOException;
    void save() throws IOException;
    void restore() throws IOException;
}
