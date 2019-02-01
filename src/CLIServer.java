import org.json.JSONObject;
import org.json.JSONStringer;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.swing.*;
import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;

class CLIServer extends Thread {
    private String serverIP;
    private int serverPort;
    private final byte[] defaultJson;
    private RSA rsa;

    CLIServer(String serverIP, int serverPort, String sessionID, RSA rsa) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, InvalidKeySpecException {
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        this.rsa = rsa;
        defaultJson = rsa.encryptEverything(new JSONStringer().object().key("message_type").value("ping").key("data").value(sessionID).endObject().toString(), false).getBytes(StandardCharsets.UTF_8);
    }

    public void run() {
        Socket socket;
        while (true) {
            try {
                socket = new Socket(serverIP, serverPort);
                OutputStream outputStream = new BufferedOutputStream(socket.getOutputStream());
                outputStream.write(defaultJson);
                outputStream.flush();
                InputStream inputStream = new DataInputStream(socket.getInputStream());
                byte[] encryptedMessage = inputStream.readAllBytes();
                try {
                    byte[] decryptedMessage = rsa.decryptAES(encryptedMessage);
                    JSONObject json = new JSONObject(new String(decryptedMessage));
                    switch (json.get("status").toString()) {
                        case "success":
                            showMessage(new String(decryptedMessage));
                            break;
                        case "error":
                            System.out.println("You lost your authentication");
                            showMessage(new String(decryptedMessage));
                    }
                } catch (NoSuchPaddingException | NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException | InvalidKeyException | InvalidAlgorithmParameterException e) {
                    e.printStackTrace();
                    System.out.println("Failed to decrypt");
                } catch (IllegalArgumentException e) {
                    System.out.println("Username not found");
                    break;
                }
                socket.close();
            } catch (ConnectException e) {
                System.out.println("Connection closed");
                break;
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Failed to listen");
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void showMessage(String message) {
        System.out.println(message);
        JOptionPane.showMessageDialog(null, "Recieved: " + message);
    }
}
