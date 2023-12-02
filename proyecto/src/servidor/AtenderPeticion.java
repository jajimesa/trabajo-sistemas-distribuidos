package servidor;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


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
					System.out.println("Servidor> Se ha desconectado el cliente " 
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
			streamSong();
		} 
		else if(peticion.equals("GET SONGLIST")) 
		{
			sendSongList();
		}
		else if(peticion.equals("GET PLAYLISTS")) 
		{
			sendPlaylists();
		}
		else if(peticion.equals("GET PLAYLIST")) {
			streamPlaylist();
		}
		//TODO: añadir un GET RADIO
	}
	
	/* Método que atiende una petición POST del cliente. Comprueba el tipo de petición POST y actúa:
	 * 1) POST PLAYLIST: crea una playlist.
	 */
	private void atenderPOST(String peticion) {
		if(peticion.equals("POST PLAYLIST")) 
		{
			createPlaylist();
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
	private void streamPlaylist() {
		
		//TODO terminar
		
		
		
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
			List<Song> canciones = (List<Song>) inputPeticion.readObject();
			playlistParser.addAllSongs(name, canciones);
			
		} catch (IOException e) {
				e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
}
