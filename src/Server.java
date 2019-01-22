import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONTokener;

import javax.swing.JOptionPane;
import java.io.*;
import java.lang.reflect.Array;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

public class Server extends Thread implements EventHandler<ActionEvent> {

    private final String serverIP;
    private final int serverPort;
    private final TextField senderField;
    private final TextArea messageField;
    private Socket socket;
    private DataInputStream inputStream;
    private OutputStream outputStream;
    private ArrayList<String> messages;

    public Server(String serverIP, int serverPort, TextField senderField, TextArea messageField){
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        this.senderField = senderField;
        this.messageField = messageField;
        messages = new ArrayList<String>();
    }

    public void run() {
        try {
//            socket = new Socket(serverIP, serverPort, InetAddress.getByName("127.0.0.1"), 40000);
            while(true) {
//                Socket socket = new Socket(serverIP, serverPort);
//                inputStream = new DataInputStream(socket.getInputStream());
                ServerSocket ss = new ServerSocket(40000);
//                if (ss.)
                Socket socket = ss.accept();
                System.out.println("Accepted");
                inputStream = new DataInputStream(socket.getInputStream());
                try {
                    if (read()) {
                        break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
//                try {
//                    write();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
                socket.close();
            }
//            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private boolean read() throws IOException {
        if (inputStream.available() != 0) {
            String jsonText = new String(inputStream.readAllBytes());
            System.out.println(jsonText);
            JSONObject json = new JSONObject(new JSONTokener(jsonText));
            String senderText = json.getString("sender");
            String messageText = json.getString("message");
            if (messageText.equals("exit")) {
                return true;
            }
            showMessage("New message from " + senderText + ": " + messageText);
        }
        return false;
    }

    private void write() throws IOException {
        if (hasMessage()) {
            System.out.println("Writing message");
            Socket socket = new Socket(serverIP, serverPort);
            outputStream = new BufferedOutputStream(socket.getOutputStream());
            outputStream.write(getNextMessage().getBytes(StandardCharsets.UTF_8));
            removeLastMessage();
            outputStream.flush();
        }
    }

    private boolean hasMessage() {
        synchronized (messages) {
            return messages.size() > 0;
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

    public void handle(ActionEvent actionEvent) {
        String senderText = senderField.getText();
        String messageText = messageField.getText();
        addNextMessage(new JSONStringer().object().key("sender").value(senderText).key("message").value(messageText).endObject().toString() + "\n");
        senderField.clear();
        messageField.clear();
        try {
            write();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showMessage(String message) {
        System.out.println(message);
        JOptionPane.showMessageDialog(null, message);
    }
}
