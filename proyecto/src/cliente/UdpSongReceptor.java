package cliente;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class UdpSongReceptor implements Runnable {

	private DatagramSocket udpSocket;
	private ByteArrayOutputStream out;
	
	public UdpSongReceptor(DatagramSocket udpSocket, ByteArrayOutputStream out) {
		this.udpSocket = udpSocket;
		this.out = out;
	}
	
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
			System.out.println("Cliente(receptor)> Stream finalizado");
		}
	}
}
