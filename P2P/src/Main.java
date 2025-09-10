import java.util.Scanner;

public class Main {
    private static boolean yes(String s) {
        if (s == null) return false;
        s = s.trim().toLowerCase();
        return s.equals("s") || s.equals("sim") || s.equals("y") || s.equals("yes");
    }

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            // Nome e porta (sempre definidos pelo usuário)
            System.out.print("Digite seu nome de usuário: ");
            String username = scanner.nextLine().trim();

            Integer port = null;
            while (port == null) {
                System.out.print("Digite a porta: ");
                String p = scanner.nextLine().trim();
                try {
                    port = Integer.parseInt(p);
                } catch (NumberFormatException e) {
                    System.out.println("Porta inválida. Tente novamente (ex: 8082).");
                }
            }

            Peer peer = new Peer(username, port);

            // Pergunta se quer ligar a descoberta (só facilita achar outros peers na mesma LAN)
            System.out.print("Habilitar descoberta de peers (multicast LAN)? (s/n): ");
            boolean enableDiscovery = yes(scanner.nextLine());

            // Inicia: acceptor TCP + (opcional) descoberta, sem ler teclado ainda
            peer.start(enableDiscovery, false);

            System.out.println("\nComandos:");
            System.out.println("  /connect <host> <port>  - conecta a um peer (ex: /connect localhost 8082)");
            System.out.println("  /peers                  - lista conexões ativas");
            System.out.println("  /found                  - lista peers encontrados na LAN (descoberta)");
            System.out.println("  /history                - mostra histórico da sessão");
            System.out.println("  /exit                   - encerra com segurança");
            System.out.println("Digite mensagens para enviar em broadcast...\n");

            // Agora começa a ler o teclado
            peer.startUserInput();
            // Não fechar scanner para não fechar System.in
        }
    }
}
