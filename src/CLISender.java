import org.json.JSONObject;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.swing.JOptionPane;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;

public class CLISender implements Runnable {

    private String serverIP;
    private int serverPort;
    private final ArrayList<String> messages;
    private String sessionID;
    private RSA rsa;

    CLISender(String serverIP, int serverPort, RSA rsa) {
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        this.rsa = rsa;
        messages = new ArrayList<>();
    }

    public void run() {
        try {
            Socket socket = new Socket(serverIP, serverPort);
            BufferedOutputStream outputStream = new BufferedOutputStream(socket.getOutputStream());
            outputStream.write((getNextMessage()).getBytes(StandardCharsets.UTF_8));
            removeLastMessage();
            outputStream.flush();
            DataInputStream inputStream = new DataInputStream(socket.getInputStream());
            recievedMessage(inputStream.readAllBytes());
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getNextMessage() {
        synchronized (messages) {
            return messages.get(0);
        }
    }

    private void removeLastMessage() {
        synchronized (messages) {
            messages.remove(0);
        }
    }

    private void addNextMessage(String message) {
        synchronized (messages) {
            messages.add(message);
        }
    }

    void sendMessage(String jsonText) {
        addNextMessage(jsonText + "\n");
        new Thread(this).start();
    }

    String getSessionID() {
        return sessionID;
    }

    private void recievedMessage(byte[] messageBytes) {
        try {
            String message = new String(rsa.decryptAES(messageBytes));
            System.out.println(message);
            JOptionPane.showMessageDialog(null, message);
            JSONObject json = new JSONObject(message);
            if (json.has("session_id")) {
                sessionID = json.get("session_id").toString();
                try {
                    new CLIServer(serverIP, serverPort, sessionID, rsa).start();
                } catch (InvalidKeySpecException e) {
                    e.printStackTrace();
                    System.out.println("Could not start server");
                }
            }
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | IOException | BadPaddingException | IllegalBlockSizeException | InvalidKeyException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
            System.out.println("Could not decrypt");
        } catch (IllegalArgumentException e) {
            System.out.println("Username not found");
        }
    }
}
