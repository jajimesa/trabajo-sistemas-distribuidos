package cliente;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import javax.sound.sampled.AudioFormat;

import modelo.Song;

public class AudioStreamingClient {
	
	private Socket socket;
	private Scanner teclado;
	private ObjectOutputStream outputPeticion;
	private ObjectInputStream inputRespuesta;
	
	private List<Song> canciones;
	
	/* Método que inicializa el AudioStreamingClient y que invoca al método menu() 
	 * para permitir al usuario elegir qué hacer.
	 */
	public void init() 
	{
		this.teclado = new Scanner(System.in);
		
		try
		{
			this.socket = new Socket("localhost", 6666);
			this.outputPeticion = new ObjectOutputStream(socket.getOutputStream());
			this.inputRespuesta = new ObjectInputStream(socket.getInputStream());
			
			menu();
		
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				outputPeticion.close();
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/* Método que muestra por pantalla las funcionalidades disponibles del cliente y que
	 * permite seleccionarlas leyendo un entero por pantalla. Si se selecciona la opción
	 * de desconexión, el servidor sigue pendiente de recibir una petición y lee null,
	 * con lo que sabe que el cliente se ha desconectado exitosamente.
	 */
	public void menu() {
		// Muestro el menú con las opciones al cliente.
		while(true) 
		{
			System.out.println("Cliente> Seleccione una opción:");
			System.out.println("\t 1 - Lista de canciones.");
			System.out.println("\t 2 - Solicitar canción.");
			System.out.println("\t 3 - Desconectarse.");
						
			int opcion = 0;
			while(true) 
			{
				if(teclado.hasNextInt()) 
				{
					opcion = teclado.nextInt();
					if(opcion==1||opcion==2||opcion==3) {
						break;
					} else {
						System.out.println("Cliente> Introduce un número correcto, por favor:");
					}
				}
			}
			if(opcion==1) {
				mostrarCanciones();
			}
			else if(opcion==2) {
				reproducirCancion();
			}
			else if (opcion==3) {
				// Finaliza
				return;
			}
			
			// Le doy unos segundos de cancha antes de mostrar de nuevo el menú.
			try {
				Thread.sleep(1500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	/* Método que muestra por pantalla el listado de canciones disponibles alojadas en
	 * el servidor y disponibles para reproducir por el cliente. Si todavía no se dispone
	 * del listado de canciones, lo solicita al servidor.
	 */
	public void mostrarCanciones() {
		
		// Si aún no se dispone del listado, solicítalo.
		if(this.canciones==null) {
			solicitarCanciones();
		}
		
		/* Utilizo "printf" para imprimir una tablita con las canciones. Para darle un formato
		 * más legible, calculo el tamaño del título de canción más largo y le sumo uno.
		 */
		List<String> titulos = new ArrayList<String>(canciones.size());
		canciones.forEach(s->titulos.add(s.getTitle()));
		int tituloMasLargo = titulos.stream().map(String::length).max(Integer::compare).get();
		tituloMasLargo++;
		
		// Imprimo por pantalla la tabla con el listado de canciones.
		System.out.printf("%-6s%-" + tituloMasLargo + "s%-6s\n", "Id", "Título", "Duración");
		for(int i=0; i<canciones.size(); i++) {
			String titulo = canciones.get(i).getTitle();
			Float duracion = canciones.get(i).getDuration();
			
			// Hago una transformación de la duración (seg) al formato min:seg.
			int sec = (int) (duracion % 60);
			String segundos = "";
			if(sec<10) { segundos = "0" + sec;} 
			else { segundos += sec; }
			int minutos = (int) ((duracion / 60) % 60);
			
			System.out.printf("%-6s%-" + tituloMasLargo + "s%-6s\n", i, titulo, minutos+":"+segundos);
		}
	}
	
	/* Método que solicita el listado de canciones alojadas en el servidor, para guardarlo en el
	 * atributo "canciones".
	 */
	
	@SuppressWarnings("unchecked")
	public void solicitarCanciones() 
	{
		try {
			// Hago la petición del listado de canciones
			outputPeticion.writeBytes("GET SONGLIST\r\n");
			outputPeticion.flush();

			this.canciones = (List<Song>) inputRespuesta.readObject();
			
			Comparator<Song> comparador = new Comparator<Song>(){
				@Override
				public int compare(Song s1, Song s2) {
					return s1.getTitle().compareTo(s2.getTitle());
				}
			};
			
			canciones.sort(comparador);
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	/* Método para seleccionar y reproducir una canción de las disponibles en el listado
	 * de canciones. Si no se dispone todavía del listado (es null), lo solicita y lo
	 * muestra por pantalla.
	 */
	
	public void reproducirCancion() 
	{	
		Song s = seleccionarCancion();
		try {
			// Hago la petición de streaming de la canción
			outputPeticion.writeBytes("GET SONG\r\n");
			outputPeticion.flush();
			outputPeticion.writeObject(s);
			outputPeticion.flush();
			
			SongPlayer songPlayer = new SongPlayer(socket, s);
			songPlayer.init();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}	
	
	/* Método para seleccionar una canción del listado de canciones disponibles.
	 * Si no se dispone todavía del listado (es null), lo solicita al servidor y lo
	 * muestra por pantalla. Devuelve la canción seleccionada.
	 */
	public Song seleccionarCancion() {
		
		System.out.println("Cliente> Seleccione un Id de canción:");
		if(this.canciones==null) {
			mostrarCanciones();
		}
		
		int opcion = -1;
		while(true) 
		{
			if(teclado.hasNextInt()) 
			{
				opcion = teclado.nextInt();
				if(opcion>=0 && opcion<canciones.size()) {
					break;
				} else {
					System.out.println("Cliente> Introduce un Id correcto, por favor:");
				}
			}
		}
		
		return canciones.get(opcion);
	}
}
