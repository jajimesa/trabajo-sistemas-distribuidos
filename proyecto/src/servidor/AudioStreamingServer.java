package servidor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AudioStreamingServer {
	
	public static void main(String[] args) 
	{
		init();
	}
	
	public static void init() 
	{
		ExecutorService pool = Executors.newCachedThreadPool();
		
		try(ServerSocket server = new ServerSocket(6666)) {
			
			// TODO: Iniciar hilo con la radio.
			
			while(true) 
			{
				try 
				{
					Socket cliente = server.accept();
					AtenderPeticion atenderPeticion = new AtenderPeticion(cliente);
					pool.execute(atenderPeticion);
					
				} catch(IOException e) {
					e.printStackTrace();
				}
			}
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
	}
}
