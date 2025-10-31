package com.sarptekin.awsuploader.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;
import java.util.UUID;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class UploaderUI extends JFrame {

    private final JTextField apiUrlField;
    private final JTextField folderField;
    private final JTextField filePathField;

    public UploaderUI() {
        super("S3 Uploader");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(520, 180);
        setLocationRelativeTo(null);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.CENTER));
        top.add(new JLabel("API URL:"));
        apiUrlField = new JTextField("http://localhost:8080/api/upload", 30);
        top.add(apiUrlField);

        JPanel mid = new JPanel(new FlowLayout(FlowLayout.CENTER));
        mid.add(new JLabel("Folder (optional):"));
        folderField = new JTextField("", 20);
        mid.add(folderField);

        JPanel filePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        filePathField = new JTextField("", 25);
        filePathField.setEditable(false);
        JButton chooseBtn = new JButton("Choose File");
        chooseBtn.addActionListener(this::onChooseFile);
        filePanel.add(new JLabel("File:"));
        filePanel.add(filePathField);
        filePanel.add(chooseBtn);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton uploadBtn = new JButton("Upload");
        uploadBtn.addActionListener(this::onUpload);
        actions.add(uploadBtn);

        setLayout(new BorderLayout());
        add(top, BorderLayout.NORTH);
        add(mid, BorderLayout.CENTER);
        add(filePanel, BorderLayout.WEST);
        add(actions, BorderLayout.SOUTH);
    }

    private void onChooseFile(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            filePathField.setText(selected.getAbsolutePath());
        }
    }

    private void onUpload(ActionEvent e) {
        String apiUrl = apiUrlField.getText().trim();
        String folder = folderField.getText().trim();
        String filePath = filePathField.getText().trim();

        if (apiUrl.isEmpty()) {
            JOptionPane.showMessageDialog(this, "API URL is required.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (filePath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please choose a file.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            File file = new File(filePath);
            if (!file.exists() || !file.isFile()) {
                JOptionPane.showMessageDialog(this, "Selected file not found.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String response = uploadMultipart(apiUrl, file, folder);
            JOptionPane.showMessageDialog(this, "Uploaded. URL:\n" + response, "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Upload failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String uploadMultipart(String url, File file, String folder) throws IOException, InterruptedException {
        String boundary = "----JavaBoundary" + UUID.randomUUID();
        String fileFieldName = "file";
        String fileName = file.getName();
        String contentType = Files.probeContentType(file.toPath());
        if (contentType == null) contentType = "application/octet-stream";

        StringBuilder sb = new StringBuilder();
        // folder field if provided
        if (folder != null && !folder.isBlank()) {
            sb.append("--").append(boundary).append("\r\n");
            sb.append("Content-Disposition: form-data; name=\"folder\"\r\n\r\n");
            sb.append(folder).append("\r\n");
        }
        // file field header
        sb.append("--").append(boundary).append("\r\n");
        sb.append("Content-Disposition: form-data; name=\"").append(fileFieldName)
          .append("\"; filename=\"").append(fileName).append("\"\r\n");
        sb.append("Content-Type: ").append(contentType).append("\r\n\r\n");

        byte[] fileBytes = Files.readAllBytes(file.toPath());
        byte[] preamble = sb.toString().getBytes();
        byte[] closing = ("\r\n--" + boundary + "--\r\n").getBytes();

        byte[] body = new byte[preamble.length + fileBytes.length + closing.length];
        System.arraycopy(preamble, 0, body, 0, preamble.length);
        System.arraycopy(fileBytes, 0, body, preamble.length, fileBytes.length);
        System.arraycopy(closing, 0, body, preamble.length + fileBytes.length, closing.length);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return response.body();
        }
        throw new IOException("Upload failed: HTTP " + response.statusCode() + " - " + response.body());
    }

    public static void launch() {
        SwingUtilities.invokeLater(() -> {
            UploaderUI ui = new UploaderUI();
            ui.setVisible(true);
        });
    }
}


