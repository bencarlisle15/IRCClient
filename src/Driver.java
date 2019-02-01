import org.json.JSONStringer;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Scanner;

public class Driver {

    private final static String serverIP = "172.16.68.71";
    private final static int serverPort = 4000;

    public static void main(String[] args) {
        try {
            mainExceptions();
        } catch (IOException | NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException | BadPaddingException | InvalidKeySpecException | IllegalBlockSizeException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
            System.out.println("Encryption Error");
            System.exit(1);
        }
    }

    private static void mainExceptions() throws InvalidKeySpecException, NoSuchAlgorithmException, IOException, InvalidKeyException, BadPaddingException, NoSuchPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException {
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
            String user, password, jsonText;
            switch (arguments[0]) {
                case "register":
                    if (arguments.length != 3) {
                        System.out.println("Wrong number of arguments");
                        break;
                    }
                    user = arguments[1];
                    password = arguments[2];
                    jsonText = new JSONStringer().object().key("message_type").value("register").key("user").value(user).key("password").value(password).endObject().toString();
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
                    jsonText = new JSONStringer().object().key("message_type").value("login").key("user").value(user).key("password").value(password).endObject().toString();
                    sender.sendMessage(encryptor.encryptEverything(jsonText, false));
                    break;
                case "send":
                    if (arguments.length < 3) {
                        System.out.println("Wrong number of arguments");
                        break;
                    }
                    String to = arguments[1];
                    String message = createMessage(arguments);
                    String sessionID = sender.getSessionID();
                    jsonText = new JSONStringer().object().key("message_type").value("send_message").key("from").value(encryptor.getUser()).key("to").value(to).key("message").value(message).key("session_id").value(sessionID).endObject().toString();
                    sender.sendMessage(encryptor.encryptEverything(jsonText, false));
                    break;
                default:
                    System.out.println("Command not found");
            }
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
