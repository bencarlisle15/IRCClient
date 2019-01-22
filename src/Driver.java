import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class Driver extends Application {

//    private final static String serverIP = "192.168.0.18";
    private final static String serverIP = "127.0.0.1";
    private final static int serverPort = 4000;


    @Override
    public void start(Stage primaryStage) {
        BorderPane mainPane = new BorderPane();
        TextArea inputText = new TextArea();
        BorderPane senderSection = new BorderPane();
        TextField inputSender = new TextField();
        Button submitButton = new Button();
        Listener listener = new Listener(serverIP, serverPort);
        Sender sender = new Sender(serverIP, serverPort, inputSender, inputText);
        submitButton.setOnAction(sender);
        senderSection.setCenter(inputSender);
        senderSection.setRight(submitButton);
        mainPane.setTop(senderSection);
        mainPane.setCenter(inputText);
        primaryStage.setTitle("Chat Window");
        primaryStage.setScene(new Scene(mainPane, 300, 275));
        primaryStage.show();
        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                System.exit(0);
            }
        });
        listener.start();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
