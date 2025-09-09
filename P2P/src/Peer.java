import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Peer {
    private String username;
    private ServerSocket serverSocket;
    private List<Socket> connection = new ArrayList<>();


    public Peer(String username, int port) {
        this.username = username;
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("P2p " + username + "esta ouvindo na porta" + port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        new Thread(this::listenForconnections).start();
        new Thread(this::listenForUserInput).start();
    }

    private void listenForconnections() {
        while (true) {
            try {
                Socket socket = serverSocket.accept();
                connection.add(socket);
                new Thread(() -> handleConnection(socket)).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleConnection(Socket socket) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println(message);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void listenForUserInput() {
        try (BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in))) {
            while (true) {
                String message = userInput.readLine();
                broadCastMessage(message);
            }
        } catch (IOException e) {
            e.printStackTrace();

        }
    }

    private void broadCastMessage(String message) {
        for (Socket socket : connection) {
            try {
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println(username + ": " + message);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    public void connectionPeer(String host, int port){
        try{
            Socket socket = new Socket(host, port);
            connection.add(socket);
            new Thread(() -> handleConnection(socket)).start();
            System.out.println("Conectado ao peer em "+host + ":" +port);
        }catch(IOException e){
            System.out.println("Erro ao conectar ao par em "+host+":"+port);
            throw new RuntimeException(e);
        }
    }
}
