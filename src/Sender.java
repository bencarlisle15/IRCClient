import org.json.JSONObject;

import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class Sender extends SocketConnection {

    private final ArrayList<byte[]> messages = new ArrayList<>();
    private Server server;

    Sender(String serverIP, int serverPort, Encryptor encryptor) {
        SocketConnection.serverIP = serverIP;
        SocketConnection.serverPort = serverPort;
        SocketConnection.encryptor = encryptor;
    }

    public void run() {
        try {
            Socket socket = new Socket(serverIP, serverPort);
            if (!write(socket, getNextMessage())) {
                printMessage("Sending failed");
            }
            read(socket);
            socket.close();
        } catch (IOException e) {
            printMessage("Server not online");
        }
    }

    private byte[] getNextMessage() {
        synchronized (messages) {
            return messages.remove(0);
        }
    }

    private void addNextMessage(byte[] message) {
        synchronized (messages) {
            messages.add(message);
        }
    }

    public void killServer() {
        server.killServer();
    }

    void sendMessage(String jsonText) {
        addNextMessage((jsonText + "\n").getBytes(StandardCharsets.UTF_8));
        Thread senderThread = new Thread(this);
        senderThread.setDaemon(true);
        senderThread.start();
    }

    JSONObject read(Socket socket) {
        JSONObject json = super.read(socket);
        String message = json.getString("message");
        printMessage(message);
        if (json.getInt("status") == 202) {
            sessionID = json.get("sessionId").toString();
            server = new Server();
            Thread serverThread = new Thread(server);
            serverThread.setDaemon(true);
            serverThread.start();
        }
        return null;
    }
}
