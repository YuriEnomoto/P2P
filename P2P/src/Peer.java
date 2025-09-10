import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Peer {
    private final String username;
    private final int port;
    private ServerSocket serverSocket;

    // TCP
    private final List<Socket> connections = new CopyOnWriteArrayList<>();
    private final Map<Socket, String> remoteNames = Collections.synchronizedMap(new HashMap<>());
    private final Set<String> knownEndpoints = Collections.synchronizedSet(new HashSet<>());

    // Descoberta (recebidos via multicast)
    private final Map<String, String> discoveredByKey = Collections.synchronizedMap(new HashMap<>()); // key host:port -> nome
    private PeerDiscovery discovery;

    // Histórico
    private final List<String> history = Collections.synchronizedList(new ArrayList<>());
    private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

    private volatile boolean userInputStarted = false;

    public Peer(String username, int port) {
        this.username = username;
        this.port = port;
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Peer " + username + " ouvindo na porta " + port);
        } catch (IOException e) {
            System.err.println("Falha ao abrir porta " + port + ": " + e.getMessage());
            System.exit(1);
        }
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    /** Inicia acceptor TCP e opcionalmente descoberta e leitura de teclado */
    public void start(boolean enableDiscovery, boolean withUserInput) {
        new Thread(this::listenForConnections, "accept-thread").start();
        if (enableDiscovery) startDiscovery();
        if (withUserInput) startUserInput();
    }

    /** Overload para compatibilidade (liga tudo) */
    public void start() {
        start(true, true);
    }

    public void startUserInput() {
        if (userInputStarted) return;
        userInputStarted = true;
        new Thread(this::listenForUserInput, "stdin-thread").start();
    }

    public void startDiscovery() {
        if (discovery == null) {
            discovery = new PeerDiscovery(username, port, this);
            discovery.start();
        }
    }

    private void listenForConnections() {
        while (!serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();
                registerConnection(socket);
                new Thread(() -> handleConnection(socket), "conn-" + socket.getRemoteSocketAddress()).start();
            } catch (IOException e) {
                if (!serverSocket.isClosed()) {
                    System.err.println("Erro no accept: " + e.getMessage());
                }
            }
        }
    }

    private void handleConnection(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            // Se fomos o lado que aceitou, enviamos nosso HELLO logo que possível
            sendHello(socket);

            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("HELLO ")) {
                    String remote = line.substring(6).trim();
                    remoteNames.put(socket, remote);
                    printlnAndStore("[" + timeNow() + "] " + remote + " conectou (" + socket.getRemoteSocketAddress() + ")");
                } else if (line.startsWith("MSG ")) {
                    String remote = remoteNames.getOrDefault(socket, "desconhecido");
                    String msg = line.substring(4);
                    printlnAndStore("[" + timeNow() + "] " + remote + ": " + msg);
                } else {
                    String remote = remoteNames.getOrDefault(socket, "desconhecido");
                    printlnAndStore("[" + timeNow() + "] " + remote + ": " + line);
                }
            }
        } catch (IOException ignored) {
            // queda de conexão
        } finally {
            closeConnection(socket);
        }
    }

    private void listenForUserInput() {
        try (BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = userInput.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;

                if (trimmed.equalsIgnoreCase("/exit")) {
                    shutdown();
                    break;
                } else if (trimmed.equalsIgnoreCase("/peers")) {
                    listPeers();
                } else if (trimmed.equalsIgnoreCase("/found")) {
                    listFound();
                } else if (trimmed.equalsIgnoreCase("/history")) {
                    printHistory();
                } else if (trimmed.startsWith("/connect ")) {
                    String[] parts = trimmed.split("\\s+");
                    if (parts.length == 3) {
                        String host = parts[1];
                        try {
                            int p = Integer.parseInt(parts[2]);
                            connectToPeer(host, p);
                        } catch (NumberFormatException e) {
                            System.out.println("Uso: /connect <host> <port>  (ex: /connect localhost 8082)");
                        }
                    } else {
                        System.out.println("Uso: /connect <host> <port>");
                    }
                } else {
                    // mensagem normal -> broadcast
                    broadcastMessage(trimmed);
                    printlnAndStore("[" + timeNow() + "] " + username + " (você): " + trimmed);
                }
            }
        } catch (IOException ignored) {
            // input fechado (EOF)
        }
    }

    public void connectToPeer(String host, int port) {
        String key = endpointKey(host, port);
        if (knownEndpoints.contains(key)) {
            System.out.println("Já conectado (ou pendente) a " + host + ":" + port);
            return;
        }
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), 3000);
            registerConnection(socket);
            sendHello(socket); // nosso nome
            new Thread(() -> handleConnection(socket), "conn-" + socket.getRemoteSocketAddress()).start();
            System.out.println("Conectado a " + host + ":" + port);
        } catch (IOException e) {
            System.err.println("Falha ao conectar em " + host + ":" + port + " -> " + e.getMessage());
        }
    }

    private void broadcastMessage(String message) {
        String line = "MSG " + message;
        for (Socket s : connections) {
            sendLine(s, line);
        }
    }

    private void listPeers() {
        System.out.println("Peers conectados: " + connections.size());
        for (Socket s : connections) {
            String remote = remoteNames.getOrDefault(s, "?");
            System.out.println(" - " + s.getRemoteSocketAddress() + " [" + remote + "]");
        }
    }

    private void listFound() {
        synchronized (discoveredByKey) {
            if (discoveredByKey.isEmpty()) {
                System.out.println("Nenhum peer encontrado ainda. (Habilite descoberta ou aguarde anúncios)");
                return;
            }
            System.out.println("Peers encontrados na LAN (host:port -> nome):");
            for (Map.Entry<String, String> e : discoveredByKey.entrySet()) {
                System.out.println(" - " + e.getKey() + " -> " + e.getValue());
            }
        }
        System.out.println("Use: /connect <host> <port>");
    }

    private void printHistory() {
        System.out.println("=== Histórico desta sessão (" + history.size() + " msgs) ===");
        synchronized (history) {
            for (String h : history) System.out.println(h);
        }
        System.out.println("===============================================");
    }

    private void registerConnection(Socket socket) {
        connections.add(socket);
        String host = ((InetSocketAddress) socket.getRemoteSocketAddress()).getAddress().getHostAddress();
        int p = ((InetSocketAddress) socket.getRemoteSocketAddress()).getPort();
        knownEndpoints.add(endpointKey(host, p));
    }

    private void closeConnection(Socket socket) {
        try {
            connections.remove(socket);
            remoteNames.remove(socket);
            socket.close();
        } catch (IOException ignored) {}
    }

    private void sendHello(Socket socket) {
        sendLine(socket, "HELLO " + username);
    }

    private void sendLine(Socket socket, String line) {
        try {
            PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            out.println(line);
        } catch (IOException ignored) {}
    }

    public synchronized void shutdown() {
        if (serverSocket == null) return;
        System.out.println("\nEncerrando com segurança...");

        // para descoberta
        if (discovery != null) discovery.stopDiscovery();

        // fecha TCP
        for (Socket s : new ArrayList<>(connections)) {
            try { s.close(); } catch (IOException ignored) {}
        }
        connections.clear();

        try { serverSocket.close(); } catch (IOException ignored) {}
        serverSocket = null;

        saveHistoryToFile();
        System.out.println("Encerrado. Até logo!");
    }

    private void saveHistoryToFile() {
        String filename = "history-" + username + "-" + System.currentTimeMillis() + ".txt";
        try (PrintWriter pw = new PrintWriter(filename, StandardCharsets.UTF_8)) {
            synchronized (history) {
                for (String h : history) pw.println(h);
            }
            System.out.println("Histórico salvo em: " + filename);
        } catch (IOException e) {
            System.err.println("Falha ao salvar histórico: " + e.getMessage());
        }
    }

    private void printlnAndStore(String line) {
        System.out.println(line);
        history.add(line);
    }

    private String timeNow() { return sdf.format(new Date()); }
    private String endpointKey(String host, int port) { return host + ":" + port; }

    /* ==== callbacks usados pela descoberta ==== */
    void onPeerDiscovered(String host, int remotePort, String remoteName) {
        // ignora a si mesmo
        if (remotePort == this.port && isLocalhost(host)) return;
        discoveredByKey.put(endpointKey(host, remotePort), remoteName);
    }

    private boolean isLocalhost(String host) {
        try {
            InetAddress addr = InetAddress.getByName(host);
            return addr.isAnyLocalAddress() || addr.isLoopbackAddress() ||
                    NetworkInterface.getByInetAddress(addr) != null;
        } catch (Exception e) {
            return false;
        }
    }
}
