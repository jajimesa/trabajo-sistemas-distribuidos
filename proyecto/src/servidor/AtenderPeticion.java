package servidor;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import modelo.Song;


public class AtenderPeticion extends Thread {

	private Socket socket;
	private ObjectOutputStream outputRespuesta;
	private ObjectInputStream inputPeticion;
	
	public AtenderPeticion(Socket socket) {
		this.socket = socket;
	}
	
	
	public void run() 
	{

		try {
			// Se declaran en este orden
			this.outputRespuesta = new ObjectOutputStream(socket.getOutputStream());
			this.inputPeticion = new ObjectInputStream(socket.getInputStream());
			
			while(true) 
			{
				String peticion = inputPeticion.readLine();
				
				if(peticion.startsWith("GET")) 
				{
					atenderGET(peticion);
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
			songList();
		}
		//TODO: añadir un GET RADIO
	}
	
	
	private void streamSong() {
		try {
			Song s = (Song) inputPeticion.readObject();
			s = SongBuilder.construirCancion(s);
			
			// Le envío al cliente el sample size de la canción para que pueda reproducirla correctamente.
			AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(s.getFile());
			int sampleSize = audioInputStream.getFormat().getSampleSizeInBits();
			outputRespuesta.writeInt(sampleSize);
			outputRespuesta.flush();
			
			SongStreaming songStreaming = new SongStreaming(socket, s);
			songStreaming.init();
			
		} catch (ClassNotFoundException | IOException | UnsupportedAudioFileException e) {
			e.printStackTrace();
		}
	}
	
	private void songList() {
		
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
}
