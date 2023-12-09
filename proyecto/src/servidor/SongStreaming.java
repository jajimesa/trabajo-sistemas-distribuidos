package servidor;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import modelo.Song;

public class SongStreaming {

	private Socket socket;
	private List<Song> songs;
	private DatagramSocket udpSocket;
	
	// PRE: s debe de ser una canción bien construida, con un File asociado.
	public SongStreaming(Socket socket, Song s) 
	{
		this.socket = socket;
		this.songs = new ArrayList<Song>(1);
		this.songs.add(s);
	}
	
	// PRE: songs debe ser una playlist de canciones bien construida, cada canción con su File asociado.
	public SongStreaming(Socket socket, List<Song> songs) 
	{
		this.socket = socket;
		this.songs = songs;
	}

	public void init() 
	{
		// El DatagramSocket será utilizado para enviar la información
		try {
			// Su puerto local será el siguiente al del socket tcp.
			this.udpSocket = new DatagramSocket(socket.getLocalPort() + 1);

			try {
				System.out.println("Servidor(streaming)> Comienza el streaming al cliente " +
						socket.getInetAddress() + "/" + socket.getPort() + ".");
				for(Song song: songs) {
					// Obtengo el audioInputStream del fichero de la canción solicitada.
					AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(song.getFile());
					
					// Streameo el contenido de la canción vía UDP
					byte[] b = new byte[128];
					int leido;
					while((leido = audioInputStream.read(b))!= -1) 
					{
						// El cliente tiene un socket udp escuchando en el puerto siguiente a su puerto local tcp.
						DatagramPacket packet = new DatagramPacket(b, 0, leido, socket.getInetAddress(), socket.getPort() + 1);
						udpSocket.send(packet);
					}
				}
				System.out.println("Servidor(streaming)> El envío de paquetes udp al cliente " +
									socket.getInetAddress() + "/" + socket.getPort() + " ha finalizado.");	
		
			} catch (UnsupportedAudioFileException | IOException e) {
				e.printStackTrace();
			}
		} catch (SocketException e) {
			e.printStackTrace();
		} 
		finally {
			// Ahora cierro recursos
			if(udpSocket!=null) udpSocket.close(); 	
		}
	}
}
