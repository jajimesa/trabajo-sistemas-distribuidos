package cliente;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import javax.sound.sampled.SourceDataLine;

public class UdpSongPlayer implements Runnable {

	private SourceDataLine sourceDataLine;
	private ByteArrayOutputStream out;
	private CyclicBarrier barrier;

	/* UdpSongPlayer es un Runnable que recibe una SourceDataLine para reproducir bytes, un ByteArrayOutputStream
	 * del que recibe los bytes a reproducir, y una CyclicBarrier que sirve para sincronizar el UdpSongPlayer con el
	 * SongPlayer que lo inicia.
	 * PRE: La SourceDataline se encuentra Open.
	 */
	public UdpSongPlayer(SourceDataLine sourceDataLine, ByteArrayOutputStream out, CyclicBarrier barrier) {
		this.sourceDataLine = sourceDataLine;
		this.out = out;
		this.barrier = barrier;
	}
	
	/* El método run inicia la SourceDataLine y lee bytes del ByteArrayOutputStream mientras esté abierta (el método
	 * closeAndAwait la cierra). Si la canción finaliza, por razones de la implementación del SourceDataLine, vuelve 
	 * a comenzar a leer desde cero el buffer del ArrayOutputStream (la canción se reinicia).
	 */
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
		System.out.println("Cliente(reproductor)> Reproducción finalizada.");
	}
	
	/* Método que pone a esperar a la CyclicBarrier y que cierra la SourceDataLine para finalizar
	 * la reproducción de la canción y la propia ejecución del UdpSongPlayer.
	 */
	public void awaitAndClose() {
		try {
			this.barrier.await();

			if(sourceDataLine!=null && sourceDataLine.isRunning()) {
				sourceDataLine.stop();
				sourceDataLine.flush();
				sourceDataLine.close();	
			}

		} catch (InterruptedException | BrokenBarrierException e) {
			e.printStackTrace();
		}
	}
}
