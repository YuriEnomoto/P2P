import java.util.Scanner;

// Press Shift twice to open the Search Everywhere dialog and type `show whitespaces`,
// then press Enter. You can now see whitespace characters in your code.
public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        //Solicitar nome do usuário
        System.out.println("Digite seu nome de usuário: ");
        String username = scanner.nextLine();

        //Solicitar a porta
        System.out.println("Digite a porta: ");
        int port = scanner.nextInt();
        scanner.nextLine();

        //Inicializar o peer
        Peer peer = new Peer(username,port);
        peer.start();

        //Perguntar se deseja conectar a outro peer
        System.out.println("Deseja conectar outro peer? s/n: ");
        String resposta = scanner.nextLine();

        if(resposta.equalsIgnoreCase("s")){
            System.out.println("Digite o endereço do peer (host) :  ");
            String peerHost = scanner.nextLine();

            System.out.println("Digite a porta do peer (port) :  ");
            int peerPort = scanner.nextInt();
            scanner.nextLine();

            peer.connectionPeer(peerHost,peerPort);
        }
        scanner.close();
    }
}