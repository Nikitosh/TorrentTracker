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
    private final short port;
    private ExecutorService taskExecutor;
    private Function<Socket, Runnable> handlerFactory;

    public AbstractServer(short port) {
        this.port = port;
    }

    @Override
    public void start() {
        LOGGER.info("Server has started");
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            // ?
            e.printStackTrace();
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
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void stop() {
        if (serverSocket == null) {
            return;
        }
        try {
            taskExecutor.shutdown();
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        serverSocket = null;
    }

    @Override
    public void join() throws InterruptedException {
        taskExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    }

    protected void setHandlerFactory(Function<Socket, Runnable> handlerFactory) {
        this.handlerFactory = handlerFactory;
    }
}
