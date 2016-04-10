package ru.spbau.mit;

public final class Constants {
    private Constants() {}
    public static final short SERVER_PORT = 8081;
    public static final int LIST_REQUEST = 1;
    public static final int UPLOAD_REQUEST = 2;
    public static final int SOURCES_REQUEST = 3;
    public static final int UPDATE_REQUEST = 4;
    public static final int STAT_REQUEST = 1;
    public static final int GET_REQUEST = 2;
    public static final long UPDATE_REQUEST_DELAY = 60 * 1000;
    public static final int DATA_BLOCK_SIZE = 10 * 1024 * 1024;
}
