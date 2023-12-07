package principal;

import cliente.AudioStreamingClient;
import servidor.AudioStreamingServer;

public class principal {

	/* Para una prueba en local sencilla se puede utilizar esta
	 * clase. El servidor va a su bola en un hilo y el cliente se
	 * inicializa en el hilo principal.
	 */
	public static void main(String[] args) 
	{
		Thread server = new Thread() {
			public void run() {
				AudioStreamingServer.init();
			}
		};
		server.start();
		
		AudioStreamingClient cliente = new AudioStreamingClient();
		cliente.init();
	}
}
