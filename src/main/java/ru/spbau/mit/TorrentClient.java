package ru.spbau.mit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class TorrentClient implements Client {
    private static final Logger LOGGER = LogManager.getLogger(TorrentClient.class);
    private static final String DOWNLOADS_PATH = "downloads";

    private static final String LIST = "list";
    private static final String GET = "get";
    private static final String NEW_FILE = "newfile";
    private static final String RUN = "run";

    private static final int LIST_ARGUMENTS_NUMBER = 2;
    private static final int GET_ARGUMENTS_NUMBER = 3;
    private static final int NEW_FILE_ARGUMENTS_NUMBER = 3;
    private static final int RUN_ARGUMENTS_NUMBER = 2;

    private static final String WRONG_ARGUMENTS_NUMBER = "Wrong number of arguments of command ";
    private static final String WRONG_TRACKER_ADDRESS = "Wrong tracker address format: ";
    private static final String NAME = "Name: ";
    private static final String SIZE = "Size: ";
    private static final String ID = "Id: ";

    private final TrackerClient trackerClient = new TrackerClientImpl();
    private final ClientState clientState;
    private final PeerToPeerServer peerToPeerServer;
    private final PeerToPeerClient peerToPeerClient;
    private Timer updateTimer;
    private TimerTask updateTask;

    public TorrentClient() {
        clientState = new ClientState();
        peerToPeerServer = new PeerToPeerServer(clientState);
        peerToPeerClient = new PeerToPeerClientImpl();
    }

    @Override
    public void start(byte[] ip) throws IOException {
        trackerClient.connect(ip, Constants.SERVER_PORT);
        peerToPeerServer.start();
        updateTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    trackerClient.executeUpdate(peerToPeerServer.getServerSocketPort(),
                            clientState.getAvailableFileIds());
                } catch (IOException e) {
                    LOGGER.warn("Failed during updating: " + e.getMessage());
                }
            }
        };
        updateTimer = new Timer();
        updateTimer.schedule(updateTask, 0, Constants.UPDATE_REQUEST_DELAY);
    }

    @Override
    public void stopClient() throws IOException {
        trackerClient.disconnect();
        peerToPeerClient.disconnect();
        updateTask.cancel();
        updateTimer.cancel();
    }

    @Override
    public void stopServer() throws IOException {
        peerToPeerServer.stop();
    }

    @Override
    public List<FileInfo> getFilesList() throws IOException {
        return trackerClient.executeList();
    }

    @Override
    public void addFileToDownload(byte[] ip, int fileId) {
        clientState.addFileToDownload(ip, fileId);
    }

    @Override
    public void upload(String path) throws IOException {
        Path p = Paths.get(path);
        File file = p.toFile();
        if (!file.exists() || file.isDirectory()) {
            throw new NoSuchFileException(path);
        }
        int id = trackerClient.executeUpload(p.getFileName().toString(), file.length());
        clientState.addFile(id, p);
        trackerClient.executeUpdate(peerToPeerServer.getServerSocketPort(), clientState.getAvailableFileIds());
    }

    @Override
    public void run(byte[] ip) throws IOException {
        List<Integer> filesToDownloadList = clientState.getToDownloadFilesWithIp(ip);
        if (filesToDownloadList == null) {
            return;
        }
        for (int id : filesToDownloadList) {
            download(id, Paths.get(DOWNLOADS_PATH));
        }
    }

    @Override
    public void download(int fileId, Path path) throws IOException { //public for future using as separate function
        List<ClientInfo> clientsList = trackerClient.executeSources(fileId);
        List<FileInfo> filesList = trackerClient.executeList();
        FileInfo newFileInfo = null;
        for (FileInfo fileInfo : filesList) {
            if (fileInfo.getId() == fileId) {
                newFileInfo = fileInfo;
                break;
            }
        }
        assert newFileInfo != null;
        if (!path.toFile().exists()) {
            Files.createDirectory(path);
        }
        Path filePath = path.resolve(newFileInfo.getName());
        File file = filePath.toFile();
        RandomAccessFile newFile = new RandomAccessFile(file, "rw");
        long fileSize = newFileInfo.getSize();
        newFile.setLength(fileSize);

        int partNumber = (int) ((fileSize + Constants.DATA_BLOCK_SIZE - 1) / Constants.DATA_BLOCK_SIZE);
        Set<Integer> availableParts = new HashSet<>();
        while (availableParts.size() != partNumber) {
            for (ClientInfo clientInfo : clientsList) {
                try {
                    peerToPeerClient.connect(clientInfo.getIp(), clientInfo.getPort());
                } catch (IOException e) {
                    continue;
                }
                List<Integer> fileParts = peerToPeerClient.executeStat(fileId);
                for (int part : fileParts) {
                    if (!availableParts.contains(part)) {
                        newFile.seek(part * Constants.DATA_BLOCK_SIZE);
                        int partSize = Constants.DATA_BLOCK_SIZE;
                        if (part == partNumber - 1) {
                            partSize = (int) (fileSize % Constants.DATA_BLOCK_SIZE);
                        }
                        byte[] buffer;
                        try {
                            buffer = peerToPeerClient.executeGet(fileId, part);
                        } catch (IOException e) {
                            continue;
                        }
                        newFile.write(buffer, 0, partSize);
                        availableParts.add(part);
                        clientState.addFilePart(fileId, part, filePath);
                        trackerClient.executeUpdate(peerToPeerServer.getServerSocketPort(),
                                clientState.getAvailableFileIds());
                    }
                }

            }
        }
        newFile.close();
    }

    @Override
    public void save() throws IOException {
        clientState.save();
    }

    @Override
    public void restore() throws IOException {
        clientState.restore();
    }

    public static void main(String[] args) {
        Client client = new TorrentClient();
        if (Constants.TO_SAVE_PATH.toFile().exists()) {
            try {
                client.restore();
            } catch (IOException e) {
                LOGGER.error("Failed during restoring");
                return;
            }
        }
        byte[] trackerAddress;
        try {
            trackerAddress = InetAddress.getByName(args[1]).getAddress();
        } catch (UnknownHostException e) {
            LOGGER.error(WRONG_TRACKER_ADDRESS + args[1]);
            return;
        }
        switch (args[0]) {
            case LIST:
                if (args.length != LIST_ARGUMENTS_NUMBER) {
                    LOGGER.error(WRONG_ARGUMENTS_NUMBER + LIST);
                    return;
                }
                handleList(client, trackerAddress);
                break;
            case GET:
                if (args.length != GET_ARGUMENTS_NUMBER) {
                    LOGGER.error(WRONG_ARGUMENTS_NUMBER + GET);
                    return;
                }
                try {
                    handleGet(client, trackerAddress, Integer.parseInt(args[2]));
                } catch (NumberFormatException e) {
                    LOGGER.error("Wrong file id format: " + args[2]);
                }
                break;
            case NEW_FILE:
                if (args.length != NEW_FILE_ARGUMENTS_NUMBER) {
                    LOGGER.error(WRONG_ARGUMENTS_NUMBER + NEW_FILE);
                    return;
                }
                handleNewFile(client, trackerAddress, args[2]);
                break;
            case RUN:
                if (args.length != RUN_ARGUMENTS_NUMBER) {
                    LOGGER.error(WRONG_ARGUMENTS_NUMBER + RUN);
                    return;
                }
                handleRun(client, trackerAddress);
                break;
            default:
                LOGGER.error("Not supported operation: " + args[0]);
                break;
        }
        try {
            client.save();
        } catch (IOException e) {
            LOGGER.warn("Save request exception: " + e.getMessage());
        }
    }

    private static void handleList(Client client, byte[] trackerAddress) {
        try {
            client.start(trackerAddress);
            List<FileInfo> filesList = client.getFilesList();
            for (FileInfo fileInfo : filesList) {
                System.out.println(NAME + fileInfo.getName() + ", "
                        + SIZE + fileInfo.getSize() + ", " + ID + fileInfo.getId());
            }
            client.stopClient();
            client.stopServer();
        } catch (IOException e) {
            LOGGER.warn("List request exception: " + e.getMessage());
        }
    }

    private static void handleGet(Client client, byte[] trackerAddress, int id) {
        try {
            client.addFileToDownload(trackerAddress, id);
        } catch (IOException e) {
            LOGGER.warn("Get request exception: " + e.getMessage());
        }
    }

    private static void handleNewFile(Client client, byte[] trackerAddress, String path) {
        try {
            client.start(trackerAddress);
            client.upload(path);
            client.stopClient();
            client.stopServer();
        } catch (NoSuchFileException e) {
            LOGGER.error("Wrong file path");
        } catch (IOException e) {
            LOGGER.warn("NewFile request exception: " + e.getMessage());
        }
    }

    private static void handleRun(Client client, byte[] trackerAddress) {
        try {
            client.start(trackerAddress);
            client.run(trackerAddress);
        } catch (NoSuchFileException e) {
            LOGGER.error("Wrong file path");
        } catch (IOException e) {
            LOGGER.warn("NewFile request exception: " + e.getMessage());
        }
    }
}
