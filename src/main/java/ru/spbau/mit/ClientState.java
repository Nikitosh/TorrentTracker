package ru.spbau.mit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ClientState {
    private static final Logger LOGGER = LogManager.getLogger(ClientState.class);

    private Map<Integer, BitSet> availableFileParts; //stores numbers of available parts of given id
    private Map<Integer, Path> filesPaths; //stores path of file with given id
    private Map<InetAddress, List<Integer>> toDownloadFiles = new HashMap<>(); //stores list of ids for given ip

    public ClientState() {
        availableFileParts = new HashMap<>();
        filesPaths = new HashMap<>();
    }

    public void addFile(int id, Path path) {
        long size = path.toFile().length();
        filesPaths.put(id, path);
        BitSet fileParts = new BitSet();
        for (int i = 0; i < (size + Constants.DATA_BLOCK_SIZE - 1) / Constants.DATA_BLOCK_SIZE; i++) {
            fileParts.set(i);
        }
        availableFileParts.put(id, fileParts);
    }

    public void addFilePart(int id, int part, Path path) {
        if (!filesPaths.containsKey(id)) {
            filesPaths.put(id, path);
            availableFileParts.put(id, new BitSet());
        }
        availableFileParts.get(id).set(part);
    }

    public void save() throws IOException {
        File file = Constants.TO_SAVE_PATH.toFile();
        if (!file.exists()) {
            Files.createFile(Constants.TO_SAVE_PATH);
            file = Constants.TO_SAVE_PATH.toFile();
        }
        DataOutputStream outputStream = new DataOutputStream(new FileOutputStream(file));

        outputStream.writeInt(availableFileParts.size());
        for (Map.Entry<Integer, BitSet> entry : availableFileParts.entrySet()) {
            outputStream.writeInt(entry.getKey());
            BitSet availableParts = entry.getValue();
            outputStream.writeInt(availableParts.cardinality());
            for (int i = 0; i < availableParts.size(); i++) {
                if (availableParts.get(i)) {
                    outputStream.writeInt(i);
                }
            }
        }

        outputStream.writeInt(filesPaths.size());
        for (Map.Entry<Integer, Path> filePath : filesPaths.entrySet()) {
            outputStream.writeInt(filePath.getKey());
            outputStream.writeUTF(filePath.getValue().toString());
        }
        outputStream.close();
    }

    public void restore() throws IOException {
        File file = Constants.TO_SAVE_PATH.toFile();
        DataInputStream inputStream = new DataInputStream(new FileInputStream(file));

        availableFileParts = new HashMap<>();
        filesPaths = new HashMap<>();

        int availableFilePartsSize = inputStream.readInt();
        for (int i = 0; i < availableFilePartsSize; i++) {
            int id = inputStream.readInt();
            int setSize = inputStream.readInt();
            BitSet availableParts = new BitSet();
            for (int j = 0; j < setSize; j++) {
                availableParts.set(inputStream.readInt());
            }
            availableFileParts.put(id, availableParts);
        }

        int filesPathsSize = inputStream.readInt();
        for (int i = 0; i < filesPathsSize; i++) {
            int id = inputStream.readInt();
            String path = inputStream.readUTF();
            filesPaths.put(id, Paths.get(path));
        }
    }

    public List<Integer> getAvailableFileIds() {
        return new ArrayList<>(availableFileParts.keySet());
    }

    public boolean containsFileWithId(int id) {
        return availableFileParts.containsKey(id);
    }

    public BitSet getAvailableFilePartsWithId(int id) {
        return availableFileParts.get(id);
    }

    public Path getPathWithId(int id) {
        return filesPaths.get(id);
    }

    public void addFileToDownload(byte[] ip, int id) {
        InetAddress address;
        try {
            address = InetAddress.getByAddress(ip);
        } catch (UnknownHostException e) {
            LOGGER.warn(e.getMessage());
            return;
        }
        if (!toDownloadFiles.containsKey(address)) {
            toDownloadFiles.put(address, new ArrayList<>());
        }
        toDownloadFiles.get(address).add(id);
    }

    public List<Integer> getToDownloadFilesWithIp(byte[] ip) {
        try {
            return toDownloadFiles.get(InetAddress.getByAddress(ip));
        } catch (UnknownHostException e) {
            LOGGER.warn(e.getMessage());
            return null;
        }
    }
}