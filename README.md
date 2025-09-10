#  Chat P2P em Java

Projeto desenvolvido para a disciplina de Computação Distribuída.  
Um sistema de chat descentralizado, onde múltiplos usuários podem se comunicar **sem servidor central**, usando **Java + Sockets (TCP/IP)**.

---

## Como compilar

Na pasta `src` do projeto:

javac Main.java Peer.java PeerDiscovery.java

yaml
Copiar código

---

## Como executar

Ainda dentro da pasta `src`:

java -cp . Main

yaml
Copiar código

---

## Passos ao iniciar

1. Digite seu nome de usuário → ex.: Alice  
2. Digite a porta TCP → ex.: 8082  
   (cada peer deve ter uma porta diferente)  
3. Pergunta sobre descoberta de peers (LAN)  
   - Digite `s` → ativa multicast para descobrir peers automaticamente na rede local.  
   - Digite `n` → conecta manualmente usando `/connect`.

---

## Comandos disponíveis no terminal

- **Enviar mensagem**  
  Basta digitar o texto e pressionar Enter → será enviado a todos os peers conectados.

- **/connect <host> <port>**  
  Conecta a outro peer manualmente.  
  Exemplo:  
/connect localhost 8082

markdown
Copiar código

- **/peers**  
Lista todos os peers conectados.

- **/found**  
Lista peers encontrados via descoberta LAN (multicast).  
Exemplo:
Peers encontrados na LAN (host:port -> nome):

192.168.0.10:8082 -> Alice
Use: /connect <host> <port>

yaml
Copiar código

- **/history**  
Mostra as mensagens da sessão atual.

- **/exit**  
Encerra o chat com segurança:
- Fecha conexões
- Para a descoberta
- Salva histórico em arquivo (`history-<usuario>-<timestamp>.txt`)

---

## Exemplo de uso local com 2 peers

**Terminal A**
java -cp . Main
Digite seu nome de usuário: Alice
Digite a porta: 8082
Habilitar descoberta de peers (multicast LAN)? (s/n): n

css
Copiar código

**Terminal B**
java -cp . Main
Digite seu nome de usuário: Bob
Digite a porta: 8083
Habilitar descoberta de peers (multicast LAN)? (s/n): n

arduino
Copiar código

No terminal do Bob:
/connect localhost 8082

yaml
Copiar código

Agora mensagens enviadas em Alice ↔ Bob aparecem em ambos os terminais.

---

## Arquivos gerados

- `history-<usuario>-<timestamp>.txt` → histórico da sessão salvo automaticamente ao sair com `/exit`.

---

## Checklist para demonstração em aula

- Abrir 2 (ou mais) terminais  
- Iniciar peers com portas diferentes  
- Conectar com `/connect` ou mostrar descoberta com `/found`  
- Trocar mensagens (broadcast funcionando)  
- Mostrar `/peers` e `/history`  
- Encerrar com `/exit` e mostrar arquivo de histórico salvo  
