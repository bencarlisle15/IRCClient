import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;


import org.json.JSONObject;
import org.json.JSONString;
import org.json.JSONStringer;
import org.json.JSONWriter;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.swing.*;

public class CLISender implements Runnable {

    private String serverIP;
    private int serverPort;
    private final ArrayList<String> messages;
    private String sessionID;
    private RSA rsa;

    public CLISender(String serverIP, int serverPort, RSA rsa) {
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        this.rsa = rsa;
        messages = new ArrayList<>();
    }

    public void run() {
        try {
            Socket socket = new Socket(serverIP, serverPort);
            OutputStream outputStream = new BufferedOutputStream(socket.getOutputStream());
            outputStream.write((getNextMessage()).getBytes(StandardCharsets.UTF_8));
            removeLastMessage();
            outputStream.flush();
            InputStream inputStream = new DataInputStream(socket.getInputStream());
            recievedMessage(inputStream.readAllBytes());
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getNextMessage() {
        synchronized (messages) {
            System.out.println(messages.get(0));
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

    public void sendMessage(String jsonText) {
        addNextMessage(jsonText + "\n");
        new Thread(this).start();
    }

    public String getSessionID() {
        return sessionID;
    }

    private void recievedMessage(byte[] messageBytes) {
        try {
            String message = new String(rsa.decryptAES(messageBytes));
            System.out.println(message);
            JOptionPane.showMessageDialog(null, message);
            JSONObject json = new JSONObject(message);
            if (json.has("sessionID")) {
                sessionID = json.get("sessionID").toString();
                try {
                    new CLIServer(serverIP, serverPort, sessionID, rsa).start();
                } catch (InvalidKeySpecException e) {
                    e.printStackTrace();
                    System.out.println("Could not start server");
                }
            }
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | UnrecoverableKeyException | CertificateException | KeyStoreException | IOException | BadPaddingException | IllegalBlockSizeException | InvalidKeyException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
            System.out.println("Could not decrypt");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
