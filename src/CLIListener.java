import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.swing.*;
import java.io.*;
import java.net.*;

import com.dosse.upnp.UPnP;
import com.offbynull.portmapper.PortMapperFactory;
import com.offbynull.portmapper.gateway.Bus;
import com.offbynull.portmapper.gateway.Gateway;
import com.offbynull.portmapper.gateways.network.NetworkGateway;
import com.offbynull.portmapper.gateways.network.internalmessages.KillNetworkRequest;
import com.offbynull.portmapper.gateways.process.ProcessGateway;
import com.offbynull.portmapper.gateways.process.internalmessages.KillProcessRequest;
import com.offbynull.portmapper.mapper.MappedPort;
import com.offbynull.portmapper.mapper.PortMapper;
import com.offbynull.portmapper.mapper.PortType;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.logging.*;
public class CLIListener extends Thread {

    private String serverIP;
    private int serverPort;
    private ServerSocket ss;
    private MappedPort mappedPort;

    public CLIListener(String serverIP, int serverPort) {
        this.serverIP = serverIP;
        this.serverPort = serverPort;
    }

    public void run() {
        try {
            ss = new ServerSocket(0);
//            new Thread(this::forwardPort).start();
            while(true) {
                Socket socket = ss.accept();
                System.out.println("Accepted");
                InputStream inputStream = new DataInputStream(socket.getInputStream());
                String encryptedText = new String(inputStream.readAllBytes());
                try {
                    String jsonText = new String(new RSA().decryptAES(encryptedText.getBytes(StandardCharsets.UTF_8)));
                    JSONObject json = new JSONObject(new JSONTokener(jsonText));
                    String senderText = json.getString("from");
                    String messageText = json.getString("message");
                    if (messageText.equals("exit")) {
                        break;
                    }
                    showMessage("New message from " + senderText + ": " + messageText);
                } catch (NoSuchPaddingException | NoSuchAlgorithmException | UnrecoverableKeyException | CertificateException | KeyStoreException | BadPaddingException | IllegalBlockSizeException | InvalidKeyException | InvalidAlgorithmParameterException e) {
                    e.printStackTrace();
                }
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void forwardPort() {
        try {
            Gateway network = NetworkGateway.create();
            Gateway process = ProcessGateway.create();
            Bus networkBus = network.getBus();
            Bus processBus = process.getBus();
            List<PortMapper> mappers = PortMapperFactory.discover(networkBus, processBus);
            PortMapper mapper = mappers.get(0);
            System.out.println("Port: " + ss.getLocalPort());
            int port  = (int) (Math.random()*15535) + 50000;
            mappedPort = mapper.mapPort(PortType.TCP, ss.getLocalPort(), port, 3600);
            System.out.println("Port mapping added: " + mappedPort);
            while (mappedPort.getLifetime() > 1L) {
                mappedPort = mapper.refreshPort(mappedPort, mappedPort.getLifetime() / 2L);
                System.out.println("Port mapping refreshed: " + mappedPort);
                Thread.sleep(mappedPort.getLifetime() * 1000L);
            }
            System.out.println("Shut off");
            mapper.unmapPort(mappedPort);
            networkBus.send(new KillNetworkRequest());
            processBus.send(new KillProcessRequest());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public int getCurrentPort() {
        return ss.getLocalPort();
//        return mappedPort.getExternalPort();
    }

    public String getCurrentIP() {
//        return UPnP.getExternalIP();
        return "127.0.0.1";
    }

    public void showMessage(String message) {
        System.out.println(message);
        JOptionPane.showMessageDialog(null, message);
    }
}
