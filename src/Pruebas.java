import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Pruebas {
    static Random random = new Random();
    public static void main(String[] args) {
        List<String> list = new ArrayList<>();
        list.add("Hola");
        list.add("Adios");
        list.add("Hasta luego Lucas");
        list.add("Esto ha sido todo amigos");
        list.add("Bye bye bye");

        System.out.println(list.toString());
        aleatorizarLista(list);
        System.out.println(list.toString());
    }

    public static void aleatorizarLista(List<String> clients){
        List<String > clientesDesordenados = new ArrayList<>(clients.size());
        while (!clients.isEmpty()){
            try {
                clientesDesordenados.add(clients.remove(random.nextInt(clients.size())));
            }catch (Exception e){
                System.out.println("Pasar al siguiente");
            }
        }
        clients.addAll(clientesDesordenados);
    }
}
