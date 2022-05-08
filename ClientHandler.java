import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

/*
 * When a client connects the server spawns a thread to handle the client.
 * This way the server can handle multiple clients at the same time.
*/

public class ClientHandler implements Runnable {
    // Array list of all the threads handling clients so each message can be sent to the client the thread is handling.
    public static ArrayList<ClientHandler> clientHandlers = new ArrayList<>();

    // Socket for a connection, buffer reader and writer for receiving and sending data respectively.
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String clientUsername;

    // Creating the client handler from the socket the server passes.
    public ClientHandler(Socket socket) {
        try {
            this.socket = socket;
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.bufferedWriter= new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            // When a client connects their username is sent.
            this.clientUsername = bufferedReader.readLine();
            // Add the new client handler to the array so they can receive messages from others.
            clientHandlers.add(this);
            unicastMessage("🙌 Welcome to this Chat Room, " + clientUsername + "!\nUsers connected: " + clientHandlers.size() + "\n");
            broadcastMessage("\r😃 SERVER: " + clientUsername + " has entered the chat!\n");
        } catch (IOException | NullPointerException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
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
                // Read what the client sent and then send it to every other client.
                messageFromClient = bufferedReader.readLine();
                broadcastMessage(messageFromClient);
            } catch (IOException | NullPointerException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
                break;
            }
        }
    }

    // Send a message through each client handler thread so that everyone gets the message.
    // Basically each client handler is a connection to a client. So for any message that
    // is received, loop through each connection and send it down it.
    public void broadcastMessage(String messageToSend) {
        for (ClientHandler clientHandler : clientHandlers) {
            try {
                // You don't want to broadcast the message to the user who sent it.
                if (!clientHandler.clientUsername.equals(clientUsername)) {
                    clientHandler.bufferedWriter.write(messageToSend);
                    clientHandler.bufferedWriter.newLine();
                    clientHandler.bufferedWriter.flush();
                }
            } catch (IOException | NullPointerException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
            }
        }
    }

    // Send a message to a client, specifically the last client that established a connection.
    public void unicastMessage(String messageToSend) {
        for (ClientHandler clientHandler : clientHandlers) {
            if (clientHandler.clientUsername.equals(clientUsername)) {
                try {
                    clientHandler.bufferedWriter.write(messageToSend);
                    clientHandler.bufferedWriter.newLine();
                    clientHandler.bufferedWriter.flush();
                } catch (IOException e) {
                    closeEverything(socket, bufferedReader, bufferedWriter);
                }
            }
        }
    }

    // If the client disconnects for any reason remove them from the list so a message isn't sent down a broken connection.
    public void removeClientHandler() {
        clientHandlers.remove(this);
        broadcastMessage("\r😢 SERVER: " + clientUsername + " has left the chat!\n");
    }

    // Helper method to close everything so you don't have to repeat yourself.
    public void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        // The client disconnected or an error occurred so remove them from the list so no message is broadcasted.
        removeClientHandler();
        try {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
        }
    }
}
