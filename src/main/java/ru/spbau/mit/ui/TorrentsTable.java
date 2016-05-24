package ru.spbau.mit.ui;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.spbau.mit.model.Client;
import ru.spbau.mit.model.Constants;
import ru.spbau.mit.model.FileInfo;

import javax.swing.*;
import javax.swing.plaf.basic.BasicProgressBarUI;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class TorrentsTable extends JTable {
    private static final Logger LOGGER = LogManager.getLogger(TorrentsTable.class);
    private static final AbstractTorrentsTableModel TABLE_MODEL = new AbstractTorrentsTableModel();

    private Client client;

    public TorrentsTable(Client client) {
        super(TABLE_MODEL);
        this.client = client;
        getColumn("Download").setCellRenderer(new JTableButtonRenderer());
        getColumn("Progress").setCellRenderer(new JTableProgressBarRenderer());
        addMouseListener(new JTableButtonMouseListener(this));
    }

    public void setFilesList(List<FileInfo> filesList) {
        TABLE_MODEL.setFilesList(client, filesList);
    }

    private static class AbstractTorrentsTableModel extends AbstractTableModel {
        private static final String[] COLUMN_NAMES = {"Name", "Size", "Id", "Progress", "Download"};

        private Client client;
        private List<FileInfo> filesList = new ArrayList<>();
        private List<JButton> downloadButtons = new ArrayList<>();

        @Override
        public int getRowCount() {
            return filesList.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMN_NAMES.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMN_NAMES[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            //Check:OFF: MagicNumber
            switch (columnIndex) {
                case 0:
                    return filesList.get(rowIndex).getName();
                case 1:
                    return filesList.get(rowIndex).getSize();
                case 2:
                    return filesList.get(rowIndex).getId();
                case 3:
                    return client.getProgress(filesList.get(rowIndex).getId());
                case 4:
                    return downloadButtons.get(rowIndex);
                default:
                    return null;
            }
            //Check:ON: MagicNumber
        }

        public void setFilesList(Client client, List<FileInfo> filesList) {
            this.client = client;
            this.filesList = filesList;
            downloadButtons.clear();
            for (FileInfo info : filesList) {
                JButton button = new JButton("Download");
                button.addActionListener((ActionEvent actionEvent) -> {
                    try {
                        client.download(info.getId(), Paths.get(Constants.DOWNLOADS_PATH));
                    } catch (IOException e) {
                        LOGGER.warn("Exception during file download: " + e.getMessage());
                    }
                });
                downloadButtons.add(button);
            }
        }
    }

    private static class JTableButtonMouseListener extends MouseAdapter {
        private final JTable table;

        JTableButtonMouseListener(JTable table) {
            this.table = table;
        }

        public void mouseClicked(MouseEvent e) {
            int column = table.getColumnModel().getColumnIndexAtX(e.getX());
            int row = e.getY() / table.getRowHeight();
            if (row < table.getRowCount() && row >= 0 && column < table.getColumnCount() && column >= 0) {
                Object value = table.getValueAt(row, column);
                if (value instanceof JButton) {
                    ((JButton) value).doClick();
                }
            }
        }
    }

    private static class JTableButtonRenderer implements TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                                 boolean hasFocus, int row, int column) {
            JButton button = (JButton) value;
            if (isSelected) {
                button.setBackground(Color.cyan);
            }
            return button;
        }
    }

    private class JTableProgressBarRenderer implements TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            JProgressBar bar = new JProgressBar();
            bar.setUI(new BasicProgressBarUI() {
                protected Color getSelectionBackground() {
                    return Color.black;
                }

                protected Color getSelectionForeground() {
                    return Color.black;
                }
            });
            bar.setStringPainted(true);
            bar.setForeground(Color.green);
            int progress = (int) TorrentsTable.this.getValueAt(row, column);
            bar.setValue(progress);
            bar.setString(Integer.toString(progress) + "%");
            return bar;
        }
    }
}
