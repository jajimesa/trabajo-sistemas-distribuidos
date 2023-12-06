package servidor.playlists;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import modelo.Song;

public class PlaylistParser {

	private static File baseDeDatos = new File("./src/servidor/playlists/playlists.xml");
	
	private DocumentBuilderFactory dbf;
	private DocumentBuilder db;
	private Document dom;
	private String idUsuario;
	
	public PlaylistParser(String idUsuario) {
		this.idUsuario = idUsuario;
		try {
			this.dbf = DocumentBuilderFactory.newInstance();
			this.db = dbf.newDocumentBuilder();
			this.dom = db.parse(baseDeDatos);

		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/* PRE: El usuario ya ha sido dado de alta.
	 * Método que devuelve cierto si existe una playlist del usuario identificado por
	 * "idUsuario" con el nombre de playlist "nombrePlaylist".
	 */
	public boolean playlistExists(String namePlaylist) 
	{
		Element usuario = (Element) dom.getElementById(idUsuario);
		
		if(usuario==null) {
			return false;
		}
		
		NodeList listaPlaylists = usuario.getElementsByTagName("playlist");
		int n = listaPlaylists.getLength();
		for(int i=0; i<n; i++) 
		{
			Element playlist = (Element) listaPlaylists.item(i);
			String playlistName = playlist.getAttribute("playlistName");
			if (playlistName.equals(namePlaylist)) {
				return true;
			}
		}
		return false;
	}
	
	/* PRE: El usuario no está dado de alta.
	 * Método que da de alta al usuario. Se invoca desde otros métodos con control de
	 * la sincronización.
	 */
	public void addUsuario() 
	{
		Element usuario = dom.createElement("user");
		usuario.setAttribute("identificador", idUsuario);
		usuario.setIdAttribute("identificador", true);
		usuario.setTextContent("");
		Element root = dom.getDocumentElement();
		root.appendChild(usuario);
		saveContext();
	}
	
	/* PRE: No existe ya una playlist con nombre "namePlaylist".
	 * Método que crea una playlist con nombre "namePlaylist". 
	 */
	public void addPlaylist(String namePlaylist) 
	{
		synchronized(baseDeDatos) 
		{
			// 1º Actualizo el dom por si otro usuario ha modificado el xml
			try {
				this.dom = db.parse(baseDeDatos);
			} catch (SAXException | IOException e) {
				e.printStackTrace();
			}
			
			// 2º Busco el usuario, si no existe lo creo
			Element usuario = (Element) dom.getElementById(idUsuario);
			if(usuario==null) {
				addUsuario();
			}
			usuario = (Element) dom.getElementById(idUsuario);

			// 3º Añado la playlist y guardo los cambios
			Element playlist = dom.createElement("playlist");
			playlist.setAttribute("playlistName", namePlaylist);
			usuario.appendChild(playlist);
			saveContext();
		}
	}
	
	/* PRE: La playlist a eliminar existe.
	 * Método que borra la playlist de nombre "nombrePlaylist".
	 */
	public void removePlaylist(String namePlaylist) 
	{
		synchronized(baseDeDatos) 
		{
			// 1º Actualizo el dom por si otro usuario ha modificado el xml
			try {
				this.dom = db.parse(baseDeDatos);
			} catch (SAXException | IOException e) {
				e.printStackTrace();
			}
			
			// 2º Busco el usuario (si existe)
			Element usuario = (Element) dom.getElementById(idUsuario);
			
			// 3º Busco, elimino la playlist y guardo los cambios
			NodeList playlists = usuario.getElementsByTagName("playlist");			
			int tam = playlists.getLength();
			Element playlist = null;
			for(int i=0; i<tam; i++) 
			{
				playlist = (Element) playlists.item(i);
				if(playlist.getAttribute("playlistName").equals(namePlaylist)) 
				{
					// Si no tomo su nodo padre y tomo directamente usuario salta excepción
					playlist.getParentNode().removeChild(playlist);
					break;
				}
			}
			
			// 4º Guardo los cambios
			saveContext();
		}
	}
	
	/* Método que añade canciones a una playlist existente de nombre "namePlaylist".
	 */
	public void addAllSongs(String namePlaylist, List<Song> songs) 
	{
		synchronized(baseDeDatos) 
		{
			// 1º Actualizo el dom por si otro usuario ha modificado el xml
			try {
				this.dom = db.parse(baseDeDatos);
			} catch (SAXException | IOException e) {
				e.printStackTrace();
			}
			
			// 2º Obtengo el usuario (que existe)
			Element usuario = (Element) dom.getElementById(idUsuario);
			
			// 3º Busco la playlist en particular
			NodeList listaPlaylists = usuario.getElementsByTagName("playlist");
			
			// 4º Añado las canciones a la playlist
			int n = listaPlaylists.getLength();
			for(int i=0; i<n; i++) 
			{
				Element playlist = (Element) listaPlaylists.item(i);
				String playlistName = playlist.getAttribute("playlistName");
				if (playlistName.equals(namePlaylist)) 
				{
					for(Song s : songs) {
						Element song = dom.createElement("song");
						Element title = dom.createElement("title");
						Text aux = dom.createTextNode(s.getTitle());
						title.appendChild(aux);
						Element duration = dom.createElement("duration");
						aux = dom.createTextNode(String.valueOf(s.getDuration()));
						duration.appendChild(aux);
						song.appendChild(title);
						song.appendChild(duration);
						playlist.appendChild(song);
					}
					break;
				}
			}
			saveContext();
		}
	}

	/* Método que borra una serie de canciones existentes en una playlist de nombre "namePlaylist".
	 */
	public void removeAllSongs(String namePlaylist, HashSet<Integer> songPositions) 
	{
		synchronized(baseDeDatos) 
		{
			// 1º Actualizo el dom por si otro usuario ha modificado el xml
			try {
				this.dom = db.parse(baseDeDatos);
			} catch (SAXException | IOException e) {
				e.printStackTrace();
			}
			
			// 2º Obtengo el usuario (que existe)
			Element usuario = (Element) dom.getElementById(idUsuario);
			
			// 3º Busco la playlist en particular
			NodeList listaPlaylists = usuario.getElementsByTagName("playlist");
			
			// 4º Borro las canciones de la playlist
			int n = listaPlaylists.getLength();
			for(int i=0; i<n; i++) 
			{
				Element playlist = (Element) listaPlaylists.item(i);
				String playlistName = playlist.getAttribute("playlistName");
				if (playlistName.equals(namePlaylist)) 
				{
					NodeList songs = playlist.getElementsByTagName("song");
					for(Integer j : songPositions) {
						/* Como la misma canción, por ser una playlist, puede estar repetida
						 * pero solo queremos borrar la que ocupa la posición j-ésima, usamos
						 * un Set de posiciones. */
						Element song = (Element) songs.item(j.intValue());
						// Si en lugar del getParentNode tomas playlist, salta excepción.
						song.getParentNode().removeChild(song);
					}
					break;
				}
			}
			saveContext();
		}
	}

	/* Método que devuelve aquellas playlists del usuario identificado por "idUsuario"
	 * almacenadas en la base de datos XML del servidor.
	 */
	public HashMap<String, LinkedList<Song>> getPlaylists() {

		/* Recupero del dom de las playlist aquellas playlists que tienen por identificador a 
		 * "idUsuario" en forma de diccionario con clave el nombre de la playlist y por valor 
		 * el listado de canciones de la playlist.
		 */
		HashMap<String, LinkedList<Song>> playlists = new HashMap<String, LinkedList<Song>>();
			
		Element usuario = (Element) dom.getElementById(idUsuario);
		
		if(usuario==null) {
			addUsuario();
			return playlists;
		}
		
		NodeList listaPlaylists = usuario.getElementsByTagName("playlist");
		int n = listaPlaylists.getLength();
		for(int i=0; i<n; i++) 
		{
			Element playlist = (Element) listaPlaylists.item(i);
			String playlistName = playlist.getAttribute("playlistName");
			
			LinkedList<Song> songList = new LinkedList<Song>();
			
			NodeList songs = playlist.getElementsByTagName("song");
			int m = songs.getLength();
			for(int j=0; j<m; j++) {
				Element songElement = (Element) songs.item(j);
				String songName = songElement.getElementsByTagName("title").item(0).getTextContent();
				Float songDuration = Float.parseFloat(songElement.getElementsByTagName("duration").item(0).getTextContent());
				Song song = new Song(songName, songDuration);
				songList.add(song);
			}
			
			playlists.put(playlistName, songList);
		}
		
		return playlists;
	}

	/* Método que guarda el contexto en la base de datos xml. Contiene código crítico y el
	 * método que invoque a este debe manejar la sincronización adecuadamente.
	 */
	public void saveContext() 
	{
		// Transformo el DOM  en un nuevo xml actualizado
		try {
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer;
			transformer = transformerFactory.newTransformer();
			
			DOMSource source = new DOMSource(dom);
			StreamResult result = new StreamResult("./src/servidor/playlists/playlists.xml");
			
			// Para que esté identado el xml
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

			// Para que me guarde la declaración del !DOCTYPE y la relación con su dtd
	        DocumentType doctype = dom.getDoctype();
	        if(doctype != null) {
	            //transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, doctype.getPublicId());
	            transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, doctype.getSystemId());
	        }
	        
			transformer.transform(source, result);	
			
		} catch(TransformerException e) {
			e.printStackTrace();	
		}
	}
}
