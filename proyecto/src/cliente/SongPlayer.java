package cliente;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import modelo.Song;

public class SongPlayer {

	private Socket socket;
	private Song song;
	
	private DatagramSocket udpSocket;
	
	public SongPlayer(Socket socket, Song song) {
		this.socket = socket;
		this.song = song;
	}
	
	public void init() 
	{
		// El DatagramSocket será utilizado para enviar la información
		
		try {
			// Su puerto local será el siguiente al del socket tcp.
			this.udpSocket = new DatagramSocket(socket.getLocalPort() + 1);
			
			/* Obtengo una salida de audio que soporte el formato de audio.
			 * NOTA: Los .wav por lo general tienen un SampleRate de 44100Hz pero las canciones que he descargado
			 * son a 48000Hz porque son extraídas de vídeos, que por lo general siempre van a 48000Hz.
			 */
			AudioFormat audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 48000, 16, 2, 4 , 44100, false);
			
			// Obtengo y abro la SourceDataLine (conexión con la salida de audio) a partir del formato de audio.
			SourceDataLine sourceDataLine = (SourceDataLine) AudioSystem.getSourceDataLine(audioFormat);
			sourceDataLine.open(audioFormat);
					
			// Declaramos "final" el array para que la referencia a este objeto sea inmutable.
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
					
					while(sourceDataLine.isOpen()) 
					{
						// No guardarme el array de bytes produce problemas de sincronización entre los hilos
						byte [] b = out.toByteArray();
						
						//sourceDataLine.write(out.toByteArray(), 0, out.size());
						sourceDataLine.write(b, 0, b.length); // Reproduzco lo recibido						
					}
				}
			};
			
			Thread hiloControlador = new Thread() {
				@Override public void run() 
				{
					Scanner teclado = new Scanner(System.in);
					System.out.println("Cliente(Reproductor)> Escribe 0 para parar, 1 para resumir y 2 para salir:");
					while(true) 
					{
						int opcion = -1;
						while(true) 
						{
							if(teclado.hasNextInt()) 
							{
								opcion = teclado.nextInt();
								if(opcion==0||opcion==1||opcion==2) {
									break;
								} else {
									System.out.println("Cliente(Reproductor)> Introduce un número correcto, por favor:");
								}
							}
						}
						if(opcion==0) {
							if(sourceDataLine.isRunning()) {
								sourceDataLine.flush();
								sourceDataLine.stop();
							} else {
								System.out.println("Cliente(Reproductor)> La canción ya está parada.");
							}
						}
						else if(opcion==1) {
							if(!sourceDataLine.isRunning()) {
								sourceDataLine.start();
							} else {
								System.out.println("Cliente(Reproductor)> La canción ya está en marcha.");
							}
						}
						else if(opcion==2) {
							break;
						}
					}
					if(sourceDataLine.isRunning()) sourceDataLine.stop();
					sourceDataLine.close();				
				}
			};
			
//			Timer timer= new Timer();
//			TimerTask concluirCancion = new TimerTask() {
//
//				@Override public void run() {
//					sourceDataLine.stop();
//					sourceDataLine.close();
//				}
//				
//			};
			
			threadReceptor.setPriority(Thread.MAX_PRIORITY);
			threadReceptor.start();
			Thread.sleep(1000);
			threadPlayer.setPriority(Thread.MAX_PRIORITY);
			threadPlayer.start();
			hiloControlador.start();
//			timer.schedule(concluirCancion, (long) (1000*song.getDuration())); //seg->mseg
			
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (LineUnavailableException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
