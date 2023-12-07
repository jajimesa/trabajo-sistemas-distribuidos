package cliente;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Scanner;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class SongPlayer {

	private Socket socket;

	public SongPlayer(Socket socket) {
		this.socket = socket;
	}
	
	public void init() 
	{
		DatagramSocket udpSocket = null; // El DatagramSocket será utilizado para recibir la información
		
		/* Declaramos "final" el array para que la referencia a este objeto sea inmutable.
		 * Este array de buffer adaptativo va a recibir los bytes de los paquetes udp y va a nutrir a la
		 * línea de audio.
		 */
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		try {
			// Su puerto local será el siguiente al del socket tcp.
			udpSocket = new DatagramSocket(socket.getLocalPort() + 1);
			
			/* Obtengo una salida de audio que soporte el formato de audio.
			 * NOTA: Los .wav por lo general tienen un SampleRate de 44100Hz pero las canciones que he descargado
			 * son a 48000Hz porque son extraídas de vídeos, que por lo general siempre van a 48000Hz.
			 */
			AudioFormat audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 48000, 16, 2, 4 , 44100, false);
			
			// Obtengo y abro la SourceDataLine (conexión con la salida de audio) a partir del formato de audio.
			SourceDataLine sourceDataLine = (SourceDataLine) AudioSystem.getSourceDataLine(audioFormat);
			sourceDataLine.open(audioFormat);
					
			// Creo el receptor udp
			UdpSongReceptor udpSongReceptor = new UdpSongReceptor(udpSocket, out);

			// Creo el reproductor udp
			CyclicBarrier barrier = new CyclicBarrier(2);
			UdpSongPlayer udpSongPlayer = new UdpSongPlayer(sourceDataLine, out, barrier);
			
			// Creo un hilo con los controles (parar, reiniciar y salir)
			Thread hiloControlador = new Thread() 
			{
				@Override public void run() 
				{
					Scanner teclado = new Scanner(System.in);
					System.out.println("Cliente(Controlador)> Escribe 0 para parar, 1 para volver a empezar y 2 para salir:");
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
									System.out.println("Cliente(Controlador)> Introduce un número correcto, por favor:");
								}
							}
						}
						if(opcion==0) {
							if(sourceDataLine.isRunning()) {
								sourceDataLine.stop();
								sourceDataLine.flush();
								//out.reset();
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
							udpSongPlayer.awaitAndClose();
							break;
						}
					}			
				}
			};
			
			// Me pongo a recibir paquetes udp
			Thread threadReceptor = new Thread(udpSongReceptor);
			threadReceptor.setPriority(Thread.MAX_PRIORITY);
			threadReceptor.start();
			
			// Me pongo a reproducir los paquetes (espero un poco para que comience después del hilo receptor)
			Thread.sleep(1000);
			Thread threadPlayer = new Thread(udpSongPlayer);
			threadPlayer.setPriority(Thread.MAX_PRIORITY);
			threadPlayer.start();
			
			// Pongo en marcha el hilo controlador del reproductor.
			hiloControlador.start();
			
			// Pongo a esperar el SongPlayer hasta que el usuario decida salir.
			barrier.await();
								
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (LineUnavailableException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (BrokenBarrierException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			// Ahora cierro recursos
			if(udpSocket!=null) udpSocket.close(); // --> genera una que excepción finaliza el hilo receptor
			try {
				out.flush();
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
