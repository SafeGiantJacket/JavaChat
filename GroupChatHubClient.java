import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class GroupChatHubClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    private ArrayList<String> joinedGroups;

    // GUI components
    private JFrame frame;
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton, fileButton, createGroupButton, deleteGroupButton;
    private JList<String> groupsList;
    private DefaultListModel<String> groupModel;

    public GroupChatHubClient(String username) {
        this.username = username;
        this.joinedGroups = new ArrayList<>();
        createGUI();
    }

    public void connect(String host, int port) {
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Start a thread to listen for messages from the server
            new Thread(new ServerHandler()).start();

            // Send username to the server
            out.println(username);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createGUI() {
        frame = new JFrame(username + " - GroupChat Hub");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 500);
        frame.setLayout(new BorderLayout());

        // Chat Area
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(chatArea);

        // Message Input and Send Button
        messageField = new JTextField();
        sendButton = new JButton("Send");

        JPanel messagePanel = new JPanel();
        messagePanel.setLayout(new BorderLayout());
        messagePanel.add(messageField, BorderLayout.CENTER);
        messagePanel.add(sendButton, BorderLayout.EAST);

        // Send File Button
        fileButton = new JButton("Send File");

        JPanel filePanel = new JPanel();
        filePanel.setLayout(new FlowLayout());
        filePanel.add(fileButton);

        // Group List Panel (Sidebar)
        groupModel = new DefaultListModel<>();
        groupsList = new JList<>(groupModel);
        JScrollPane groupScrollPane = new JScrollPane(groupsList);
        groupScrollPane.setPreferredSize(new Dimension(150, 0));
        JLabel groupsLabel = new JLabel("Joined Groups");

        JPanel groupPanel = new JPanel();
        groupPanel.setLayout(new BorderLayout());
        groupPanel.add(groupsLabel, BorderLayout.NORTH);
        groupPanel.add(groupScrollPane, BorderLayout.CENTER);

        // Add group creation and deletion buttons
        createGroupButton = new JButton("Create Group");
        deleteGroupButton = new JButton("Delete Group");

        createGroupButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                createGroup();
            }
        });

        deleteGroupButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteGroup();
            }
        });

        JPanel groupActionPanel = new JPanel();
        groupActionPanel.setLayout(new FlowLayout());
        groupActionPanel.add(createGroupButton);
        groupActionPanel.add(deleteGroupButton);

        groupPanel.add(groupActionPanel, BorderLayout.SOUTH);

        // Add components to frame
        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);
        frame.getContentPane().add(messagePanel, BorderLayout.SOUTH);
        frame.getContentPane().add(filePanel, BorderLayout.NORTH);
        frame.getContentPane().add(groupPanel, BorderLayout.WEST);

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

        messageField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                out.println("TYPING " + username);
            }
        });

        fileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendFile();
            }
        });

        frame.setVisible(true);
    }

    private void createGroup() {
        String groupName = JOptionPane.showInputDialog("Enter new group name:");
        if (groupName != null && !groupName.isEmpty()) {
            out.println("CREATE_GROUP " + groupName);
        }
    }

    private void deleteGroup() {
        String groupName = JOptionPane.showInputDialog("Enter group name to delete:");
        if (groupName != null && !groupName.isEmpty()) {
            out.println("DELETE_GROUP " + groupName);
        }
    }

    private void sendPrivateMessage(String recipient, String message) {
        if (!message.isEmpty()) {
            out.println("PRIVATE " + recipient + " " + message);
            chatArea.append("Private to " + recipient + ": " + message + "\n");
        }
    }

    private void sendMessage() {
        String message = messageField.getText();
        if (message.startsWith("/pm ")) {
            String[] parts = message.split(" ", 3);
            if (parts.length == 3) {
                sendPrivateMessage(parts[1], parts[2]);
            }
        } else {
            if (!message.isEmpty()) {
                out.println("MESSAGE " + message);
                chatArea.append("You: " + message + "\n");
                messageField.setText("");
            }
        }
    }

    private void sendFile() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                out.println("FILE " + file.getName());
                FileInputStream fis = new FileInputStream(file);
                BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());
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

    private class ServerHandler implements Runnable {
        public void run() {
            String receivedMessage;
            try {
                while ((receivedMessage = in.readLine()) != null) {
                     if (receivedMessage.startsWith("ONLINE_USERS ")) {
                        String[] users = receivedMessage.substring(13).split(",");
                        chatArea.append("Online users: " + String.join(", ", users) + "\n");
                    } else {
                        chatArea.append(receivedMessage + "\n");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        String username = JOptionPane.showInputDialog("Enter your username:");
        GroupChatHubClient client = new GroupChatHubClient(username);
        client.connect("localhost", 6666); // Replace "localhost" with the server's IP if on a different machine
    }
}
