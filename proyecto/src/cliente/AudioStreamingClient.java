package cliente;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

import javax.sound.sampled.AudioFormat;

import modelo.Song;

public class AudioStreamingClient {
	
	private Socket socket;
	private Scanner teclado;
	private ObjectOutputStream outputPeticion;
	private ObjectInputStream inputRespuesta;
	private List<Song> canciones;
	
	public void init() 
	{
		this.teclado = new Scanner(System.in);
		
		try
		{
			this.socket = new Socket("localhost", 6666);
			this.outputPeticion = new ObjectOutputStream(socket.getOutputStream());
			this.inputRespuesta = new ObjectInputStream(socket.getInputStream());
			
			menu();
		
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				outputPeticion.close();
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void menu() {
		// Muestro el menú con las opciones al cliente.
		while(true) 
		{
			System.out.println("Cliente> Seleccione una opción:");
			System.out.println("\t 1 - Lista de canciones.");
			System.out.println("\t 2 - Solicitar canción.");
			System.out.println("\t 3 - Desconectarse.");
						
			
			int opcion = 0;
			while(true) 
			{
				if(teclado.hasNextInt()) 
				{
					opcion = teclado.nextInt();
					if(opcion==1||opcion==2||opcion==3) {
						break;
					} else {
						System.out.println("Cliente> Introduce un número correcto, por favor:");
					}
				}
			}
			switch(opcion) {
				case 1:
					mostrarCanciones();
					break;
				case 2:
					reproducirCancion();
					break;
				case 3:
					// Desconexión
					break;
			}	
		}
	}
	
	public void mostrarCanciones() {
		
		if(this.canciones==null) {
			solicitarCanciones();
		}
		
		List<String> titulos = new ArrayList<String>(canciones.size());
		canciones.forEach(s->titulos.add(s.getTitle()));
		int tituloMasLargo = titulos.stream().map(String::length).max(Integer::compare).get();
		tituloMasLargo++;
		
		// Imprimo por pantalla el listado de canciones
		System.out.printf("%-6s%-" + tituloMasLargo + "s%-6s\n", "Id", "Título", "Duración");
		for(int i=0; i<canciones.size(); i++) {
			String titulo = canciones.get(i).getTitle();
			Float duracion = canciones.get(i).getDuration();
			
			int sec = (int) (duracion % 60);
			String segundos = "";
			if(sec<10) { segundos = "0" + sec;} 
			else { segundos += sec; }
			int minutos = (int) ((duracion / 60) % 60);
			
			System.out.printf("%-6s%-" + tituloMasLargo + "s%-6s\n", i, titulo, minutos+":"+segundos);
		}
	}
	
	@SuppressWarnings("unchecked")
	public void solicitarCanciones() 
	{
		try {
			// Hago la petición del listado de canciones
			outputPeticion.writeBytes("GET SONGLIST\r\n");
			outputPeticion.flush();

			this.canciones = (List<Song>) inputRespuesta.readObject();
			
			Comparator<Song> comparador = new Comparator<Song>(){

				@Override
				public int compare(Song s1, Song s2) {
					return s1.getTitle().compareTo(s2.getTitle());
				}
			};
			
			canciones.sort(comparador);
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public void reproducirCancion() 
	{	
		Song s = seleccionarCancion();
		try {
			// Hago la petición de streaming de la canción
			outputPeticion.writeBytes("GET SONG\r\n");
			outputPeticion.flush();
			outputPeticion.writeObject(s);
			outputPeticion.flush();
			
			// Reproduzco la canción
			int sampleSize = inputRespuesta.readInt();
			SongPlayer songPlayer = new SongPlayer(socket, sampleSize);
			songPlayer.init();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public Song seleccionarCancion() {
		
		System.out.println("Cliente> Seleccione un Id de canción:");
		if(this.canciones==null) {
			mostrarCanciones();
		}
		
		int opcion = -1;
		while(true) 
		{
			if(teclado.hasNextInt()) 
			{
				opcion = teclado.nextInt();
				if(opcion>=0 && opcion<canciones.size()) {
					break;
				} else {
					System.out.println("Cliente> Introduce un Id correcto, por favor:");
				}
			}
		}
		
		return canciones.get(opcion);
	}
}
