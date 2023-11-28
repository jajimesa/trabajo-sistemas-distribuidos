package cliente;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import javax.sound.sampled.SourceDataLine;

public class UdpSongPlayer implements Runnable {

	private SourceDataLine sourceDataLine;
	private ByteArrayOutputStream out;
	private CyclicBarrier barrier;

	public UdpSongPlayer(SourceDataLine sourceDataLine, ByteArrayOutputStream out, CyclicBarrier barrier) {
		this.sourceDataLine = sourceDataLine;
		this.out = out;
		this.barrier = barrier;
	}
	
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

	public void await() {
		try {
			this.barrier.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (BrokenBarrierException e) {
			e.printStackTrace();
		}
	}
}
