import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;

public class ChatServerGUI {
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;

    // GUI components
    private JFrame frame;
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton, fileButton, audioButton;

    public ChatServerGUI() {
        username = JOptionPane.showInputDialog("Enter your username:");
        createGUI();
    }

    public void start(int port) {
        try {
            serverSocket = new ServerSocket(port);
            chatArea.append("Server started. Waiting for a client to connect...\n");
            clientSocket = serverSocket.accept();
            chatArea.append("Client connected!\n");

            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            // Start a thread to listen for messages from the client
            new Thread(new ClientHandler()).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createGUI() {
        frame = new JFrame(username + " - Chat Server");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 500);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(chatArea);

        messageField = new JTextField();
        sendButton = new JButton("Send");
        fileButton = new JButton("Send File");
        audioButton = new JButton("Send Audio");

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(messageField, BorderLayout.CENTER);
        panel.add(sendButton, BorderLayout.EAST);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new FlowLayout());
        bottomPanel.add(fileButton);
        bottomPanel.add(audioButton);

        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);
        frame.getContentPane().add(panel, BorderLayout.SOUTH);
        frame.getContentPane().add(bottomPanel, BorderLayout.NORTH);

        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        messageField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        fileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendFile();
            }
        });

        audioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendAudio();
            }
        });

        frame.setVisible(true);
    }

    private void sendMessage() {
        String message = messageField.getText();
        if (!message.isEmpty()) {
            out.println(username + ": " + message);
            chatArea.append("You: " + message + "\n");
            messageField.setText("");
        }

        if (message.equalsIgnoreCase("bye")) {
            stop();
        }
    }

    private void sendFile() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                out.println(username + " is sending a file: " + file.getName());
                FileInputStream fis = new FileInputStream(file);
                BufferedOutputStream bos = new BufferedOutputStream(clientSocket.getOutputStream());
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    bos.write(buffer, 0, bytesRead);
                }
                bos.flush();
                chatArea.append("File sent: " + file.getName() + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendAudio() {
        try {
            AudioFormat format = new AudioFormat(44100, 16, 2, true, true);
            TargetDataLine microphone = AudioSystem.getTargetDataLine(format);
            microphone.open(format);
            microphone.start();

            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;

            chatArea.append("Recording audio...\n");
            for (int i = 0; i < 50; i++) {  // Recording duration controlled by loop
                bytesRead = microphone.read(buffer, 0, buffer.length);
                outStream.write(buffer, 0, bytesRead);
            }

            chatArea.append("Audio recorded. Sending...\n");
            microphone.close();
            byte[] audioData = outStream.toByteArray();
            OutputStream os = clientSocket.getOutputStream();
            os.write(audioData);
            os.flush();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stop() {
        try {
            in.close();
            out.close();
            clientSocket.close();
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class ClientHandler implements Runnable {
        public void run() {
            String receivedMessage;
            try {
                while ((receivedMessage = in.readLine()) != null) {
                    chatArea.append(receivedMessage + "\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        ChatServerGUI server = new ChatServerGUI();
        server.start(6666);
    }
}
