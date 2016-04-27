package ru.spbau.mit;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class ClientInfo {
    private static final int P = 31;

    private final byte[] ip;
    private final int port;

    public ClientInfo(byte[] ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ClientInfo)) {
            return false;
        }
        ClientInfo clientInfo = (ClientInfo) obj;
        return ip == clientInfo.ip && port == clientInfo.port;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(ip) * P + port;
    }

    public void write(DataOutputStream outputStream) throws IOException {
        outputStream.write(ip);
        outputStream.writeInt(port);
    }

    public static ClientInfo read(DataInputStream inputStream) throws IOException {
        byte[] ip = new byte[Constants.IP_BYTE_NUMBER];
        inputStream.read(ip, 0, Constants.IP_BYTE_NUMBER);
        int port = inputStream.readInt();
        return new ClientInfo(ip, port);
    }

    public byte[] getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }
}
