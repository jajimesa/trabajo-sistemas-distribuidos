package cliente;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

//TODO: Checkear lo del sampleRate, que parece que falla si se lo paso manualmente

public class SongPlayer {

	private Socket socket;
	private int sampleSize;
	private DatagramSocket udpSocket;
	
	public SongPlayer(Socket socket, int sampleSize) {
		this.socket = socket;
		this.sampleSize = sampleSize;
	}
	
	public void init() 
	{
		// El DatagramSocket será utilizado para enviar la información
		
		try {
			// Su puerto local será el siguiente al del socket tcp.
			this.udpSocket = new DatagramSocket(socket.getLocalPort() + 1);
			
			// Obtengo una salida de audio que soporte el formato de audio.
			AudioFormat audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 48000, 16, 2, 4 , 44100, false);
			
			SourceDataLine sourceDataLine = (SourceDataLine) AudioSystem.getSourceDataLine(audioFormat);
			sourceDataLine.open(audioFormat);
			
//			Thread threadPlayer = new Thread() {
//
//				@Override public void run() 
//				{
//					sourceDataLine.start(); // Activo la lectura (grabación) de esta linea
//					
//					try {
//						while(true) 
//						{
//							DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
//							udpSocket.receive(packet);
//							byte [] b = packet.getData();
//							sourceDataLine.write(b, 0, packet.getLength()); // Reproduzco lo recibido
//						}
//					} catch (IOException e) {
//						e.printStackTrace();
//					}
//				}
//			};
			
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			
			Thread threadReceptor = new Thread() {
				
				@Override public void run() 
				{
					byte [] buffer = new byte[128];
					try {
						while(true) 
						{
							DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
							udpSocket.receive(packet);
							
							byte [] b = packet.getData();
							
							out.write(b, 0, packet.getLength()); // Escribo lo que he grabado
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			};
				
			Thread threadPlayer = new Thread() {

				@Override public void run() 
				{
					sourceDataLine.start(); // Activo la lectura (grabación) de esta linea
					
					while(true) 
					{
						byte [] b = out.toByteArray();
						sourceDataLine.write(b, 0, b.length); // Reproduzco lo recibido
						
						//System.out.println(out.toByteArray().length +" "+out.size());
						//sourceDataLine.write(out.toByteArray(), 0, out.size()); // Reproduzco lo recibido
					}
				}
			};
			
			threadReceptor.setPriority(Thread.MAX_PRIORITY);
			threadReceptor.start();
			Thread.sleep(1000);
			threadPlayer.setPriority(Thread.MAX_PRIORITY);
			threadPlayer.start();	
			
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (LineUnavailableException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
