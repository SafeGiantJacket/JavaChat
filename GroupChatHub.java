import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class GroupChatHub {
    private ServerSocket serverSocket;
    private ArrayList<ClientHandler> clients;
    private ArrayList<String> groupList;

    // GUI components
    private JFrame frame;
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton, fileButton;
    private DefaultListModel<String> groupModel;

    public GroupChatHub() {
        clients = new ArrayList<>();
        groupList = new ArrayList<>();
        createGUI();
    }

    public void start(int port) {
        try {
            serverSocket = new ServerSocket(port);
            chatArea.append("Server started. Waiting for clients to connect...\n");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                chatArea.append("Client connected!\n");

                // Handle client connection
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createGUI() {
        frame = new JFrame("GroupChat Hub Server");
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
        JList<String> groupsList = new JList<>(groupModel);
        JScrollPane groupScrollPane = new JScrollPane(groupsList);
        groupScrollPane.setPreferredSize(new Dimension(150, 0));
        JLabel groupsLabel = new JLabel("Created Groups");

        JPanel groupPanel = new JPanel();
        groupPanel.setLayout(new BorderLayout());
        groupPanel.add(groupsLabel, BorderLayout.NORTH);
        groupPanel.add(groupScrollPane, BorderLayout.CENTER);

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

        frame.setVisible(true);
    }

    private void sendMessage() {
        String message = messageField.getText();
        if (!message.isEmpty()) {
            broadcastMessage("Server: " + message);
            chatArea.append("You: " + message + "\n");
            messageField.setText("");
        }
    }

    private void broadcastMessage(String message) {
        for (ClientHandler client : clients) {
            client.clientOut.println(message);
        }
    }

    private void broadcastTyping(String username) {
        for (ClientHandler client : clients) {
            client.clientOut.println("TYPING " + username);
        }
    }

    private void broadcastOnlineUsers() {
        String[] users = clients.stream().map(client -> client.username).toArray(String[]::new);
        for (ClientHandler client : clients) {
            client.clientOut.println("ONLINE_USERS " + String.join(",", users));
        }
    }

    private void createGroup(String groupName) {
        if (!groupList.contains(groupName)) {
            groupList.add(groupName);
            groupModel.addElement(groupName);
            broadcastMessage("Group " + groupName + " created.");
        }
    }

    private void deleteGroup(String groupName) {
        if (groupList.contains(groupName)) {
            groupList.remove(groupName);
            groupModel.removeElement(groupName);
            broadcastMessage("Group " + groupName + " deleted.");
        }
    }

    private class ClientHandler implements Runnable {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter clientOut;
        private String username;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                clientOut = new PrintWriter(socket.getOutputStream(), true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                // First message from client should be their username
                username = in.readLine();

                // Send the list of online users to the newly connected client
                broadcastOnlineUsers();

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("PRIVATE ")) {
                        String[] parts = message.split(" ", 3);
                        String recipient = parts[1];
                        String msgContent = parts[2];
                        sendPrivateMessage(recipient, username + " (private): " + msgContent);
                    } else if (message.startsWith("MESSAGE ")) {
                        broadcastMessage(username + ": " + message.substring(8));
                    } else if (message.startsWith("TYPING ")) {
                        broadcastTyping(username);
                    } else if (message.startsWith("CREATE_GROUP ")) {
                        createGroup(message.substring(13));
                    } else if (message.startsWith("DELETE_GROUP ")) {
                        deleteGroup(message.substring(13));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                clients.remove(this);
                broadcastOnlineUsers();
            }
        }

        private void sendPrivateMessage(String recipient, String message) {
            for (ClientHandler client : clients) {
                if (client.username.equals(recipient)) {
                    client.clientOut.println(message);
                    break;
                }
            }
        }
    }

    public static void main(String[] args) {
        GroupChatHub server = new GroupChatHub();
        server.start(6666); // Start the server on port 6666
    }
}
