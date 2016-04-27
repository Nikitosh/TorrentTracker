package ru.spbau.mit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.util.BitSet;

public class PeerToPeerClientHandler implements Runnable {
    private static final Logger LOGGER = LogManager.getLogger(PeerToPeerClientHandler.class);

    private final Socket socket;
    private final ClientState clientState;

    public PeerToPeerClientHandler(Socket socket, ClientState clientState) {
        this.socket = socket;
        this.clientState = clientState;
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
                    case Constants.STAT_REQUEST:
                        handleStat(inputStream, outputStream);
                        break;
                    case Constants.GET_REQUEST:
                        handleGet(inputStream, outputStream);
                        break;
                    default:
                        throw new UnsupportedOperationException();
                }
            } catch (IOException e) {
                LOGGER.warn(e.getMessage());
            }
        }
    }

    private void handleStat(DataInputStream inputStream, DataOutputStream outputStream) throws IOException {
        int id = inputStream.readInt();
        if (!clientState.containsFileWithId(id)) {
            outputStream.writeInt(0);
        } else {
            BitSet availableParts = clientState.getAvailableFilePartsWithId(id);
            outputStream.writeInt(availableParts.cardinality());
            for (int i = 0; i < availableParts.size(); i++) {
                if (availableParts.get(i)) {
                    outputStream.writeInt(i);
                }
            }
        }
        outputStream.flush();
    }

    private void handleGet(DataInputStream inputStream, DataOutputStream outputStream) throws IOException {
        int id = inputStream.readInt();
        int partNumber = inputStream.readInt();
        if (clientState.containsFileWithId(id) && clientState.getAvailableFilePartsWithId(id).get(partNumber)) {
            byte[] buffer = new byte[Constants.DATA_BLOCK_SIZE];
            DataInputStream fileInputStream = new DataInputStream(
                    Files.newInputStream(clientState.getPathWithId(id)));
            fileInputStream.skipBytes(partNumber * Constants.DATA_BLOCK_SIZE);
            try {
                fileInputStream.readFully(buffer);
            } catch (EOFException ignored) { }
            fileInputStream.close();
            outputStream.write(buffer);
        }
    }
}
