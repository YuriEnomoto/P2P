import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Descoberta simples via UDP Multicast (LAN)
 * - Peers anunciam periodicamente: DISCOVER <nome> <porta>
 * - Escutamos anúncios e só guardamos na lista de "found"
 * - O usuário escolhe conectar usando /connect <host> <porta>
 */
public class PeerDiscovery {
    private static final String GROUP = "230.0.0.1";
    private static final int PORT = 4446;

    private final String username;
    private final int tcpPort;
    private final Peer peer;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread senderThread;
    private Thread listenerThread;
    private MulticastSocket socket;

    public PeerDiscovery(String username, int tcpPort, Peer peer) {
        this.username = username;
        this.tcpPort = tcpPort;
        this.peer = peer;
    }

    public void start() {
        if (running.get()) return;
        running.set(true);

        try {
            socket = new MulticastSocket(PORT);
            socket.setReuseAddress(true);
            InetAddress group = InetAddress.getByName(GROUP);
            socket.joinGroup(group);
        } catch (Exception e) {
            System.err.println("Descoberta indisponível (multicast bloqueado?): " + e.getMessage());
            running.set(false);
            return;
        }

        senderThread = new Thread(this::sendLoop, "discovery-sender");
        listenerThread = new Thread(this::listenLoop, "discovery-listener");
        senderThread.start();
        listenerThread.start();

        System.out.println("Descoberta habilitada (multicast " + GROUP + ":" + PORT + "). Use /found para listar peers.");
    }

    public void stopDiscovery() {
        running.set(false);
        try { if (socket != null) socket.leaveGroup(InetAddress.getByName(GROUP)); } catch (Exception ignored) {}
        if (socket != null) socket.close();
    }

    private void sendLoop() {
        try (DatagramSocket ds = new DatagramSocket()) {
            ds.setReuseAddress(true);
            while (running.get()) {
                String payload = "DISCOVER " + username + " " + tcpPort;
                byte[] data = payload.getBytes(StandardCharsets.UTF_8);
                DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(GROUP), PORT);
                ds.send(packet);
                Thread.sleep(3000);
            }
        } catch (Exception ignored) {}
    }

    private void listenLoop() {
        byte[] buf = new byte[512];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        while (running.get()) {
            try {
                socket.receive(packet);
                String msg = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8).trim();
                if (msg.startsWith("DISCOVER ")) {
                    String[] parts = msg.split("\\s+");
                    if (parts.length == 3) {
                        String remoteUser = parts[1];
                        int remotePort = Integer.parseInt(parts[2]);
                        String host = packet.getAddress().getHostAddress();
                        peer.onPeerDiscovered(host, remotePort, remoteUser); // só registra; não conecta sozinho
                    }
                }
            } catch (IOException ignored) {}
        }
    }
}
