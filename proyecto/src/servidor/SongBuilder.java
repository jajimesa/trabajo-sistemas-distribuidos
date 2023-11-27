package servidor;

import java.io.File;

import modelo.Song;

/* Clase SongBuilder: el servidor construye canciones válidas a partir de las canciones que envía el cliente
 * en forma de petición GET. Estas canciones llegan con tan solo el título inicializado.
 */
public class SongBuilder {
	
	/* PRE: s es una canción construida a partir de su título, sin File asociado y sin duración calculada.
	 * 		s es una canción que existe en el servidor.
	 * POST: se busca el File asociado a s y devuelve una canción con el titulo de s y su File y duración inicializados.
	 */
	public static Song construirCancion(Song s) {
		
		return new Song(new File("./src/servidor/canciones/" + s.getTitle() + ".wav"));
	}
}
