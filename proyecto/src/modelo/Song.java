package modelo;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public class Song implements Serializable {
	
	/* Clase Song: representa una canción. Desde el lado del servidor, una canción se
	 * construye a partir del File que representa el archivo .wav de la canción. A partir de
	 * este File, obtiene el título y duración de la canción. Esta clase tiene representación
	 * en forma de XML y puede serializarse. El cliente construye canciones a partir del
	 * título, que sí lo conoce.
	 */
	
	private static final long serialVersionUID = 1L;
	
	private File file; // Debe ser .wav
	private String title; // Sin el formato
	
	private float duration; // En segundos
	
	public Song(File f) {
		this.file = f;
		this.title = f.getName().substring(0,f.getName().indexOf(".wav"));
		this.duration = calcularDuracion(file);
	}
	
	public Song(String title) {
		this.file = null;
		this.title = title;
		this.duration = -1.0f;
	}
	
	private float calcularDuracion(File file) {
		AudioInputStream audioInputStream = null;
		try {
			audioInputStream = AudioSystem.getAudioInputStream(file);
		} catch (UnsupportedAudioFileException | IOException e) {
			e.printStackTrace();
		}
		AudioFormat format = audioInputStream.getFormat();
		long audioFileLength = file.length();
		int frameSize = format.getFrameSize();
		float frameRate = format.getFrameRate();
		return (audioFileLength / (frameSize * frameRate));
	}
	
	public String getTitle() {
		return this.title;
	}

	public float getDuration() {
		return this.duration;
	}
	
	public File getFile() {
		return this.file;
	}
}
