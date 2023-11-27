package prueba_java_sound_api;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.UnsupportedAudioFileException;

public class PruebaClip {
	
	public static Mixer mixer; // Componente de más alto nivel de la api javax.sound.sampled
	public static Clip clip; // Un clip en memoria el fichero al completo y tiene que conocer su duración
	
	/* Clase que toma Fly Me to The Moon, la carga en memoria RAM
	 * y la reproduce a través del sistema de audio del pc.
	 */
	public static void main(String[] args) throws InterruptedException {
		
		/* 1. Obtenemos todos los "puertos" de salida de audio de nuestro sistema.
		 * La clase AudioSystem sirve como puente a los sistemas de audio de nuestro dispositivo.
		 */
		Mixer.Info[] mixInfos = AudioSystem.getMixerInfo();
		for(Mixer.Info info: mixInfos) {
			System.out.println(info);
		}
		
		// 2. Escojo para el mixer el primer puerto, que se corresponde con mis altavoces (salta excepción) 
		// https://stackoverflow.com/questions/55680752/why-does-attempting-to-get-a-clip-throw-exception
//		mixer = AudioSystem.getMixer(mixInfos[0]);
//		
		/* 3. Obtenemos la información de Clip, que es subclase de DataLine, junto a SourceDataLine
		 *  y TargetDataLine.
		 */
//		DataLine.Info dataInfo = new DataLine.Info(Clip.class, null);
		
//		// 4. Construyo el clip.
//		try {
//			clip = (Clip) mixer.getLine(dataInfo); // Nos devuelve una línea, hay que castearla
//		} catch(LineUnavailableException e) {
//			e.printStackTrace();
//		}
		
		// 5. Reproduczo un fichero de audio como prueba
		try {
			// Se puede con una URL en lugar de con un fichero
			 File file = new File("./src/prueba_java_sound_api/Fly Me To The Moon (2008 Remastered).wav");
             AudioInputStream audioStream = AudioSystem.getAudioInputStream(file);
             DataLine.Info info = new DataLine.Info(Clip.class, audioStream.getFormat());
             clip = (Clip) AudioSystem.getLine(info);
             clip.open(audioStream);
		} catch(LineUnavailableException e) {
			e.printStackTrace();
		} catch(UnsupportedAudioFileException e) {
			e.printStackTrace();
		} catch(IOException e) {
			e.printStackTrace();
		}
		
		// Ahora lo reproduzco (se reproduce en un hilo a parte).
		clip.start();
		do {
			Thread.sleep(50); // Lo justo para que pueda ponerse en Activo
		} while(clip.isActive());
		//clip.close();
	}
}
