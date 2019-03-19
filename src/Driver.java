import org.json.JSONStringer;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.Base64;
import java.util.Scanner;

public class Driver {

    private final static String serverIP = "127.0.0.1";
    private final static int serverPort = 1515;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Encryptor encryptor = new Encryptor();
        Sender sender = new Sender(serverIP, serverPort, encryptor);
        while (true) {
            String response = scanner.nextLine();
            if (response.equals("exit")) {
                break;
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
                    jsonText = new JSONStringer().object().key("messageType").value("register").key("user").value(user).key("password").value(password).endObject().toString();
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
                    jsonText = new JSONStringer().object().key("messageType").value("login").key("user").value(user).key("password").value(password).endObject().toString();
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
                    jsonText = new JSONStringer().object().key("messageType").value("sendMessage").key("to").value(to).key("isFile").value(false).key("message").value(message).key("sessionId").value(sessionID).endObject().toString();
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
                    jsonText = new JSONStringer().object().key("messageType").value("sendMessage").key("to").value(to).key("isFile").value(true).key("message").value(message).key("sessionId").value(sessionID).endObject().toString();
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
