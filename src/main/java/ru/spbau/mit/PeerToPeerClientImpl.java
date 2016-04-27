package ru.spbau.mit;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class PeerToPeerClientImpl implements PeerToPeerClient {
    private Socket socket;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;

    @Override
    public void connect(byte[] ip, int port) throws IOException {
        socket = new Socket(InetAddress.getByAddress(ip), port);
        inputStream = new DataInputStream(socket.getInputStream());
        outputStream = new DataOutputStream(socket.getOutputStream());
    }

    @Override
    public void disconnect() throws IOException {
        if (socket != null) {
            socket.close();
        }
    }

    @Override
    public List<Integer> executeStat(int id) throws IOException {
        outputStream.writeInt(Constants.STAT_REQUEST);
        outputStream.writeInt(id);
        outputStream.flush();
        int size = inputStream.readInt();
        List<Integer> partsList = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            partsList.add(inputStream.readInt());
        }
        return partsList;
    }

    @Override
    public byte[] executeGet(int id, int part) throws IOException {
        outputStream.writeInt(Constants.GET_REQUEST);
        outputStream.writeInt(id);
        outputStream.writeInt(part);
        outputStream.flush();
        byte[] buffer = new byte[Constants.DATA_BLOCK_SIZE];
        inputStream.readFully(buffer);
        return buffer;
    }
}
