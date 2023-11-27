package prueba_java_sound_api;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFileFormat;
//import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
//import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
//import javax.sound.sampled.Port;
import javax.sound.sampled.TargetDataLine;

public class PruebaTargetDataLine {
	
	/* Clase que graba a partir del micrófono cinco segundos de prueba
	 * y lo almacena como un fichero.wav llamado grabacion.wav.
	 */
	public static void main(String [] args) throws InterruptedException 
	{
		System.out.println("prueba_java_sound_api de sonido...");
		
		try {
			/* El formato WAV tiene las siguientes características:
			 * Encoding = PMC_SIGNED
			 * Sample Rate = 44100
			 * Bits/sample = 16 bits
			 * Channels = 2 (stereo)
			 * Frame size = 4 bytes
			 * Framerate = 44100
			 * Little Endian
			 */
//			//No funciona
//			AudioFormat formatoWAV = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2, 4 , 44100, false);
//			
//			DataLine.Info info = new DataLine.Info(DataLine.class, formatoWAV);
//			if (!AudioSystem.isLineSupported(info)) {
//				System.out.println("Linea no soportada");
//			}
//			TargetDataLine targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
//			TargetDataLine targetDataLine = (TargetDataLine) AudioSystem.getLine(Port.Info.MICROPHONE); // No encuentra el micrófono así tampoco
			
			//Enumerates all available microphones
	        Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
	        for (Mixer.Info info: mixerInfos)
	        {
	            Mixer m = AudioSystem.getMixer(info);
	            Line.Info[] lineInfos = m.getTargetLineInfo();
	            if(lineInfos.length>=1 && lineInfos[0].getLineClass().equals(TargetDataLine.class))
	            {//Only prints out info is it is a Microphone
	                System.out.println("Line Name: " + info.getName());//The name of the AudioDevice
	                System.out.println("Line Description: " + info.getDescription());//The type of audio device
	                for (Line.Info lineInfo:lineInfos){
	                    System.out.println ("\t"+"---"+lineInfo);
	                    Line line;
	                    try {
	                        line = m.getLine(lineInfo);
	                    } catch (LineUnavailableException e) {
	                        // TODO Auto-generated catch block
	                        e.printStackTrace();
	                        return;
	                    }
	                    System.out.println("\t-----"+line);
	                }
	            }
	        }
//	        TargetDataLine targetDataLine = (TargetDataLine) AudioSystem.getLine(mixerInfos[1]); // no está bien
				 
	        
	        // FUNCIONA!!!!!!!!!!!!!!!
	        // https://www.technogumbo.com/tutorials/Java-Microphone-Selection-And-Level-Monitoring/Java-Microphone-Selection-And-Level-Monitoring.html
	        
	        /* El vídeo de TargetDataLine no funciona porque a él le coge directamente el micrófono con tan solo poner ese AudioFormat.
	         * Tampoco sirve obtener el micrófono por defecto, seguramente por estar utilizando la Focusrite.
	         * En este caso, selecciono el micrófono que sé que es a través del mixer!!
	         */
	        	        
	        // Setup a Line.Info instance specifically of the TargetDataLine class.
	        Line.Info targetDLInfo = new Line.Info(TargetDataLine.class);

	        // Get all the mixers from the Java AudioSystem
	        Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();
	        Mixer currentMixer = null;
		        // Iterate through each mixer and see if it supports TargetDataLine
		    for(int i = 0; i < mixerInfo.length; i++) {
	
		        // Get a temporary instance of the current mixer
		        currentMixer = AudioSystem.getMixer(mixerInfo[i]);
	
		        if(currentMixer.isLineSupported(targetDLInfo)) {
		        // This mixer supports recording 
		        	break;
		        }
	        }
	        
	        TargetDataLine targetDataLine = (TargetDataLine) currentMixer.getLine(targetDLInfo);
	        
			targetDataLine.open();
			targetDataLine.start();
			
			Thread hilo = new Thread() {
				
				public void run() {
					AudioInputStream audioInputStream = new AudioInputStream(targetDataLine);
					File grabacion = new File("./src/prueba_java_sound_api/grabacion.wav");
					try {
						AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, grabacion);  // bloqueante
					} catch (IOException e) {
						e.printStackTrace();
					}
					System.out.println("Grabación finalizada.");
				}
			};
			System.out.println("Comienza la grabación...");
			hilo.start();
			
			Thread.sleep(5000);
			
			targetDataLine.stop();
			targetDataLine.close();
			
			System.out.println("prueba_java_sound_api de sonido terminada.");
		} catch (LineUnavailableException e) {
			e.printStackTrace();
		}
	}
}
