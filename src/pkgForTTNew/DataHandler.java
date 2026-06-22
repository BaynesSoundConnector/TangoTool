package pkgForTTNew;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;

import javax.swing.JOptionPane;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class DataHandler
{
    public DataHandler() {}

    public static boolean loadDataNew(String musicBasePath, TrackTableModel trackTableModel,
            TandaTreeModel tandaTreeModel, PlaylistTreeModel playlistTreeModel)
    {
        int tandaCount = 0;
        int playlistCount = 0;
        ArrayList<Tanda> tandas = new ArrayList<>();
        ArrayList<Playlist> playlists = new ArrayList<>();

        ArrayList<Track> tracks = getTracks(musicBasePath + "/" + "tracks.xml");

        try
        {
            File file = new File(musicBasePath + "/" + "database.xml");
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file), "UTF-8"));
            StringBuilder sb = new StringBuilder();

            String str = br.readLine();
            if (str == null || !str.equals("***tracks***"))
            {
                JOptionPane.showMessageDialog(null, "Invalid Database:" + file.getAbsolutePath());
                br.close();
                return false;
            }
            // Bypass tracks section
            while (br.ready())
            {
                str = br.readLine();
                if (str.equals("***tandas***"))
                    break;
            }
            // Process Tandas — handles both old XStream format and new format
            while (br.ready())
            {
                str = br.readLine();
                if (str.equals("***playlists***"))
                    break;
                if (str.equals("</pkgForTTNew.Tanda>") || str.equals("</Tanda>"))
                {
                    sb.append(str);
                    Tanda tanda = parseTanda(sb.toString());
                    if (tanda != null)
                    {
                        tanda.bChanged = false;
                        tandas.add(tanda);
                        tandaCount++;
                    }
                    sb = new StringBuilder();
                }
                else
                    sb.append(str).append("\n");
            }
            // Process Playlists — handles both old XStream format and new format
            while (br.ready())
            {
                str = br.readLine();
                if (str.equals("</pkgForTTNew.Playlist>") || str.equals("</Playlist>"))
                {
                    sb.append(str);
                    Playlist playlist = parsePlaylist(sb.toString());
                    if (playlist != null)
                    {
                        playlist.setChanged(false);
                        playlists.add(playlist);
                        playlistCount++;
                    }
                    sb = new StringBuilder();
                }
                else
                    sb.append(str).append("\n");
            }
            br.close();
            trackTableModel.setTracks(tracks);
            tandaTreeModel.setTandas(tandas);
            playlistTreeModel.setPlaylists(playlists);
            Utilities.out("loadDataNew completed, Tracks=" + tracks.size()
                    + ", Tandas=" + tandaCount + ", Playlists=" + playlistCount);
        }
        catch (IOException ex)
        {
            Utilities.out("DataHandler.loadDataNew() IOException:" + ex);
        }
        return true;
    }

    private static Tanda parseTanda(String xml)
    {
        try
        {
            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = db.parse(new InputSource(new StringReader(xml)));
            Element root = doc.getDocumentElement();
            boolean isOld = root.getTagName().equals("pkgForTTNew.Tanda");

            Tanda tanda = new Tanda("");
            tanda.uniqueId = parseLong(getTag(root, "uniqueId"));
            tanda.orchestra = getTag(root, "orchestra");
            tanda.description = getTag(root, "description");
            tanda.source = getTag(root, "source");
            String styleStr = getTag(root, "style");
            if (!styleStr.isEmpty())
            {
                try { tanda.style = Track.Style.valueOf(styleStr); }
                catch (IllegalArgumentException e) {}
            }
            String calcTime = getTag(root, "calculatedTime");
            if (!calcTime.isEmpty())
                tanda.calculatedTime = Float.parseFloat(calcTime);

            // Old format: <mTracks><long>id</long>...  New format: <tracks><id>id</id>...
            String containerTag = isOld ? "mTracks" : "tracks";
            String idTag = isOld ? "long" : "id";
            NodeList containers = root.getElementsByTagName(containerTag);
            if (containers.getLength() > 0)
            {
                NodeList ids = ((Element) containers.item(0)).getElementsByTagName(idTag);
                for (int i = 0; i < ids.getLength(); i++)
                    tanda.addTrackID(Long.parseLong(ids.item(i).getTextContent().trim()));
            }
            return tanda;
        }
        catch (Exception e)
        {
            Utilities.out("DataHandler.parseTanda() Exception:" + e.getMessage());
            return null;
        }
    }

    private static Playlist parsePlaylist(String xml)
    {
        try
        {
            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = db.parse(new InputSource(new StringReader(xml)));
            Element root = doc.getDocumentElement();
            boolean isOld = root.getTagName().equals("pkgForTTNew.Playlist");

            Playlist playlist = new Playlist("");
            playlist.uniqueId = parseLong(getTag(root, "uniqueId"));
            playlist.name = getTag(root, "name");
            playlist.lastChangedDateTime = parseLong(getTag(root, "lastChangedDateTime"));
            playlist.currentlyPlayingTrack = parseLong(getTag(root, "currentlyPlayingTrack"));
            playlist.currentlyPlayingTanda = parseLong(getTag(root, "currentlyPlayingTanda"));
            String calcTime = getTag(root, "calculatedTime");
            if (!calcTime.isEmpty())
                playlist.calculatedTime = Float.parseFloat(calcTime);

            // Old format: <tandaIDs><long>id</long>...  New format: <tandas><id>id</id>...
            String containerTag = isOld ? "tandaIDs" : "tandas";
            String idTag = isOld ? "long" : "id";
            NodeList containers = root.getElementsByTagName(containerTag);
            if (containers.getLength() > 0)
            {
                NodeList ids = ((Element) containers.item(0)).getElementsByTagName(idTag);
                for (int i = 0; i < ids.getLength(); i++)
                    playlist.addTandaID(Long.parseLong(ids.item(i).getTextContent().trim()));
            }
            return playlist;
        }
        catch (Exception e)
        {
            Utilities.out("DataHandler.parsePlaylist() Exception:" + e.getMessage());
            return null;
        }
    }

    private static String getTag(Element e, String name)
    {
        NodeList nl = e.getElementsByTagName(name);
        if (nl.getLength() == 0)
            return "";
        return nl.item(0).getTextContent().trim();
    }

    private static long parseLong(String s)
    {
        if (s == null || s.isEmpty())
            return 0L;
        try { return Long.parseLong(s); }
        catch (NumberFormatException e) { return 0L; }
    }

    private static ArrayList<Track> getTracks(String fn)
    {
        ArrayList<Track> tracks = new ArrayList<>();
        try
        {
            TrackReader reader = new TrackReader(fn);
            while (reader.hasNext())
                tracks.add(reader.nextTrack());
            Utilities.out("Loaded " + tracks.size() + " tracks.");
        }
        catch (Exception e)
        {
            Utilities.out("DataHandler.getTracks() Exception:" + e.getMessage());
        }
        return tracks;
    }
}
