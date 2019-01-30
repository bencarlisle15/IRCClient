import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;


import org.json.JSONStringer;

import javax.swing.*;

public class Sender implements Runnable, EventHandler<ActionEvent> {

    private String serverIP;
    private int serverPort;
    private TextField senderField;
    private TextArea messageField;
    private final ArrayList<String> messages;

    public Sender(String serverIP, int serverPort, TextField senderField, TextArea messageField) {
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        this.senderField = senderField;
        this.messageField = messageField;
        messages = new ArrayList<String>();
    }

    public void run() {
        try {
            Socket socket = new Socket(serverIP, serverPort);
            OutputStream outputStream = new BufferedOutputStream(socket.getOutputStream());
            outputStream.write(getNextMessage().getBytes(StandardCharsets.UTF_8));
            removeLastMessage();
            outputStream.flush();
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

    @Override
    public void handle(ActionEvent actionEvent) {
        String senderText = senderField.getText();
        String messageText = messageField.getText();
        addNextMessage(new JSONStringer().object().key("sender").value(senderText).key("message").value(messageText).endObject().toString() + "\n");
        senderField.clear();
        messageField.clear();
        new Thread(this).start();
    }
}
