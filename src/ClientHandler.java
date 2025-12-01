import java.io.*;
import java.net.*;

class ClientHandler implements Runnable{
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private ImprovedImpostorServer server;

    private String playerName;
    private String role;
    private boolean connected;



    public ClientHandler(Socket socket, ImprovedImpostorServer server) {
        this.socket = socket;
        this.server = server;

        this.connected = true;
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            String firstLine = in.readLine();
            if (firstLine != null && firstLine.startsWith("Jugador")) {
                this.playerName = firstLine.substring(8).trim(); // obtiene "Fran"
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            out.println("Bienvenido al Juego del Impostor!");
            out.println("Esperando más jugadores...");

            String inputLine;
            while ((inputLine = in.readLine()) != null && connected) {
                /*TODO
                Desde el cliente, cuando termine la ronda de palabras y empiecen los votos, es decir
                /les llegue [SISTEMA]: Votaciones
                Al votar a alguien lo que realmente hace es enviar el mensaje '/vote $nombreVoto'
                 */
                if (inputLine.startsWith("/vote ")) {
                    try {
                        String votedId = inputLine.substring(6);
                        server.processVote(playerName, votedId);
                    } catch (NumberFormatException e) {
                        sendMessage("Voto inválido. Usa: /vote [número jugador]");
                    }
                }
                else if (inputLine.equalsIgnoreCase("/quit")) {
                    sendMessage("¡Hasta pronto!");
                    break;
                } else {
                    // Mensaje de chat normal
                    server.broadcastChatMessage(inputLine, this);
                }
            }
        } catch (IOException e) {
            System.out.println("Jugador " + playerName + " desconectado abruptamente");
        } finally {
            disconnect();
        }
    }


    public void sendMessage(String message) {
        if (out != null && connected) {
            out.println(message);
        }
    }

    @Override
    public boolean equals(Object obj) {
        ClientHandler otherClient;
        try {
            otherClient = (ClientHandler) obj;
        } catch (Exception e) {
            System.out.println("El objeto no era un Cliente");
            return false;
        }
        if (otherClient.getPlayerName().equals(this.playerName)){
            return true;
        }
        return false;
    }

    public void disconnect() {
        connected = false;
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.out.println("Error cerrando conexión del jugador " + playerName);
        }
        server.removeClient(this);
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isConnected() {
        return connected;
    }

    public String toString(){
        return "" + this.getPlayerName() + ": " + this.getRole();
    }
}