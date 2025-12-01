import javax.swing.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ImprovedImpostorServer {
    private ServerSocket serverSocket;
    private static final int PORT = 12345;

    //TODO Cambiar NORMAL_WORD por un array, leer un archivo y coger una palabra aleatoria
    private static final String NORMAL_WORD = "ELEFANTE";
    private static final String IMPOSTOR_WORD = "IMPOSTOR";


    private List<ClientHandler> clients;

    private boolean gameStarted = false;
//    private int impostorIndex = -1;
    private ClientHandler impostor;
    private Random random;
    private Map<String, String> votes;

    public ImprovedImpostorServer() {
        clients = new CopyOnWriteArrayList<>();
        random = new Random();
        votes = new ConcurrentHashMap<>();
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Servidor del Impostor iniciado en puerto " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                clients.add(clientHandler);
                new Thread(clientHandler).start();

                System.out.println("Cliente conectado. Total: " + clients.size());

                broadcastSystemMessage(clients.get(clients.size()-1).getPlayerName() + " se ha unido. Jugadores: " + clients.size());


                int start = JOptionPane.showConfirmDialog(null, "¿Comenzar?");
                System.out.println(start);

                if (start == 0) {
                    broadcastSystemMessage("¡Suficientes jugadores! El juego comenzará en 5 segundos...");
                    startCountdown();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startCountdown() {
        new Thread(() -> {
            try {
                for (int i = 5; i > 0; i--) {
                    broadcastSystemMessage("Comenzando en " + i + "...");
                    Thread.sleep(1000);
                }
                startGame();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void startGame() {
        int finalizar = -1;
        while (finalizar != 0){
        siguienteRonda(clients);
        gameStarted = true;
        votes.clear();
        int numAleatorio = random.nextInt(clients.size());
        impostor = clients.get(numAleatorio);
        clients.get(numAleatorio).setRole("IMPOSTOR");

        System.out.println("¡El juego ha comenzado!");
        System.out.println("-----------------------");
        for (ClientHandler clientHandler: clients){
            System.out.println(clientHandler);
        }



        for (int i = 0; i < clients.size(); i++) {
            ClientHandler client = clients.get(i);
            if (client.equals(impostor)) {
                client.setRole("IMPOSTOR");
                client.sendMessage("¡ERES EL IMPOSTOR!");
            } else {
                client.setRole("NORMAL");
                client.sendMessage("Tu palabra es: " + NORMAL_WORD);
            }
        }

        aleatorizarLista(clients);

        for (ClientHandler cliente : clients){
        broadcastSystemMessage("Turno de " + cliente.getPlayerName());
        JOptionPane.showMessageDialog(null,"Finalizar turno de " + cliente.getPlayerName());
        }

        broadcastSystemMessage("Votaciones");
        startVotingTimer();

        finalizar = JOptionPane.showConfirmDialog(null,"¿Finalizar?");
        }
    }

    private void aleatorizarLista(List<ClientHandler> clients){
        List<ClientHandler> clientesDesordenados = new ArrayList<>(clients.size());
        while (!clients.isEmpty()){
            try {
                clientesDesordenados.add(clients.remove(random.nextInt(clients.size())));
            }catch (Exception e){
                System.out.println("Pasar al siguiente");
            }
        }
        clients.addAll(clientesDesordenados);
    }



    private void startVotingTimer() {
        new Thread(() -> {
            try {
                Thread.sleep(120000); // 2 minutos
                if (gameStarted) {
                    endGame("Tiempo agotado! El impostor gana.");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public synchronized void processVote(String voterName, String votedName) {
        if (!gameStarted) {
            getClient(votedName).sendMessage("El juego no ha comenzado aún.");
            return;
        }
        boolean contained = false;

        for (ClientHandler cliente : clients){
            if (cliente.getPlayerName().equalsIgnoreCase(votedName)){
                contained = true;
                return;
            }
        }

        if (!contained) {
            getClient(voterName).sendMessage("Jugador inválido.");
            return;
        }

        votes.put(voterName, votedName);

        // Verificar si todos han votado
        if (votes.size() == clients.size()) {
            countVotes();
        }
    }

    private void countVotes() {
        Map<String, Integer> voteCount = new HashMap<>();

        // Contar los votos
        for (String votedName : votes.values()) {
            voteCount.put(votedName, voteCount.getOrDefault(votedName, 0) + 1);
        }

        // Encontrar el jugador más votado
        String mostVotedPlayer = null;
        int maxVotes = 0;

        for (Map.Entry<String, Integer> entry : voteCount.entrySet()) {
            if (entry.getValue() > maxVotes) {
                maxVotes = entry.getValue();
                mostVotedPlayer = entry.getKey();
            }
        }

        // Verificación final
        if (mostVotedPlayer == null) {
            endGame("Error al contar votos.");
            return;
        }

        // Comparar por nombre del jugador
        String impostorName = impostor.getPlayerName();

        if (mostVotedPlayer.equalsIgnoreCase(impostorName)) {
            endGame("¡El impostor ha sido encontrado! Los jugadores normales ganan.");
        } else {
            endGame("¡Voto incorrecto! El impostor era " + impostorName + ". Gana el impostor.");
        }
    }

    private void siguienteRonda(List<ClientHandler> clients){
        for (ClientHandler clientHandler : clients){
            clientHandler.setRole("NORMAL");
        }
        //TODO Cambiar aquí la palabra
    }

    private void endGame(String message) {
        broadcastSystemMessage(message);
        broadcastSystemMessage("El juego ha terminado. Nuevo juego comenzando en 10 segundos...");
        gameStarted = false;

        // Reiniciar después de 10 segundos
        new Thread(() -> {
            try {
                Thread.sleep(10000);
                if (clients.size() >= 2) {
                    startGame();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void broadcastSystemMessage(String message) {
        System.out.println("[SISTEMA] " + message);
        for (ClientHandler client : clients) {
            client.sendMessage("[SISTEMA] " + message);
        }
    }

    public void broadcastChatMessage(String message, ClientHandler sender) {
        String formattedMessage = "Jugador " + sender.getPlayerName() + ": " + message;
        for (ClientHandler client : clients) {
            client.sendMessage(formattedMessage);
        }
    }

    public void removeClient(ClientHandler client) {
        clients.remove(client);
        broadcastSystemMessage("Jugador " + client.getPlayerName() + " se ha desconectado.");
    }

    public ClientHandler getClient(String playerId) {
        for (ClientHandler client : clients) {
            if (client.getPlayerName().equalsIgnoreCase(playerId)) {
                return client;
            }
        }
        return null;
    }

    public List<ClientHandler> getClients() {
        return new ArrayList<>(clients);
    }

    public static void main(String[] args) {
        ImprovedImpostorServer server = new ImprovedImpostorServer();
        server.start();
    }
}