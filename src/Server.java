import org.json.JSONObject;
import org.json.JSONStringer;

import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Base64;

class Server extends SocketConnection {
    private boolean running;

    private byte[] getDefaultJson() {
        return encryptor.encryptEverything(new JSONStringer().object().key("messageType").value("ping").key("nonce").value(encryptor.generateNonce()).key("sessionId").value(sessionID).endObject().toString(), false).getBytes(StandardCharsets.UTF_8);

    }

    JSONObject read(Socket socket) {
        JSONObject json = super.read(socket);
        switch (json.getInt("status")) {
            case 200:
                if (json.getBoolean("isFile")) {
                    try {
                        String filename = "file.out";
                        for (int i = 0; i < 1024; i++) {
                            if (new File(filename).exists()) {
                                filename = "file" + i + ".out";
                            } else {
                                break;
                            }
                        }
                        FileOutputStream fileOutputStream = new FileOutputStream(filename);
                        fileOutputStream.write(Base64.getDecoder().decode(json.getString("message")));
                        printMessage("File created in " + filename);
                    } catch (IOException e) {
                        System.out.println("Could not create incoming file");
                    }
                } else {
                    ZonedDateTime dateTime = Instant.ofEpochSecond(json.getInt("timestamp")).atZone(ZoneId.systemDefault());
                    String toSend = "Message from " + json.getString("sender") + " (sent " + dateTime.toLocalDate() + " at " + dateTime.toLocalTime() + "): " + json.getString("message");
                    printMessage(toSend);
                }
                break;
            case 403:
                printMessage(json.getString("message"));
                printMessage("Killing client");
                running = false;
        }
        return null;
    }

    public void killServer() {
        running = false;
    }

    public void run() {
        running = true;
        while (running) {
            Thread singleThread = new Thread(this::singleServerOperation);
            singleThread.setDaemon(true);
            singleThread.start();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void singleServerOperation() {
        try {
            Socket socket = new Socket(serverIP, serverPort);
            write(socket, getDefaultJson());
            read(socket);
            socket.close();
        } catch (ConnectException e) {
            System.out.println("Connection closed");
            running = false;
        } catch (IOException e) {
            System.out.println("Connection could not be created");
            running = false;
        }
    }
}
