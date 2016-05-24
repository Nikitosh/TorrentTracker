package ru.spbau.mit.model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.spbau.mit.ui.TorrentClientFrame;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TorrentClient implements Client {
    private static final Logger LOGGER = LogManager.getLogger(TorrentClient.class);

    private final TrackerClient trackerClient = new TrackerClientImpl();
    private final ClientState clientState;
    private final PeerToPeerServer peerToPeerServer;
    private final PeerToPeerClient peerToPeerClient;
    private ExecutorService downloadExecutor;
    private Timer updateTimer;
    private TimerTask updateTask;

    public TorrentClient() {
        clientState = new ClientState();
        peerToPeerServer = new PeerToPeerServer(clientState);
        peerToPeerClient = new PeerToPeerClientImpl();
        downloadExecutor = Executors.newCachedThreadPool();
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
        updateTimer.schedule(updateTask, 0, Constants.UPDATE_REQUEST_PERIOD);
    }

    @Override
    public void stop() throws IOException {
        trackerClient.disconnect();
        peerToPeerClient.disconnect();
        peerToPeerServer.stop();
        updateTask.cancel();
        updateTimer.cancel();
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
    public int getProgress(int id) {
        return clientState.getProgress(id);
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
            download(id, Paths.get(Constants.DOWNLOADS_PATH));
        }
    }

    @Override
    public void download(int fileId, Path path) throws IOException {
        downloadExecutor.execute(() -> {
            try {
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
                                clientState.addFilePart(fileId, part, fileSize, filePath);
                                trackerClient.executeUpdate(peerToPeerServer.getServerSocketPort(),
                                        clientState.getAvailableFileIds());
                            }
                        }

                    }
                }
                newFile.close();
            } catch (IOException e) {
                LOGGER.warn("Exception during download: " + e.getMessage());
            }
        });
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
        TorrentClientFrame frame;
        try {
            frame = new TorrentClientFrame(client);
        } catch (IOException e) {
            System.out.print(e);
            e.printStackTrace();
            LOGGER.warn("Exception during frame initialization: " + e.getMessage());
            return;
        }
        frame.setVisible(true);
    }
}
