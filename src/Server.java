import org.json.JSONObject;
import org.json.JSONStringer;

import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

class Server extends SocketConnection {
    private final byte[] defaultJson;
    private boolean running;

    Server() {
        defaultJson = encryptor.encryptEverything(new JSONStringer().object().key("message_type").value("ping").key("data").value(sessionID).endObject().toString(), false).getBytes(StandardCharsets.UTF_8);
    }

    JSONObject read(Socket socket) {
        JSONObject json = super.read(socket);
        switch (json.getInt("status")) {
            case 200:
                printMessage(json.toString());
                break;
            case 403:
                printMessage(json.getString("message"));
                System.out.println("Killing client server");
                running = false;
        }
        return null;
    }

    public void run() {
        Socket socket;
        running = true;
        while (running) {
            try {
                socket = new Socket(serverIP, serverPort);
                write(socket, defaultJson);
                read(socket);
                socket.close();
            } catch (ConnectException e) {
                System.out.println("Connection closed");
                break;
            } catch (IOException e) {
                System.out.println("Connection could not be created");
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
