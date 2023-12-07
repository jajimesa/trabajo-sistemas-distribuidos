package cliente;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import modelo.Song;

public class AudioStreamingClient {
	
	private Socket socket;
	private Scanner teclado;
	private ObjectOutputStream outputPeticion;
	private ObjectInputStream inputRespuesta;
	
	private List<Song> canciones;
	private HashMap<String, LinkedList<Song>> playlists;
	
	public static void main(String [] args) 
	{
		AudioStreamingClient cliente = new AudioStreamingClient();
		cliente.init();
	}
	
	/* Método que inicializa el AudioStreamingClient, loggea al usuario y que invoca al método menu() 
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
			
			login();
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
	
	/* Método que loggea al usuario en el servidor para poder gestionar de forma
	 * personalizada sus listas de reproducción.
	 */
	private void login() {
		try {
			// El cliente se identifica para poder gestionar sus playlists.
			System.out.println("Cliente(login)> Introduce tu nombre de usuario, por favor:");
			String idUsuario = null;
			if(teclado.hasNextLine()) {
				idUsuario = teclado.nextLine();
			}
			outputPeticion.writeBytes(idUsuario + "\r\n");
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	/* Método que muestra por pantalla las funcionalidades disponibles del cliente y que
	 * permite seleccionarlas leyendo un entero por pantalla. Si se selecciona la opción
	 * de desconexión, el servidor sigue pendiente de recibir una petición y lee null,
	 * con lo que sabe que el cliente se ha desconectado exitosamente.
	 */
	private void menu() {
		// Muestro el menú con las opciones al cliente.
		while(true) 
		{
			System.out.println("Cliente> Seleccione una opción:");
			System.out.println("\t1 - Canciones.");
			System.out.println("\t2 - Reproducir canción.");
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
			
			// Si aún no está inicializado el atributo "canciones", las solicito al servidor
			if(this.canciones==null) {
				solicitarCanciones();
			}
			
			if(opcion==1) {
				if(this.canciones.size()==0) {
					System.out.println("Cliente> No disponemos de canciones para mostrar, lo sentimos.");
					break;
				}
				mostrarCanciones(this.canciones);
			}
			else if(opcion==2) {
				if(this.canciones.size()==0) {
					System.out.println("Cliente> No disponemos de canciones para reproducir, lo sentimos.");
					break;
				}
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
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/* Método que muestra por pantalla el listado de canciones que se le pasa por parámetro.
	 */
	private void mostrarCanciones(List<Song> canciones) {
		
		/* Utilizo "printf" para imprimir una tablita con las canciones. Para darle un formato
		 * más legible, calculo el tamaño del título de canción más largo y le sumo uno.
		 */
		List<String> titulos = new ArrayList<String>(canciones.size());
		canciones.forEach(s->titulos.add(s.getTitle()));
		int tituloMasLargo = titulos.stream().map(String::length).max(Integer::compare).get();
		tituloMasLargo++;
		
		if(tituloMasLargo<10) tituloMasLargo = 10; // Por si solo hay títulos cortitos
		
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
	private void solicitarCanciones() 
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
	
	private void reproducirCancion() 
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
	 * El listado del atributo "canciones" tiene al menos una canción.
	 * Devuelve la canción seleccionada.
	 */
	private Song seleccionarCancion()
	{
		System.out.println("Cliente> Seleccione un Id de canción:");
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
	private void menuPlaylists() {
		// Muestro el menú de las playlist al cliente
		while(true) 
		{
			System.out.println("Cliente(playlists)> Seleccione una opción:");
			System.out.println("\t1 - Ver listas de reproducción.");
			System.out.println("\t2 - Ver canciones de lista de reproducción.");
			System.out.println("\t3 - Reproducir lista.");
			System.out.println("\t4 - Crear lista.");
			System.out.println("\t5 - Borrar lista.");
			System.out.println("\t6 - Añadir canción a lista.");
			System.out.println("\t7 - Borrar canción de lista.");
			System.out.println("\t8 - Salir.");
						
			int opcion = 0;
			while(true) 
			{
				if(teclado.hasNextInt()) 
				{
					opcion = teclado.nextInt();
					if(opcion>=1 && opcion<=8) {
						break;
					} else {
						System.out.println("Cliente(playlists)> Introduce un número correcto, por favor:");
					}
				}
			}
			
			// Si aún no he inicializado el atributo playlists, lo hago:
			if(this.playlists == null) {
				solicitarPlaylists();
			}
			
			// Si aún no he inicializado el atributo "canciones", lo hago
			if(this.canciones==null) {
				solicitarCanciones();
			}
			
			// Actúo según la opción escogida
			if(opcion==1) {
				if(this.playlists.size()==0) {
					System.out.println("Cliente(playlists)> No has creado ninguna lista de reproducción aún.");
				} else {
					mostrarPlaylists();
				}
			}
			else if(opcion==2) {
				if(this.playlists.size()==0) {
					System.out.println("Cliente(playlists)> No has creado ninguna lista de reproducción aún.");
				} else {
					mostrarCancionesPlaylist();					
				}
			}
			else if(opcion==3) {
				if(this.playlists.size()==0) {
					System.out.println("Cliente(playlists)> No has creado ninguna lista de reproducción aún.");
				} else {
					reproducirPlaylist();
				}
			}
			else if(opcion==4) {
				if(this.canciones.size()==0) {
					System.out.println("Cliente(playlists)> No disponemos de ninguna canción para añadir, lo sentimos.");
				} else {
					crearPlaylist();
				}
			}
			else if(opcion==5) {
				if(this.playlists.size()==0) {
					System.out.println("Cliente(playlists)> No has creado ninguna lista de reproducción aún.");
				}
				else {
					eliminarPlaylist();
				}
			}
			else if(opcion==6) {
				if(this.playlists.size()==0) {
					System.out.println("Cliente(playlists)> No has creado ninguna lista de reproducción aún.");
				}
				else if(this.canciones.size()==0) {
					System.out.println("Cliente(playlists)> No disponemos de ninguna canción para añadir, lo sentimos.");
				}
				else {
					añadirCancionesAPlaylist();
				}
			}
			else if(opcion==7) {
				if(this.playlists.size()==0) {
					System.out.println("Cliente(playlists)> No has creado ninguna lista de reproducción aún.");
				} else {
					eliminarCancionesDePlaylist();
				}
			}
			else if(opcion==8) {
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
	
	/* Método que muestra las playlists por pantalla. Existe al menos una playlist que mostrar.
	 */
	private void mostrarPlaylists() 
	{
		List<String> titulos = new ArrayList<String>(playlists.size());
		playlists.keySet().forEach(s->titulos.add(s));
		int tituloMasLargo = titulos.stream().map(String::length).max(Integer::compare).get();
		tituloMasLargo++;
			
		if(tituloMasLargo<10) tituloMasLargo = 10; // Por si solo hay títulos cortitos
			
		// Imprimo por pantalla la tabla con el listado de playlists.
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
	private void solicitarPlaylists() 
	{
		try {
			// Hago la petición de las playlists
			outputPeticion.writeBytes("GET ALL PLAYLISTS\r\n");
			outputPeticion.flush();

			this.playlists = (HashMap<String, LinkedList<Song>>) inputRespuesta.readObject();
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	/* Método que permite seleccionar una playlist y ver su contenido de canciones.
	 * Si la playlist está vacía, no hace nada.
	 */
	private void mostrarCancionesPlaylist() 
	{
		// 1º Selecciono la canción
		List<Song> playlist = seleccionarPlaylist();
		
		// 2º Busco su título
		String nombrePlaylist = obtenerTituloPlaylist(playlist);

		if(playlist!=null && playlist.size()>0) 
		{
			System.out.println("Cliente(playlists)> \"" + nombrePlaylist + "\":");
			mostrarCanciones(playlist);
		} else {
			System.out.println("Cliente(playlists)> La lista de reproducción \"" + nombrePlaylist + "\" está vacía.");
		}
	}
	
	/* Método que devuelve el nombre de la playlist.
	 */
	private String obtenerTituloPlaylist(List<Song> playlist)
	{

		String nombrePlaylist = null;
		for(String titulo : this.playlists.keySet()) 
		{
			// No es la mejor manera, pero por cómo tengo todo implementado funciona
			if(this.playlists.get(titulo).equals(playlist)) {
				nombrePlaylist = titulo;
				break;
			}
		}
		return nombrePlaylist;
	}
	
	/* Método para reproducir una playlist una vez seleccionada.
	 * Si la playlist está vacía, no hace nada.
	 */
	private void reproducirPlaylist() 
	{
		// 1º Selecciono qué playlist reproducir
		List<Song> playlist = seleccionarPlaylist();
		
		// 2º Busco su título
		String nombrePlaylist = obtenerTituloPlaylist(playlist);
		
		if(playlist.size()==0) {
			System.out.println("Cliente(playlists)> La lista de reproducción \"" + nombrePlaylist + "\" está vacía.");
			return;
		}
		
		try {
			// Hago la petición de streaming de playlist
			outputPeticion.writeBytes("GET PLAYLIST\r\n");
			outputPeticion.flush();
			outputPeticion.writeObject(playlist);
			outputPeticion.flush();
			
			// Reproduzco la playlist
			System.out.println("Cliente(Reproductor)> Reproduciendo \"" + nombrePlaylist + "\"...");
			
			SongPlayer songPlayer = new SongPlayer(socket);
			songPlayer.init();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/* Método para seleccionar una playlist. El atributo "playlists" es no nulo y tiene tamaño mayor que cero.
	 */
	private List<Song> seleccionarPlaylist() 
	{		
		System.out.println("Cliente(playlists)> Seleccione un Id de lista de reproducción:");
		mostrarPlaylists();
		
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
			i++;
		}
		return playlist;
	}
	
	/* Método para crear una playlist, que tiene que tener un nombre único. Si se introduce un nombre
	 * que ya existe, solicita uno nuevo. Debe haber al menos una canción en el atributo "canciones".
	 */
	private void crearPlaylist() 
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
			List<Song> canciones = seleccionarCancionesPlaylist();
			
			// 3º Envio las canciones que quiero añadir a la playlist
			outputPeticion.writeObject(canciones);
			outputPeticion.flush();
			System.out.println("Cliente(playlists)> Lista de reproducción guardada.");
			
			// 4º Actualizo mi listado local con las playlist
			solicitarPlaylists();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/* Método que elimina una playlist seleccionada.
	 * Deben existir playlists en "playlists".
	 */
	private void eliminarPlaylist() 
	{
		try {
			// 1º Seleccionar la playlist y preguntar si el usuario está seguro
			int opcion = -1;
			String nombrePlaylist = null;
			while(true) 
			{
				List<Song> playlist = seleccionarPlaylist();
				nombrePlaylist = obtenerTituloPlaylist(playlist);
				System.out.println("Cliente(playlists)> Vas a borrar la lista \"" + nombrePlaylist + "\". ¿Estás seguro (1 sí/ 0 no)?");
				while(true) {
					if(teclado.hasNextInt()) {
						opcion = teclado.nextInt();
						if(opcion!=0 && opcion!=1) 
						{
							System.out.println("Cliente(playlists)> Introduce una opción correcta, por favor.");
						} else {
							break;
						}
					}	
				}
				if(opcion==1) {
					break;
				} else {
					return;
				}
			}
		
			// 2º Hacer la petición al servidor de borrar la playlist seleccionada
			outputPeticion.writeBytes("DELETE PLAYLIST\r\n");
			outputPeticion.writeBytes(nombrePlaylist + "\r\n");
			outputPeticion.flush();
			System.out.println("Cliente(playlists)> La lista de reproducción \"" + nombrePlaylist + "\" ha sido borrada.");
			
			// 3º Actualizamos nuestras playlists locales
			solicitarPlaylists();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/* Método para seleccionar canciones para añadir o borrar de una playlist. Para borrar, hay que
	 * pasar como parámetro la playlist de la que queremos borrar canciones, para añadir, pasar null.
	 * Si cancionesPlaylistBorrar es no nula, al menos tiene una canción.
	 * Para añadir, el atributo "canciones" debe tener tamaño mayor que cero.
	 */
	private List<Song> seleccionarCancionesPlaylist() 
	{
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
			
			// Si ha escrito -1, salgo
			if(opcion==-1) {
				break;
			}
			
			canciones.add(this.canciones.get(opcion));
		}
		return canciones;
	}
	
	/* Método para añadir canciones a una playlist una vez seleccionadas. El atributo "canciones" tiene
	 * al menos una canción.
	 */
	private void añadirCancionesAPlaylist() 
	{
		try {
			// 1º Hago la petición de añadir canciones a una playlist
			outputPeticion.writeBytes("POST SONGS\r\n");
			outputPeticion.flush();
						
			// 2º Selecciono una playlist y las canciones que añadir 
			System.out.println("Cliente(playlists)> Selecciona primero una lista:");
			List<Song> playlist = seleccionarPlaylist();
			String nombrePlaylist = obtenerTituloPlaylist(playlist); // el título es lo importante
			List<Song> cancionesNuevas = seleccionarCancionesPlaylist();
			
			// 3º Envío la playlist y las canciones que añadir
			outputPeticion.writeBytes(nombrePlaylist);
			outputPeticion.writeObject(cancionesNuevas);
			outputPeticion.flush();
			System.out.println("Cliente(playlists)> La lista de reproducción \"" + nombrePlaylist + "\" ha sido actualizada.");
			
			// 4º Actualizo mi listado local con las playlist
			solicitarPlaylists();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	/* Método para borrar canciones de una playlist una vez seleccionadas. 
	 * Si la playlist está vacía, no hace nada.
	 */
	private void eliminarCancionesDePlaylist() 
	{
		try {						
			// 1º Selecciono una playlist y las canciones a borrar 
			System.out.println("Cliente(playlists)> Selecciona primero una lista:");
			List<Song> playlist = seleccionarPlaylist();
			String nombrePlaylist = obtenerTituloPlaylist(playlist);
			if(playlist.size()==0) {
				System.out.println("Cliente(playlists)> La lista de reproducción \"" + nombrePlaylist + "\" está vacía.");
				return;
			}
			/* Ojo porque parte de la gracia de las playlists es repetir canciones. Queremos borrar sólo
			 * las iteraciones de la canción que haya seleccionado el usuario, no absolutamente todas.
			 * Nos interesa por tanto almacenar el integer que dice su posición en la playlist.
			 */
			HashSet<Integer> cancionesPosicionesBorrar = seleccionarCancionesBorrarPlaylist(playlist);
			
			// 2º Hago la petición de borrar canciones de una playlist
			outputPeticion.writeBytes("DELETE SONGS\r\n");
			outputPeticion.flush();
			
			// 3º Envío la playlist y las canciones que borrar
			outputPeticion.writeBytes(nombrePlaylist);
			outputPeticion.writeObject(cancionesPosicionesBorrar);
			outputPeticion.flush();
			System.out.println("Cliente(playlists)> La lista de reproducción \"" + nombrePlaylist + "\" ha sido actualizada.");
			
			// 4º Actualizo mi listado local con las playlist
			solicitarPlaylists();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	/* Método que permite seleccionar las canciones a borrar de una playlist.
	 * Ojo porque parte de la gracia de las playlists es repetir canciones. Queremos borrar sólo
	 * las iteraciones de la canción que haya seleccionado el usuario, no absolutamente todas.
	 * Nos interesa por tanto almacenar el integer que dice su posición en la playlist. Por razones
	 * de implementación devolvemos por tanto una lista con las posiciones.
	 */
	private HashSet<Integer> seleccionarCancionesBorrarPlaylist(List<Song> playlist) 
	{
		System.out.println("Cliente(playlists)> Introduce el Id de la canción que quieras borrar de la playlist. Escribe -1 para terminar.");
		mostrarCanciones(playlist);
		
		HashSet<Integer> canciones = new HashSet<Integer>();
		while(true) 
		{
			int opcion = -1;
			while(true) 
			{
				if(teclado.hasNextInt()) 
				{
					opcion = teclado.nextInt();
					if(opcion>=-1 && opcion<playlist.size()) {
						break;
					} else {
						System.out.println("Cliente(playlists)> Introduce un Id correcto, por favor:");
					}
				}
			}
			
			// Si ha escrito -1, salgo
			if(opcion==-1) {
				break;
			}
			
			// Sólo borro una vez!
			if(!canciones.contains(Integer.valueOf(opcion))) 
			{
				canciones.add(Integer.valueOf(opcion));
			} else {
				System.out.println("Cliente(playlists)> Ya has seleccionado esa canción.");
			}
		}
		return canciones;
	}
}
