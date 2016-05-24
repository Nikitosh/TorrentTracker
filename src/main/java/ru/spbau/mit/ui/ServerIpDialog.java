package ru.spbau.mit.ui;

import ru.spbau.mit.model.Settings;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class ServerIpDialog extends JDialog {
    public ServerIpDialog() {
        JLabel label = new JLabel("Server IP:");
        JTextField serverIpTextField;
        try {
            serverIpTextField = new JTextField(InetAddress.getByAddress(Settings.getIp()).getHostAddress());
        } catch (UnknownHostException e) {
            return;
        }

        JButton okButton = new JButton("Ok");
        okButton.addActionListener((ActionEvent actionEvent) -> {
            try {
                Settings.setIp(InetAddress.getByName(serverIpTextField.getText()).getAddress());
                dispose();
            } catch (UnknownHostException e) {
                JOptionPane.showMessageDialog(this, "Wrong IP address", "Warning", JOptionPane.WARNING_MESSAGE);
            }
        });

        Box horizontalBox = Box.createHorizontalBox();
        horizontalBox.add(label);
        horizontalBox.add(serverIpTextField);
        Box verticalBox = Box.createVerticalBox();
        verticalBox.add(horizontalBox);
        verticalBox.add(okButton);
        add(verticalBox);
        pack();
    }
}
