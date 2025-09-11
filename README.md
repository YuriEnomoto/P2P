#  Chat P2P em Java

Projeto desenvolvido para a disciplina de Computação Distribuída.  
Um sistema de chat descentralizado, onde múltiplos usuários podem se comunicar **sem servidor central**, usando **Java + Sockets (TCP/IP)**.

---

# Relatório Técnico — Chat P2P em Java

## 1. Arquitetura do Sistema

O projeto implementa um sistema de chat descentralizado (peer-to-peer) que permite a comunicação entre múltiplos usuários sem a necessidade de um servidor central.

### Componentes Principais

### Main
- Responsável pela **interface inicial com o usuário**.
- Coleta o nome de usuário e a porta TCP.
- Pergunta se o usuário deseja habilitar a descoberta de peers via LAN.
- Inicializa a classe `Peer`, passando as opções configuradas.
- Inicia a leitura das mensagens do teclado para envio em broadcast.

### Peer
- **Núcleo do sistema**. Gerencia toda a comunicação TCP entre os peers.
- Abre um `ServerSocket` na porta definida pelo usuário e aceita múltiplas conexões simultâneas.
- Possui uma lista (`connections`) para manter todos os sockets ativos e um mapa (`remoteNames`) para identificar cada peer remoto.
- Implementa:
  - **Conexão manual** (`/connect host port`): cria um `Socket` e registra a nova conexão.
  - **Identificação de usuário**: ao conectar, envia um `HELLO <username>` para que os peers saibam o nome de quem se conecta.
  - **Broadcast de mensagens**: envia para todos os peers conectados (`MSG <mensagem>`).
  - **Histórico de mensagens**: armazena todas as mensagens em memória e salva em arquivo `history-<usuario>-<timestamp>.txt` no encerramento.
  - **Comandos**: `/connect`, `/peers`, `/found`, `/history`, `/exit`.
  - **Encerramento seguro**: método `shutdown()` fecha todas as conexões, para a descoberta, fecha o `ServerSocket` e grava o histórico.

### PeerDiscovery
- Implementa o **mecanismo de descoberta de peers**, atendendo ao requisito de facilitar a conexão na rede.
- Utiliza **UDP Multicast** para anunciar e descobrir peers na LAN:
  - A cada 3 segundos envia `DISCOVER <username> <port>` para o grupo multicast `230.0.0.1:4446`.
  - Escuta os anúncios de outros peers e notifica a classe `Peer` por meio do método `onPeerDiscovered`.
- Armazena os peers descobertos em `discoveredByKey`, permitindo ao usuário listá-los com `/found` e decidir se quer conectar.

---

## 2. Decisões Técnicas

- **Protocolo simples baseado em texto**  
  - Mensagens enviadas em linhas únicas:  
    - `HELLO <username>` para identificação inicial.  
    - `MSG <mensagem>` para mensagens normais.  
  - Essa escolha facilita a depuração e a extensibilidade.

- **Uso de TCP para comunicação principal**  
  - Garante confiabilidade e entrega ordenada das mensagens entre os peers.

- **Descoberta com UDP Multicast**  
  - Permite que peers se encontrem automaticamente em uma rede local, sem depender de servidor central ou configuração manual de IP/porta.
  - O sistema **não conecta automaticamente**; apenas lista peers encontrados, deixando a decisão ao usuário.

- **Thread Safety**  
  - Estruturas como `CopyOnWriteArrayList` e `Collections.synchronizedMap` asseguram acesso seguro a dados compartilhados por várias threads (aceitação de conexões, envio e recebimento de mensagens).

- **Encerramento Seguro**  
  - Implementação de um `shutdown hook` (`Runtime.getRuntime().addShutdownHook`) para garantir que, mesmo em caso de encerramento inesperado, as conexões sejam fechadas e o histórico seja gravado.

- **Histórico Persistente**  
  - Além de exibir com `/history`, o histórico é salvo em um arquivo para consulta posterior, reforçando a rastreabilidade da conversa.

---

## 3. Dificuldades Encontradas

1. **Concorrência na leitura de entrada**  
   - Inicialmente, o `Peer` iniciava a leitura do teclado ao mesmo tempo em que o `Main` fazia perguntas, causando conflitos e `NumberFormatException`.
   - **Solução**: iniciar a leitura do teclado (`startUserInput`) somente depois da configuração inicial.

2. **Descoberta de peers sem API deprecada**  
   - O método antigo `joinGroup(InetAddress)` é marcado como deprecado nas versões atuais do Java.
   - **Solução**: atualização para a API nova (`joinGroup(SocketAddress, NetworkInterface)`), garantindo compatibilidade.

3. **Gerenciamento de conexões múltiplas e encerramento limpo**  
   - Foi necessário cuidar para que o encerramento do programa fechasse todas as conexões e threads, evitando vazamento de recursos.
   - **Solução**: uso centralizado de `shutdown()`, fechamento seguro de sockets e verificação de `isClosed()`.

4. **Evitar conexões duplicadas**  
   - Podia ocorrer de um peer se conectar mais de uma vez ao mesmo endereço.
   - **Solução**: controle por `knownEndpoints` (host:port) para evitar duplicatas.

---

## 4. Conclusão

O sistema desenvolvido:
- Permite **múltiplas conexões simultâneas** entre peers.
- **Identifica cada usuário** nas mensagens.
- Envia mensagens em **broadcast** para todos os peers conectados.
- **Registra histórico** de mensagens, exibindo em tempo real e salvando em arquivo.
- Possui um **mecanismo de descoberta** via UDP multicast para facilitar a conexão na LAN.
- Garante **encerramento seguro**, fechando conexões e salvando dados.

Essas características atendem plenamente aos requisitos funcionais e técnicos propostos, garantindo um chat descentralizado, confiável e extensível.



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

