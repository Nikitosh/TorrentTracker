package ru.spbau.mit.model;

public final class Settings {
    private static final byte[] LOCALHOST = {127, 0, 0, 1};
    private static byte[] ip = LOCALHOST;

    private Settings() {}

    public static byte[] getIp() {
        return ip;
    }

    public static void setIp(byte[] ip) {
        Settings.ip = ip;
    }
}
