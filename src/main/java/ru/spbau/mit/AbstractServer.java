package ru.spbau.mit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public abstract class AbstractServer implements Server {
    private static final Logger LOGGER = LogManager.getLogger(AbstractServer.class);

    private ServerSocket serverSocket;
    private final int port;
    private ExecutorService taskExecutor;
    private Function<Socket, Runnable> handlerFactory;

    public AbstractServer(int port) {
        this.port = port;
    }

    @Override
    public void start() {
        LOGGER.info("Server has started");
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            LOGGER.fatal("Failed to create ServerSocket: " + e.getMessage());
            System.exit(1);
        }
        taskExecutor = Executors.newCachedThreadPool();
        taskExecutor.execute(() -> {
            while (true) {
                synchronized (this) {
                    if (serverSocket == null || serverSocket.isClosed()) {
                        break;
                    }
                }
                try {
                    Socket clientSocket = serverSocket.accept();
                    taskExecutor.execute(handlerFactory.apply(clientSocket));
                } catch (IOException e) {
                    LOGGER.warn("Exception during accepting client: " + e.getMessage());
                }
            }
        });
    }

    @Override
    public synchronized void stop() {
        if (serverSocket == null) {
            return;
        }
        try {
            taskExecutor.shutdown();
            serverSocket.close();
        } catch (IOException e) {
            LOGGER.warn("Exception during closing ServerSocket" + e.getMessage());
        }
        serverSocket = null;
    }

    @Override
    public void join() throws InterruptedException {
        taskExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    }

    public synchronized int getServerSocketPort() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            return serverSocket.getLocalPort();
        }
        return 0;
    }

    protected void setHandlerFactory(Function<Socket, Runnable> handlerFactory) {
        this.handlerFactory = handlerFactory;
    }
}
