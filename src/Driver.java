import org.json.JSONStringer;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.Scanner;

public class Driver {

    private final static String serverIP = "127.0.0.1";
    private final static int serverPort = 1515;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Encryptor encryptor = new Encryptor();
        Sender sender = new Sender(serverIP, serverPort, encryptor);
        if (!new File("public_key.der").exists()) {
            try {
                URL website = new URL( "https://raw.githubusercontent.com/bencarlisle15/SCAHost/master/public_key.der");
                InputStream in = website.openStream();
                Files.copy(in, Paths.get("public_key.der"), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                System.out.println("Could not download public key");
                System.exit(0);
            }
        }
        while (true) {
            String response = scanner.nextLine();
            if (response.equals("exit")) {
                System.exit(0);
            }
            String[] arguments = response.split(" ");
            if (arguments.length == 0) {
                continue;
            }
            String user, password, jsonText, message, sessionID, to;
            switch (arguments[0]) {
                case "register":
                    if (arguments.length != 3) {
                        System.out.println("Wrong number of arguments");
                        break;
                    }
                    user = arguments[1];
                    encryptor.setUser(user);
                    password = arguments[2];
                    jsonText = new JSONStringer().object().key("messageType").value("register").key("nonce").value(encryptor.generateNonce()).key("password").value(password).key("publicKey").value(Base64.getEncoder().encodeToString(encryptor.getSelfPublicKey().getEncoded())).endObject().toString();
                    sender.sendMessage(encryptor.encryptEverything(jsonText, true));
                    break;
                case "login":
                    if (arguments.length != 3) {
                        System.out.println("Wrong number of arguments");
                        break;
                    }
                    user = arguments[1];
                    encryptor.setUser(user);
                    password = arguments[2];
                    jsonText = new JSONStringer().object().key("messageType").value("login").key("nonce").value(encryptor.generateNonce()).key("password").value(password).endObject().toString();
                    sender.sendMessage(encryptor.encryptEverything(jsonText, false));
                    break;
                case "send":
                    if (arguments.length < 3) {
                        System.out.println("Wrong number of arguments");
                        break;
                    }
                    to = arguments[1];
                    message = createMessage(arguments);
                    sessionID = sender.getSessionID();
                    jsonText = new JSONStringer().object().key("messageType").value("sendMessage").key("nonce").value(encryptor.generateNonce()).key("to").value(to).key("isFile").value(false).key("message").value(message).key("sessionId").value(sessionID).endObject().toString();
                    sender.sendMessage(encryptor.encryptEverything(jsonText, false));
                    break;
                case "file":
                    if (arguments.length != 2) {
                        System.out.println("Wrong number of arguments");
                        break;
                    }
                    to = arguments[1];
                    FileDialog fd;
                    do {
                        fd = new FileDialog(new JFrame());
                        fd.setVisible(true);
                    } while (fd.getFiles().length != 1);
                    message = getBytesFromFile(fd.getFiles()[0]);
                    sessionID = sender.getSessionID();
                    jsonText = new JSONStringer().object().key("messageType").value("sendMessage").key("nonce").value(encryptor.generateNonce()).key("to").value(to).key("isFile").value(true).key("message").value(message).key("sessionId").value(sessionID).endObject().toString();
                    sender.sendMessage(encryptor.encryptEverything(jsonText, false));
                    break;
                default:
                    System.out.println("Command not found");
            }
        }
    }

    private static String getBytesFromFile(File file) {
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            return Base64.getEncoder().encodeToString(fileInputStream.readAllBytes());
        } catch (IOException e) {
            return Base64.getEncoder().encodeToString(new byte[0]);
        }
    }

    private static String createMessage(String[] arguments) {
        StringBuilder message = new StringBuilder();
        for (int i = 2; i < arguments.length; i++) {
            message.append(arguments[i]).append(" ");
        }
        return message.toString();
    }
}
