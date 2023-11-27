package principal;

import cliente.AudioStreamingClient;
import servidor.AudioStreamingServer;

public class principal {

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
