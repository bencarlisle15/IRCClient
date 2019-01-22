import org.json.JSONObject;
import org.json.JSONStringer;

import java.util.List;
import java.util.Scanner;

public class CLIDriver {

    private final static String serverIP = "127.0.0.1";
    private final static int serverPort = 4000;

    public static void main(String[] args) {
        Scanner scanner  = new Scanner(System.in);
        CLISender sender = new CLISender(serverIP, serverPort);;
        CLIListener listener = new CLIListener(serverIP, serverPort);
        listener.start();
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
                    jsonText = new JSONStringer().object().key("messageType").value("register").key("user").value(user).key("password").value(password).endObject().toString();
                    sender.sendMessage(jsonText);
                    break;
                case "login":
                    if (arguments.length != 3) {
                        System.out.println("Wrong number of arguments");
                        break;
                    }
                    user = arguments[1];
                    password = arguments[2];
                    String ip = listener.getCurrentIP();
                    int port = listener.getCurrentPort();
                    jsonText = new JSONStringer().object().key("messageType").value("login").key("user").value(user).key("password").value(password).key("ip").value(ip).key("port").value(port).endObject().toString();
                    sender.sendMessage(jsonText);
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
                    sender.sendMessage(jsonText);
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
