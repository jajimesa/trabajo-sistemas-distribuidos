package servidor;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.HashSet;


import modelo.Song;
import servidor.playlists.PlaylistParser;


public class AtenderPeticion extends Thread {

	private Socket socket;
	private ObjectOutputStream outputRespuesta;
	private ObjectInputStream inputPeticion;
	private PlaylistParser playlistParser;
	
	public AtenderPeticion(Socket socket) {
		this.socket = socket;
	}
	
	/* El run de AtenderPeticion se encarga de estar a la espera de peticiones del cliente,
	 * que son en formato String. Si el cliente se desconecta, cuando lee la String recibe
	 * null y de esta forma puede saber cuándo retornar.
	 */
	@Override public void run() 
	{
		try {
			// Se declaran en este orden
			this.outputRespuesta = new ObjectOutputStream(socket.getOutputStream());
			this.inputPeticion = new ObjectInputStream(socket.getInputStream());
			
			// El usuario se identifica (necesario para gestionar sus playlists)
			String idUsuario = inputPeticion.readLine();
			this.playlistParser = new PlaylistParser(idUsuario);
			
			while(true) 
			{
				String peticion = inputPeticion.readLine();
				
				if(peticion==null) {
					System.out.println("Servidor(peticiones)> Se ha desconectado el cliente " 
										+ socket.getInetAddress() + "/" + socket.getPort() + ".");
					return;
				}			
				else if(peticion.startsWith("GET")) 
				{
					atenderGET(peticion);
				}
				else if(peticion.startsWith("POST")) {
					atenderPOST(peticion);
				}
				else if(peticion.startsWith("DELETE")) {
					atenderDELETE(peticion);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/* Método que atiende una petición GET de un cliente. Comprueba el tipo de petición GET y actúa:
	 * 1) GET SONG: lee el objeto SONG que manda el cliente después de la petición y lo stremea al cliente.
	 * 2) GET SONGLIST: envía un listado con las canciones que tiene el servidor.
	 */
	private void atenderGET(String peticion) 
	{
		if(peticion.equals("GET SONG")) 
		{
			System.out.println("Servidor(peticiones)> El cliente " +
					socket.getInetAddress() + "/" + socket.getPort() + " solicita reproducir una canción.");
			streamSong();
		} 
		else if(peticion.equals("GET SONGLIST")) 
		{
			System.out.println("Servidor(peticiones)> El cliente " +
					socket.getInetAddress() + "/" + socket.getPort() + " solicita la lista de canciones.");
			sendSongList();
		}
		else if(peticion.equals("GET ALL PLAYLISTS")) 
		{
			System.out.println("Servidor(peticiones)> El cliente " +
					socket.getInetAddress() + "/" + socket.getPort() + " solicita sus listas de reproducción.");
			sendPlaylists();
		}
		else if(peticion.equals("GET PLAYLIST")) {
			System.out.println("Servidor(peticiones)> El cliente " +
					socket.getInetAddress() + "/" + socket.getPort() + " solicita reproducir una lista.");
			streamPlaylist();
		}
	}
	
	/* Método que atiende una petición POST del cliente. Comprueba el tipo de petición POST y actúa:
	 * 1) POST PLAYLIST: crea una playlist.
	 * 2) POST SONG: añade una canción a una playlist.
	 */
	private void atenderPOST(String peticion) {
		if(peticion.equals("POST PLAYLIST")) 
		{
			System.out.println("Servidor(peticiones)> El cliente " +
					socket.getInetAddress() + "/" + socket.getPort() + " solicita crear una lista de reproducción.");
			createPlaylist();
		}
		else if(peticion.equals("POST SONGS")) 
		{
			System.out.println("Servidor(peticiones)> El cliente " +
					socket.getInetAddress() + "/" + socket.getPort() + " solicita añadir canciones a una lista de reproducción.");
			addSongsToPlaylist();
		}
	}
	
	/* Método que atiende una petición DELETE del cliente. Comprueba el tipo de petición DELETE y actúa:
	 * 1) DELETE PLAYLIST: borra una playlist.
	 * 2) DELETE SONG: borra una canción de una playlist.
	 */
	private void atenderDELETE(String peticion) {
		if(peticion.equals("DELETE PLAYLIST")) 
		{
			System.out.println("Servidor(peticiones)> El cliente " +
					socket.getInetAddress() + "/" + socket.getPort() + " solicita eliminar una lista de reproducción.");
			deletePlaylist();
		}
		else if(peticion.equals("DELETE SONGS")) 
		{
			System.out.println("Servidor(peticiones)> El cliente " +
					socket.getInetAddress() + "/" + socket.getPort() + " solicita borrar canciones de una lista.");
			deleteSongsFromPlaylist();
		}
	}
	
	/* Método que recibe y retransmite la canción que el cliente quiere reproducir mediante un objeto
	 * de tipo SongStreaming.
	 */
	private void streamSong() {
		try {
			Song s = (Song) inputPeticion.readObject();
			s = SongBuilder.construirCancion(s);
				
			SongStreaming songStreaming = new SongStreaming(socket, s);
			songStreaming.init();
			
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
	}
	
	/* Método que recibe y retransmite la playlist que el cliente quiere reproducir mediante un objeto
	 * de tipo PlaylistStreaming.
	 */
	@SuppressWarnings("unchecked")
	private void streamPlaylist() 
	{
		try {
			List<Song> aux = (List<Song>) inputPeticion.readObject();
			List<Song> canciones = new ArrayList<Song>(aux.size());
			aux.forEach(s -> canciones.add(SongBuilder.construirCancion(s)));
			
			SongStreaming songStreaming = new SongStreaming(socket, canciones);
			songStreaming.init();
			
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
	}
	
	/* Método que envía al cliente la lista de canciones de las que dispone el servidor.
	 */
	private void sendSongList() {
		
		// Genero un listado de las canciones del servidor
		File directorioCanciones = new File("./src/servidor/canciones");
		File[] ficheros = directorioCanciones.listFiles();
		List<Song> canciones = new LinkedList<Song>();
		for(File f : ficheros) {
			canciones.add(new Song(f));
		}
		
		// Envío el listado al cliente
		try {
			outputRespuesta.writeObject(canciones);
			outputRespuesta.flush();
			outputRespuesta.reset();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/* Método que envía al cliente sus playlists, si las tiene.
	 */
	private void sendPlaylists() {
		
		HashMap<String, LinkedList<Song>> playlists = playlistParser.getPlaylists();
		
		// Envío las playlists al cliente
		try {
			outputRespuesta.writeObject(playlists);
			outputRespuesta.flush();
			outputRespuesta.reset();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/* Método que crea una playlist dado un nombre de playlist, si esta no existe ya
	 * para el usuario que solicita crearla.
	 */
	@SuppressWarnings("unchecked")
	public void createPlaylist() 
	{
		try {
			/* 1º Recibo el nombre de la playlist que el usuario quiere crear. Si ya tiene una playlist con ese
			 * nombre, no se lo permito.
			 */
			String name = null;
			while(true) {
				name = inputPeticion.readLine();
				if(!playlistParser.playlistExists(name)) 
				{
					outputRespuesta.writeBytes("VALID NAME\r\n");
					outputRespuesta.flush();
					outputRespuesta.flush();
					break;
				}
					outputRespuesta.writeBytes("NAME ALREADY TAKEN\r\n");
					outputRespuesta.flush();
			}
			playlistParser.addPlaylist(name);
			
			/* 2º Recibo las canciones que el usuario quiere añadir y las meto dentro de la playlist.
			 */
			List<Song> aux = (List<Song>) inputPeticion.readObject();
			
			// Tengo ahora que construir las canciones debidamente, con su file asociados
			List<Song> canciones = new ArrayList<Song>(aux.size());
			aux.forEach(s -> canciones.add(SongBuilder.construirCancion(s)));
			
			playlistParser.addAllSongs(name, canciones);
			
		} catch (IOException e) {
				e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	/* Método que añade una canción a una playlist previa selección por consola de la playlist.
	 */
	@SuppressWarnings("unchecked")
	private void addSongsToPlaylist() 
	{
		try {
			// 1º Recibo el nombre de la playlist y las canciones que quiero añadirle
			String nombrePlaylist = inputPeticion.readLine();
			List<Song> cancionesNuevas = (List<Song>) inputPeticion.readObject();
			
			// 2º Invoco al método de añadir canciones de la clase PlaylistParser
			playlistParser.addAllSongs(nombrePlaylist, cancionesNuevas);
			
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	/* Método que borra una playlist existente.
	 */
	private void deletePlaylist() 
	{
		try
		{
			// 1º Recibo el nombre de la playlist a borrar
			String nombrePlaylist = inputPeticion.readLine();
			
			// 2º Mando al parser borrar la playlist
			playlistParser.removePlaylist(nombrePlaylist);
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	/* Método que borra las canciones de una playlist seleccionadas por el usuario.
	 */
	@SuppressWarnings("unchecked")
	private void deleteSongsFromPlaylist() 
	{
		try {
			// 1º Recibo el nombre de la playlist y las canciones que quiero quitarle
			String nombrePlaylist = inputPeticion.readLine();
			HashSet<Integer> cancionesPosicionesBorrar = (HashSet<Integer>) inputPeticion.readObject();
			// Aquí para borrar no nos hace falta construir bien las canciones con SongBuilder
			
			// 2º Invoco al método de borrar canciones de la clase PlaylistParser
			playlistParser.removeAllSongs(nombrePlaylist, cancionesPosicionesBorrar);
			
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}	
	}
}
