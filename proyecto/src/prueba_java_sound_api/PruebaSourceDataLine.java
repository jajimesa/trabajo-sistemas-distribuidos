package prueba_java_sound_api;

import java.io.*;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

public class PruebaSourceDataLine {

	public static void main(String [] args) throws InterruptedException
	{
		try {
			// Obtengo una Line.Info con la información de la clase TargetDataLine
	        Line.Info targetDLInfo = new Line.Info(TargetDataLine.class);
	        
	        // Obtengo la información de todos los mixers del AudioSystem de Java
	        Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();
	        Mixer currentMixer = null;
		   
	        // 1º Monto el programita que graba mi voz (TargetDataLine)
	        
	        // Recorro estas informaciones buscando un mixer que sea TargetDataLine (linea de entrada)
		    for(int i = 0; i < mixerInfo.length; i++) {
	
		        // Voy obteniendo cada mixer de uno en uno
		        currentMixer = AudioSystem.getMixer(mixerInfo[i]);
		        
		        // Podría usar esto para que el usuario seleccione su dispositivo de entrada
		        if(currentMixer.isLineSupported(targetDLInfo)) {
		        	System.out.println(mixerInfo[i]);
		        }
		        
		        // En mi caso solo tengo uno, así que me lo quedo
		        if(currentMixer.isLineSupported(targetDLInfo)) {
		        // Tengo
		        	break;
		        }
	        }
	        
		    final ByteArrayOutputStream out = new ByteArrayOutputStream(); // Es como un buffer de tamaño variable
		    
	        TargetDataLine targetDataLine = (TargetDataLine) currentMixer.getLine(targetDLInfo);
	        targetDataLine.open(); // Obtengo los recursos del sistema para grabar
	        
	        Thread hiloGrabador = new Thread() {
				
				@Override public void run() 
				{
					targetDataLine.start(); // Activo la lectura (grabación) de esta linea
					
					byte[] b = new byte[targetDataLine.getBufferSize()/5];
					int leido;
					
					while(true) 
					{
						leido = targetDataLine.read(b, 0, b.length); // Leo del micrófono
						out.write(b, 0, leido); // Escribo lo que he grabado
					}
				}
			};
	        
			// 2º Monto el programita que reproduce mi voz (SourceDataLine)
			AudioFormat formatoWAV = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2, 4 , 44100, false);
			//AudioFormat audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100.0F, 16, 2, 4, 44100.0F, false);
			
			//AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File("./src/prueba_java_sound_api/grabacion.wav"));
			//AudioFormat audioFormat = audioInputStream.getFormat();
			//DataLine.Info info = new Info(SourceDataLine.class, audioFormat);
			//SourceDataLine sourceDataLine = (SourceDataLine) AudioSystem.getLine(info);
			
			
			//Line.Info sourceDLInfo = new Line.Info(SourceDataLine.class, audioFormat);
	        // Recorro estas informaciones buscando un mixer que sea SourceDataLine (linea de entrada)
//		    for(int i = 0; i < mixerInfo.length; i++) {
//	
//		        // Voy obteniendo cada mixer de uno en uno
//		        currentMixer = AudioSystem.getMixer(mixerInfo[i]);
//		        
//		        // Podría usar esto para que el usuario seleccione su dispositivo de salida
//		        if(currentMixer.isLineSupported(sourceDLInfo)) {
//		        	System.out.println(mixerInfo[i]);
//		        }
//		        
//		        // En mi caso solo tengo uno, así que me lo quedo
//		        if(currentMixer.isLineSupported(sourceDLInfo)) {
//		        // Tengo
//		        	break;
//		        }
//	        }
			//SourceDataLine sourceDataLine = (SourceDataLine) currentMixer.getLine(sourceDLInfo);
		    
			
			SourceDataLine sourceDataLine = (SourceDataLine) AudioSystem.getSourceDataLine(formatoWAV);
			sourceDataLine.open(formatoWAV);
			Thread hiloReproductor = new Thread() {

				@Override public void run() 
				{
					sourceDataLine.start(); // Activo la lectura (grabación) de esta linea
					
					while(true) 
					{
						sourceDataLine.write(out.toByteArray(), 0, out.size()); // Reproduzco lo recibido
					}
				}
			};
			
			// 3º Grabo mi voz y después la reproduczo.
			
			hiloGrabador.start();
			System.out.println("La grabación ha comenzado...");
			Thread.sleep(5000);
			targetDataLine.stop();
			System.out.println("La grabación ha terminado.");
			targetDataLine.close();
			
			hiloReproductor.start();
			System.out.println("El playback ha comenzado...");
			Thread.sleep(5000);
			sourceDataLine.stop();
			System.out.println("El playback ha terminado");
			sourceDataLine.close();
			
			
		} catch (LineUnavailableException e) {
			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		} catch (UnsupportedAudioFileException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
		}	
	}
}
