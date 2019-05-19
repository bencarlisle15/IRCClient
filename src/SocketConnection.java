import org.json.JSONObject;
import org.json.JSONStringer;

import javax.crypto.SecretKey;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.HashMap;

abstract class SocketConnection implements Runnable {

    static String serverIP;
    static int serverPort;
    static Encryptor encryptor;
    static String sessionID;


    public abstract void run();

    String getSessionID() {
        return sessionID;
    }

    boolean write(Socket socket, byte[] dataToWrite) {
        try {
            BufferedOutputStream outputStream = new BufferedOutputStream(socket.getOutputStream());
            outputStream.write(dataToWrite);
            outputStream.flush();
            socket.shutdownOutput();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    JSONObject read(Socket socket) {
        try {
            InputStream inputStream = new DataInputStream(socket.getInputStream());
            byte[] encryptedMessage = inputStream.readAllBytes();
            JSONObject json = new JSONObject(new String(encryptedMessage));
            if (!json.has("aesKey")) {
                return json;
            };
            String decryptedMessage = encryptor.decryptAES(json.getString("aesKey"), json.getString("data"), json.getString("iv"));
            if (decryptedMessage == null) {
                return new JSONObject(new JSONStringer().object().key("status").value(417).key("message").value("Decryption error occurred, try re-logging in").endObject());
            }
           return new JSONObject(decryptedMessage);
        } catch (IOException e) {
            return new JSONObject(new JSONStringer().object().key("status").value(400).key("message").value("Could not connect to server").endObject());
        }

    }

    void printMessage(String message) {
        System.out.println(message);
    }
}
