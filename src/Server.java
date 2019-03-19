import org.json.JSONObject;
import org.json.JSONStringer;

import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

class Server extends SocketConnection {
    private final byte[] defaultJson;
    private boolean running;

    Server() {
        defaultJson = encryptor.encryptEverything(new JSONStringer().object().key("messageType").value("ping").key("sessionId").value(sessionID).endObject().toString(), false).getBytes(StandardCharsets.UTF_8);
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
                    printMessage(json.toString());
                }
                break;
            case 403:
                printMessage(json.getString("message"));
                System.out.println("Killing client server");
                running = false;
        }
        return null;
    }

    public void run() {
        running = true;
        while (running) {
            new Thread(this::singleServerOperation).start();
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
            write(socket, defaultJson);
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
