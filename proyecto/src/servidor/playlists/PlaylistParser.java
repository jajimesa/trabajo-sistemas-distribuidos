package servidor.playlists;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import modelo.Song;

public class PlaylistParser {

	private DocumentBuilderFactory dbf;
	private DocumentBuilder db;
	private Document dom;
	private String idUsuario;
	
	public PlaylistParser(String idUsuario) {
		this.idUsuario = idUsuario;
		try {
			this.dbf = DocumentBuilderFactory.newInstance();
			this.db = dbf.newDocumentBuilder();
			this.dom = db.parse(new File("./src/servidor/playlists/playlists.xml"));

		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/* Método que devuelve cierto si existe una playlist del usuario identificado por
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
	
	/* PRE: No existe ya una playlist con nombre "namePlaylist".
	 * Método que crea una playlist con nombre "namePlaylist". 
	 */
	public void addPlaylist(String namePlaylist) 
	{
		Element usuario = (Element) dom.getElementById(idUsuario);
		
		if(usuario==null) {
			usuario = dom.createElement("user");
			usuario.setAttribute("identificador", idUsuario);
			Element root = dom.getDocumentElement();
			root.appendChild(usuario);
		}
		
		Element playlist = dom.createElement("playlist");
		playlist.setAttribute("namePlaylist", namePlaylist);
		usuario.appendChild(playlist);
	}
	
	/* Método que añade canciones a una playlist existente de nombre "namePlaylist".
	 */
	public void addAllSongs(String namePlaylist, List<Song> songs) 
	{
		Element usuario = (Element) dom.getElementById(idUsuario);
		
		if(usuario==null) {
			usuario = dom.createElement("user");
			usuario.setAttribute("identificador", idUsuario);
			Element root = dom.getDocumentElement();
			root.appendChild(usuario);
		}
		
		NodeList listaPlaylists = usuario.getElementsByTagName("playlist");
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
					title.setTextContent(s.getTitle());
					Element duration = dom.createElement("duration");
					title.setTextContent(String.valueOf(s.getDuration()));
					song.appendChild(title);
					song.appendChild(duration);
					playlist.appendChild(song);
				}
			}
		}
	}
	
	/* Método que añade una canción a una playlist existente de nombre "namePlaylist".
	 */
	public void addSong(String namePlaylist, Song s) {
		List<Song> aux = new ArrayList<Song>(1);
		aux.add(s);
		addAllSongs(namePlaylist, aux);
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
			usuario = dom.createElement("user");
			usuario.setAttribute("identificador", idUsuario);
			Element root = dom.getDocumentElement();
			root.appendChild(usuario);
			return null;
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
}
