import org.json.JSONObject;
import org.json.JSONStringer;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.swing.*;
import java.io.*;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;

public class CLIServer extends Thread {
    private String serverIP;
    private int serverPort;
    private final ArrayList<String> messages;
    private final byte[] defaultJson;
    private RSA rsa;

    public CLIServer(String serverIP, int serverPort, String sessionID, RSA rsa) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, UnrecoverableKeyException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, KeyStoreException, InvalidKeySpecException, CertificateException {
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        this.rsa = rsa;
        messages = new ArrayList<>();
        defaultJson  = rsa.encryptEverything(new JSONStringer().object().key("messageType").value("ping").key("data").value(sessionID).endObject().toString(), false).getBytes(StandardCharsets.UTF_8);
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
                    if (json.get("status").equals("success")) {
                        showMessage(new String(decryptedMessage));
                    }
                } catch (NoSuchPaddingException | NoSuchAlgorithmException | UnrecoverableKeyException | CertificateException | KeyStoreException | BadPaddingException | IllegalBlockSizeException | InvalidKeyException | InvalidAlgorithmParameterException e) {
                    e.printStackTrace();
                    System.out.println("Failed to decrypt");
                }
                Thread.sleep(100);
                socket.close();
            } catch (ConnectException e) {
                System.out.println("Connection closed");
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Failed to listen");
            }
        }
    }

    public void showMessage(String message) {
        System.out.println(message);
        JOptionPane.showMessageDialog(null, message);
    }
}
