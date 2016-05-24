package ru.spbau.mit.ui;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.spbau.mit.model.*;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class TorrentClientFrame extends JFrame {
    private static final Logger LOGGER = LogManager.getLogger(TorrentClientFrame.class);
    private static final int FRAME_WIDTH = 1200;
    private static final int FRAME_HEIGHT = 600;
    private static final String FRAME_NAME = "Torrent";

    private TorrentsTable table;
    private TimerTask torrentsTableUpdateTask;

    public TorrentClientFrame(Client client) throws IOException {
        super(FRAME_NAME);
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                try {
                    client.stop();
                    client.save();
                } catch (IOException e) {
                    LOGGER.warn("Save request exception: " + e.getMessage());
                }
            }
        });
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        client.start(Settings.getIp());
        buildMenuBar(client);

        table = new TorrentsTable(client);
        add(new JScrollPane(table));

        torrentsTableUpdateTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    List<FileInfo> filesList = client.getFilesList();
                    table.setFilesList(filesList);
                    table.revalidate();
                    table.repaint();
                } catch (IOException e) {
                    LOGGER.warn("Exception during receiving files list from server: " + e.getMessage());
                }
            }
        };

        Timer torrentsTableUpdateTimer = new Timer();
        torrentsTableUpdateTimer.schedule(torrentsTableUpdateTask, 0, Constants.UPDATE_TABLE_PERIOD);

        setSize(FRAME_WIDTH, FRAME_HEIGHT);
    }

    private void buildMenuBar(Client client) throws IOException {
        JMenu fileMenu = new JMenu("File");
        JMenuItem uploadItem = new JMenuItem("Upload new file");
        uploadItem.addActionListener((ActionEvent actionEvent) -> uploadFile(client));
        fileMenu.add(uploadItem);

        JMenu settingsMenu = new JMenu("Settings");
        JMenuItem serverIpItem = new JMenuItem("Set server IP");
        serverIpItem.addActionListener((ActionEvent actionEvent) -> resetServerIp(client));
        settingsMenu.add(serverIpItem);

        JMenuBar menuBar = new JMenuBar();
        menuBar.add(fileMenu);
        menuBar.add(settingsMenu);
        setJMenuBar(menuBar);
    }

    private void uploadFile(Client client) {
        JFileChooser fileChooser = new JFileChooser();
        int returnValue = fileChooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                client.upload(file.getAbsolutePath());
                torrentsTableUpdateTask.run();

            } catch (IOException e) {
                LOGGER.warn("Exception during upload request: " + e.getMessage());
            }
        }
    }

    private void resetServerIp(Client client) {
        try {
            client.stop();
            new ServerIpDialog().setVisible(true);
            client.start(Settings.getIp());
            repaint();
        } catch (IOException e) {
            LOGGER.warn("Exception during reset server IP address: " + e.getMessage());
        }
    }
}
