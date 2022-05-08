import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.net.*;

public class Client {
    String clientName;
    JFrame username, app;
    JPanel panel, inputPanel;
    GridLayout layout;
    JLabel L1;
    JButton enterBtn;
    JTextField input;
    JTextArea textArea;
    JScrollPane scrollPanel;
    Font font, font1;
    Socket socket;
    BufferedReader bufferedReader;
    BufferedWriter bufferedWriter;

    public void startGui() {
        // the first frame is used to enter the username
        username = new JFrame("Enter username");
        font = new Font("Monospace", Font.PLAIN, 16);
        font1 = new Font("Monospace", Font.PLAIN, 25);

        panel = new JPanel(new GridLayout(0, 1, 0, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 30, 10, 30));

        L1 = new JLabel("Enter the username");
        L1.setHorizontalAlignment(JLabel.CENTER);
        L1.setFont(font);

        input = new JTextField();
        input.setFont(font);
        input.setHorizontalAlignment(JTextField.CENTER);

        enterBtn = new JButton("ENTER THE CHAT ROOM");
        enterBtn.setFont(font1);

        // when the button is pressed, the method core() is launched
        enterBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (input.getText().equals(""))
                    return;
                
                clientName = input.getText();
                
                try {
                    core();
                } catch (IOException e1) {
                    L1.setText("Server not found...");
                }
            }
        });

        panel.add(L1);
        panel.add(input);
        panel.add(enterBtn);

        username.add(panel, BorderLayout.CENTER);

        username.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        username.pack();
        username.setLocationRelativeTo(null);
        username.setVisible(true);
    }

    public void core() throws UnknownHostException, IOException {
        // the method core() manages the connection to the server and starts
        // the chat frame
        this.socket = new Socket("127.0.0.1", 8080);
        this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.bufferedWriter= new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

        // Initially send the username of the client.
        bufferedWriter.write(clientName);
        bufferedWriter.newLine();
        bufferedWriter.flush();

        // make the first frame invisible
        username.setVisible(false);
        app = new JFrame("App");
        enterBtn = new JButton("Send message");
        enterBtn.setFont(font1);
        input = new JTextField();
        input.setUI(new HintTextFieldUI("Type here your message...", true));
        input.setFont(font);
        
        panel = new JPanel(new BorderLayout());
        inputPanel = new JPanel();
        GroupLayout layout = new GroupLayout(inputPanel);
        inputPanel.setLayout(layout);

        // this text area is going to contain all the messages of the chat
        textArea = new JTextArea();
        textArea.setFont(font);
        scrollPanel = new JScrollPane(textArea);
        
        //add a scrollabar to the text area
        scrollPanel.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        textArea.setEditable(false);

        layout.setAutoCreateContainerGaps(true);
        layout.setAutoCreateGaps(true);

        layout.setHorizontalGroup(layout.createSequentialGroup()
                .addComponent(input)
                .addComponent(enterBtn,
                        GroupLayout.DEFAULT_SIZE,
                        GroupLayout.DEFAULT_SIZE,
                        GroupLayout.PREFERRED_SIZE)
        );

        layout.setVerticalGroup(layout.createParallelGroup()
                .addComponent(enterBtn, GroupLayout.DEFAULT_SIZE,
                        GroupLayout.DEFAULT_SIZE,
                        GroupLayout.PREFERRED_SIZE)
                .addComponent(input)
        );
        
        panel.add(scrollPanel, BorderLayout.CENTER);
        panel.add(inputPanel, BorderLayout.PAGE_END);
        
        app.add(panel, BorderLayout.CENTER);
        
        app.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        app.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        app.pack();
        app.setMinimumSize(new Dimension(600, 700));
        app.setLocationRelativeTo(null);
        app.setVisible(true);

        // after setting up the gui, the app starts listening
        // for messages from the server or other clients
        listenForMessage();
        
        // if the user clicks the button, the sendMessage method is launched
        enterBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (input.getText().equals(""))
                    return;
                
                sendMessage(input.getText());
            }
        });
    }

    public void sendMessage(String messageToSend) {
        try {
            if (messageToSend.equals("exit()"))
                System.exit(0);

            // add the message to the text area and then send it to the server
            textArea.append("\n" + clientName + ": " + messageToSend);
            input.setText("");
            bufferedWriter.write(clientName + ": " + messageToSend);
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) { }
    }

    // Listening for a message is blocking so need a separate thread for that.
    public void listenForMessage() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String msgFromGroupChat;

                // While there is still a connection with the server, continue to listen for messages on a separate thread.
                while (socket.isConnected()) {
                    try {
                        // Get the messages sent from other users and print it to the console.
                        msgFromGroupChat = bufferedReader.readLine();
                        textArea.append("\n" + msgFromGroupChat);
                    } catch (IOException e) { }
                }
            }
        }).start();
    }

    public static void main(String[] args) {
        Client app = new Client();

        app.startGui();
    }
}
