package servidor;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import modelo.Song;

public class SongStreaming {

	private Socket socket;
	private Song song;
	private DatagramSocket udpSocket;
	
	// PRE: s debe de ser una canción bien construida, con un File asociado.
	public SongStreaming(Socket socket, Song s) 
	{
		this.socket = socket;
		this.song = s;
	}

	public void init() 
	{
		// El DatagramSocket será utilizado para enviar la información
		try {
			// Su puerto local será el siguiente al del socket tcp.
			this.udpSocket = new DatagramSocket(socket.getLocalPort() + 1);
			
			try {
				// Obtengo el audioInputStream del fichero de la canción solicitada.
				AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(song.getFile());
				
				AudioFormat audioFormat = audioInputStream.getFormat();
				
				// Streameo el contenido de la canción vía UDP
				byte[] b = new byte[128];
				int leido;
				while((leido = audioInputStream.read(b))!= -1) 
				{
					// El cliente tiene un socket udp escuchando en el puerto siguiente a su puerto local tcp.
					DatagramPacket packet = new DatagramPacket(b, 0, leido, socket.getInetAddress(), socket.getPort() + 1);
					udpSocket.send(packet);
				}
			} catch (UnsupportedAudioFileException | IOException e) {
				e.printStackTrace();
			}	
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}
}
