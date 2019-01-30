import org.json.JSONStringer;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Scanner;

public class CLIDriver {

    private final static String serverIP = "172.16.68.71";
    private final static int serverPort = 4000;
    public static void main(String[] args) {
        try {
            mainExceptions(args);
        } catch (IOException | NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException | BadPaddingException | InvalidKeySpecException | IllegalBlockSizeException | KeyStoreException | CertificateException | UnrecoverableKeyException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
            System.out.println("RSA Error");
            System.exit(1);
        }
    }

    public static void mainExceptions(String[] args) throws InvalidKeySpecException, NoSuchAlgorithmException, IOException, InvalidKeyException, BadPaddingException, NoSuchPaddingException, IllegalBlockSizeException, KeyStoreException, CertificateException, UnrecoverableKeyException, InvalidAlgorithmParameterException {
        Scanner scanner  = new Scanner(System.in);
        CLISender sender = new CLISender(serverIP, serverPort);;
        CLIListener listener = new CLIListener(serverIP, serverPort);
        listener.start();
        RSA rsa = new RSA();
        while (true) {
            String response = scanner.nextLine();
            if (response.equals("exit")) {
                break;
            }
            String[] arguments = response.split(" ");
            if (arguments.length == 0) {
                continue;
            }
            String user, password, jsonText, encryptedJson;
            byte[] wrappedData, encryptedText;
            switch (arguments[0]) {
                case "register":
                    if (arguments.length != 3) {
                        System.out.println("Wrong number of arguments");
                        break;
                    }
                    user = arguments[1];
                    password = arguments[2];
                    jsonText = new JSONStringer().object().key("messageType").value("register").key("user").value(user).key("password").value(password).endObject().toString();
                    sender.sendMessage(rsa.encryptEverything(jsonText, user, true));
                    break;
                case "login":
                    if (arguments.length != 3) {
                        System.out.println("Wrong number of arguments");
                        break;
                    }
                    user = arguments[1];
                    password = arguments[2];
                    jsonText = new JSONStringer().object().key("messageType").value("login").key("user").value(user).key("password").value(password).key("ip").value(listener.getCurrentIP()).key("port").value(listener.getCurrentPort()).endObject().toString();
                    sender.sendMessage(rsa.encryptEverything(jsonText, user, false));
                    break;
                case "send":
                    if (arguments.length < 4) {
                        System.out.println("Wrong number of arguments");
                        break;
                    }
                    String from = arguments[1];
                    String to = arguments[2];
                    String message = createMessage(arguments);
                    String sessionID = sender.getSessionID();
                    jsonText = new JSONStringer().object().key("messageType").value("sendMessage").key("from").value(from).key("to").value(to).key("message").value(message).key("sessionID").value(sessionID).endObject().toString();
                    sender.sendMessage(new String(rsa.encryptEverything(jsonText, from, false)));
                    break;
                default:
                    System.out.println("Command not found");
            }
        }
    }

    public static String createMessage(String[] arguments) {
        StringBuilder message = new StringBuilder();
        for (int i = 3; i < arguments.length; i++) {
            message.append(arguments[i]).append(" ");
        }
        return message.toString();
    }
}
