package cliente;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
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
	private HashMap<String, LinkedList<Song>> playlists;
	
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
			
			// El cliente se identifica para poder gestionar sus playlists.
			System.out.println("Cliente(login)> Introduce tu nombre de usuario, por favor:");
			String idUsuario = null;
			if(teclado.hasNextLine()) {
				idUsuario = teclado.nextLine();
			}
			outputPeticion.writeBytes(idUsuario + "\r\n");
			
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
			System.out.println("\t1 - Lista de canciones.");
			System.out.println("\t2 - Solicitar canción.");
			System.out.println("\t3 - Listas de reproducción.");
			System.out.println("\t4 - Desconectarse.");
						
			int opcion = 0;
			while(true) 
			{
				if(teclado.hasNextInt()) 
				{
					opcion = teclado.nextInt();
					if(opcion==1||opcion==2||opcion==3||opcion==4) {
						break;
					} else {
						System.out.println("Cliente> Introduce un número correcto, por favor:");
					}
				}
			}
			if(opcion==1) {
				// Si aún no se dispone del listado, solicítalo.
				if(this.canciones==null) {
					solicitarCanciones();
				}
				mostrarCanciones(this.canciones);
			}
			else if(opcion==2) {
				reproducirCancion();
			}
			else if(opcion==3) {
				menuPlaylists();
			}
			else if (opcion==4) {
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


	/* Método que muestra por pantalla el listado de canciones que se le pasa por parámetro.
	 */
	public void mostrarCanciones(List<Song> canciones) {
		
		/* Utilizo "printf" para imprimir una tablita con las canciones. Para darle un formato
		 * más legible, calculo el tamaño del título de canción más largo y le sumo uno.
		 */
		List<String> titulos = new ArrayList<String>(canciones.size());
		canciones.forEach(s->titulos.add(s.getTitle()));
		int tituloMasLargo = titulos.stream().map(String::length).max(Integer::compare).get();
		tituloMasLargo++;
		
		// Imprimo por pantalla la tabla con el listado de canciones.
		System.out.printf("\t%-6s%-" + tituloMasLargo + "s%-6s\n", "Id", "Título", "Duración");
		for(int i=0; i<canciones.size(); i++) {
			String titulo = canciones.get(i).getTitle();
			Float duracion = canciones.get(i).getDuration();
			
			// Hago una transformación de la duración (seg) al formato min:seg.
			int sec = (int) (duracion % 60);
			String segundos = "";
			if(sec<10) { segundos = "0" + sec;} 
			else { segundos += sec; }
			int minutos = (int) ((duracion / 60) % 60);
			
			System.out.printf("\t%-6s%-" + tituloMasLargo + "s%-6s\n", i, titulo, minutos+":"+segundos);
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
			
			SongPlayer songPlayer = new SongPlayer(socket);
			System.out.println("Cliente> Reproduciendo \"" + s.getTitle() + "\"...");
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
			solicitarCanciones();
		}
		mostrarCanciones(this.canciones);
		
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
	
	/* Método que muestra por pantalla el gestor de playlists. El usuario puede solicitar ver sus
	 * playlists, solicitar reproducir una o crear una playlist.
	 */
	public void menuPlaylists() {
		// Muestro el menú de las playlist al cliente
		while(true) 
		{
			System.out.println("Cliente(playlists)> Seleccione una opción:");
			System.out.println("\t1 - Ver listas de reproducción.");
			System.out.println("\t2 - Ver canciones de lista de reproducción.");
			System.out.println("\t3 - Reproducir lista.");
			System.out.println("\t4 - Crear lista.");
			System.out.println("\t5 - Salir.");
						
			int opcion = 0;
			while(true) 
			{
				if(teclado.hasNextInt()) 
				{
					opcion = teclado.nextInt();
					if(opcion>=1 && opcion<=5) {
						break;
					} else {
						System.out.println("Cliente(playlists)> Introduce un número correcto, por favor:");
					}
				}
			}
			
			if(opcion==1) {
				mostrarPlaylists();
			}
			else if(opcion==2) {
				mostrarCancionesPlaylist();
			}
			else if(opcion==3) {
				reproducirPlaylist();
			}
			else if(opcion==4) {
				if(this.canciones==null) {
					solicitarCanciones();
				}
				crearPlaylist();
			}
			else if(opcion==5) {
				break;
			}
			
			// Le doy unos segundos de cancha antes de mostrar de nuevo el menú.
			try {
				Thread.sleep(1500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	/* Método que muestra las playlists por pantalla. Si no se dispone de ellas todavía,
	 * las solicita al servidor.
	 */
	public void mostrarPlaylists() 
	{
		// Si aún no se dispone del las playlists, las solicitamos.
		if(this.playlists==null) {
			solicitarPlaylists();
		}
		
		List<String> titulos = new ArrayList<String>(playlists.size());
		playlists.keySet().forEach(s->titulos.add(s));
		int tituloMasLargo = titulos.stream().map(String::length).max(Integer::compare).get();
		tituloMasLargo++;
		
		// Imprimo por pantalla la tabla con el listado de canciones.
		System.out.printf("\t%-6s%-" + tituloMasLargo + "s%-6s\n", "Id", "Título", "Canciones");
		int i=0;
		for(String titulo : titulos) 
		{
			int tam = playlists.get(titulo).size();
			System.out.printf("\t%-6s%-" + tituloMasLargo + "s%-6s\n", i, titulo, tam);
			i++;
		}
	}
	
	/* Método que solicita las playlists al servidor, guardándolas en el atributo 
	 * "playlists" de la clase.
	 */
	@SuppressWarnings("unchecked")
	public void solicitarPlaylists() 
	{
		try {
			// Hago la petición de las playlists
			outputPeticion.writeBytes("GET PLAYLISTS\r\n");
			outputPeticion.flush();

			this.playlists = (HashMap<String, LinkedList<Song>>) inputRespuesta.readObject();
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	/* Método que permite seleccionar una playlist y ver su contenido de canciones.
	 */
	public void mostrarCancionesPlaylist() 
	{
		System.out.println("Cliente(playlists)> Seleccione un Id de playlist:");
		if(this.canciones==null) {
			solicitarCanciones();
		}
		mostrarCanciones(this.canciones);
		
		int opcion = -1;
		while(true) 
		{
			if(teclado.hasNextInt()) 
			{
				opcion = teclado.nextInt();
				if(opcion>=0 && opcion<playlists.size()) {
					break;
				} else {
					System.out.println("Cliente(playlists)> Introduce un Id correcto, por favor:");
				}
			}
		}
		
		int i=0;
		String nombrePlaylist = null;
		for(String titulo : playlists.keySet()) {
			if(i==opcion) {
				nombrePlaylist = titulo;
			}
		}
				
		mostrarCanciones(playlists.get(nombrePlaylist));
	}
	
	/* Método para reproducir una playlist una vez seleccionada.
	 */
	public void reproducirPlaylist() 
	{
		List<Song> playlist = seleccionarPlaylist();
		try {
			// Hago la petición de streaming de playlist
			outputPeticion.writeBytes("GET PLAYLIST\r\n");
			outputPeticion.flush();
			outputPeticion.writeObject(playlist);
			outputPeticion.flush();
			
			// Reproduzco la playlist
			SongPlayer songPlayer = new SongPlayer(socket);
			songPlayer.init();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/* Método para seleccionar una playlist.
	 */
	public List<Song> seleccionarPlaylist() {
		System.out.println("Cliente(playlists)> Seleccione un Id de canción:");
		
		if(this.playlists==null) {
			solicitarPlaylists();
			mostrarPlaylists();
		}
		
		int opcion = -1;
		while(true) 
		{
			if(teclado.hasNextInt()) 
			{
				opcion = teclado.nextInt();
				if(opcion>=0 && opcion<playlists.size()) {
					break;
				} else {
					System.out.println("Cliente(playlists)> Introduce un Id correcto, por favor:");
				}
			}
		}
		int i=0;
		List<Song> playlist = null;
		for(String s : playlists.keySet()) {
			if(i==opcion) {
				playlist = playlists.get(s);
				break;
			}
		}
		return playlist;
	}
	
	public void crearPlaylist() 
	{
		try {
			// Hago la petición de crear una playlist
			outputPeticion.writeBytes("POST PLAYLIST\r\n");
			outputPeticion.flush();
			
			// 1º Pido al usuario que introduzca un nombre a la playlist
			System.out.println("Cliente(playlists)> Da un nombre a la nueva playlist:");
			String nombre = null;
			
			teclado.nextLine(); // Limpio el buffer del scanner
			while(true) {
				if(teclado.hasNextLine()) {
					nombre = teclado.nextLine();
				}
				outputPeticion.writeBytes(nombre + "\r\n");
				outputPeticion.flush();
				outputPeticion.reset();
				String respuesta = inputRespuesta.readLine();
				if(respuesta.equals("NAME ALREADY TAKEN")) {
					System.out.println("Cliente(playlists)> Ya existe una playlist con ese nombre, prueba uno nuevo.");
				}
				else if(respuesta.equals("VALID NAME")) {
					System.out.println("Cliente(playlists)> Nueva playlist llamada \"" + nombre + "\" creada.");
					break;
				}
			}
			
			// 2º Pido al usuario que introduzca canciones en la playlist
			System.out.println("Cliente(playlists)> Introduce el Id de la canción que quieras añadir a la playlist. Escribe -1 para terminar.");
			mostrarCanciones(this.canciones);
			
			List<Song> canciones = new LinkedList<Song>();
			while(true) 
			{
				int opcion = -1;
				while(true) 
				{
					if(teclado.hasNextInt()) 
					{
						opcion = teclado.nextInt();
						if(opcion>=-1 && opcion<this.canciones.size()) {
							break;
						} else {
							System.out.println("Cliente(playlists)> Introduce un Id correcto, por favor:");
						}
					}
				}
				if(opcion==-1) {
					break;
				}
				
				canciones.add(this.canciones.get(opcion));
			}
			
			// 3º Envio las canciones que quiero añadir a la playlist
			outputPeticion.writeObject(canciones);
			outputPeticion.flush();
			System.out.println("Cliente(playlists)> Playlist guardada.");
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
