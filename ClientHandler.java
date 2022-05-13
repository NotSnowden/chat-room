import java.io.*;
import java.net.Socket;
import java.util.*;

/*
 * When a client connects the server spawns a thread to handle the client.
 * This way the server can handle multiple clients at the same time.
*/

@SuppressWarnings("unchecked")
public class ClientHandler implements Runnable {
    // Array list of all the threads handling clients so each message can be sent to the client the thread is handling.
    public static ArrayList<ClientHandler> clientHandlers = new ArrayList<>();

    // Socket for a connection, buffer reader and writer for receiving and sending data respectively.
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String clientUsername;
    private int id;

    // Creating the client handler from the socket the server passes.
    public ClientHandler(Socket socket) {
        try {
            this.socket = socket;
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.bufferedWriter= new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            Random rand = new Random();

            this.id = rand.nextInt(0, 1000000);

            // When a client connects their username is sent.
            this.clientUsername = bufferedReader.readLine();
            // Add the new client handler to the array so they can receive messages from others.
            clientHandlers.add(this);
            unicastMessage("Welcome to this Chat Room, " + clientUsername + "!\nUsers connected: " + clientHandlers.size() + "\n");
            broadcastMessage("\r😃 SERVER: " + clientUsername + " has entered the chat!\n");
        } catch (IOException e) {
            closeEverything();
        }
    }

    // Everything in this method is run on a separate thread. We want to listen for messages
    // on a separate thread because listening (bufferedReader.readLine()) is a blocking operation.
    @Override
    public void run() {
        String messageFromClient;
        // Continue to listen for messages while a connection with the client is still established.
        while (socket.isConnected()) {
            try {
                if (bufferedReader.read() == -1) {
                    closeEverything();
                    break;
                }

                // Read what the client sent and then send it to every other client.
                messageFromClient = bufferedReader.readLine();
                broadcastMessage(messageFromClient);
            } catch (IOException e) {
                closeEverything();
                break;
            }
        }
    }

    // Send a message through each client handler thread so that everyone gets the message.
    // Basically each client handler is a connection to a client. So for any message that
    // is received, loop through each connection and send it down it.
    public void broadcastMessage(String messageToSend) {
        ArrayList<ClientHandler> clone = (ArrayList<ClientHandler>) clientHandlers.clone();

        clone.forEach(elm -> {
            try {
                if (!(elm.id == id)) {
                    elm.bufferedWriter.write(messageToSend);
                    elm.bufferedWriter.newLine();
                    elm.bufferedWriter.flush();
                }
            } catch (IOException | NullPointerException ex) {
                clientHandlers.remove(elm);
            }
        });
    }

    // Send a message to a client, specifically the last client that established a connection.
    public void unicastMessage(String messageToSend) {
        ArrayList<ClientHandler> clone = (ArrayList<ClientHandler>) clientHandlers.clone();

        clone.forEach(elm -> {
            if (elm.id == id) {
                try {
                    elm.bufferedWriter.write(messageToSend);
                    elm.bufferedWriter.newLine();
                    elm.bufferedWriter.flush();
                } catch (IOException | NullPointerException ex) {
                    clientHandlers.remove(elm);
                }
            }
        });
    }

    // Helper method to close everything so you don't have to repeat yourself.
    public void closeEverything() {
        // The client disconnected or an error occurred so remove them from the list so no message is broadcasted.
        clientHandlers.remove(this);
        broadcastMessage("\r😢 SERVER: " + clientUsername + " has left the chat!\n");
        
        try {
            socket.close();
            bufferedReader.close();
            bufferedWriter.close();
        } catch (IOException e) { }
    }
}
