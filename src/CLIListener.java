import javax.swing.*;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONTokener;
public class CLIListener extends Thread {

    private String serverIP;
    private int serverPort;
    private ServerSocket ss;

    public CLIListener(String serverIP, int serverPort) {
        this.serverIP = serverIP;
        this.serverPort = serverPort;
    }

    public void run() {
        try {
            ss = new ServerSocket(0);
            while(true) {
                Socket socket = ss.accept();
                System.out.println("Accepted");
                InputStream inputStream = new DataInputStream(socket.getInputStream());
                String jsonText = new String(inputStream.readAllBytes());
                System.out.println(jsonText);
                JSONObject json = new JSONObject(new JSONTokener(jsonText));
                String senderText = json.getString("from");
                String messageText = json.getString("message");
                if (messageText.equals("exit")) {
                    break;
                }
                showMessage("New message from " + senderText + ": " + messageText);
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getCurrentPort() {
        return ss.getLocalPort();
    }

    public String getCurrentIP() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void showMessage(String message) {
        System.out.println(message);
        JOptionPane.showMessageDialog(null, message);
    }
}
