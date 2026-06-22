package pkgForTTNew;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Queue;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import pkgForTTNew.Track.Status;
import pkgForTTNew.Track.Style;
import pkgForTTNew.Track.Type;
//import TrackXMLReader.Track;
//import TrackXMLReader.TrackReader.TrackHandler;

public class TrackReader
{
    private XMLReader xmlReader;
    private TrackHandler handler;
    private boolean finished = false;

    public TrackReader(String xmlPath) throws Exception
    {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(false);
        SAXParser parser = factory.newSAXParser();
        xmlReader = parser.getXMLReader();
        handler = new TrackHandler();
        xmlReader.setContentHandler(handler);
        // Start parsing in a background push fashion
        InputStream in = new FileInputStream(xmlPath);
        xmlReader.parse(new InputSource(in));
    }

    public boolean hasNext()
    {
        return handler.hasNextTrack();
    }

    public Track nextTrack()
    {
        return handler.getNextTrack();
    }

    // ============================
    // Internal SAX Handler
    // ============================
    private static class TrackHandler extends DefaultHandler
    {
        private Queue<Track> trackQueue = new ArrayDeque<>();
        private Track currentTrack = null;
        private StringBuilder chars = new StringBuilder();
        private boolean insideTrack = false;
        private boolean insideInTandas = false;

        @Override
        public void startElement(String uri, String local, String qName, Attributes atts)
        {
            chars.setLength(0);
            if (qName.equals("Track"))
            {
                insideTrack = true;
                currentTrack = new Track();
                currentTrack.inTandas = new ArrayList<Long>();
            }
            else if (qName.equals("inTandas"))
            {
                insideInTandas = true;
            }
        }

        @Override
        public void characters(char[] ch, int start, int length)
        {
            chars.append(ch, start, length);
        }

        @Override
        public void endElement(String uri, String local, String qName)
        {
            if (!insideTrack)
                return;
            String text = chars.toString().trim();
            switch (qName)
            {
            case "uniqueId":
                currentTrack.uniqueId = parseInt(text);
                break;
            case "sequenceInAlbum":
                currentTrack.sequenceInAlbum = parseInt(text);
                break;
            case "title":
                currentTrack.title = text;
                break;
            case "orchestra":
                currentTrack.orchestra = text;
                break;
            case "normalizedOrchestra":
                currentTrack.normalizedOrchestra = text;
                break;
            case "searchTerm":
                currentTrack.searchTerm = text;
                break;
            case "rating":
                currentTrack.rating = parseInt(text);
                break;
            case "tempo":
                currentTrack.tempo = parseInt(text);
                break;
            case "style":
                try
                {
                    currentTrack.style = Style.valueOf(text);
                }
                catch (IllegalArgumentException ex)
                {
                    Utilities.out("Illegal style:" + currentTrack.uniqueId + " " + currentTrack.title);
                    currentTrack.style = Style.Unknown;
                }
                break;
            case "songTime":
                currentTrack.songTime = text;
                break;
            case "album":
                currentTrack.album = text;
                break;
            case "relativePath":
                currentTrack.relativePath = text;
                break;
            case "fileName":
                currentTrack.fileName = text;
                break;
            case "suffix":
                currentTrack.suffix = text;
                break;
            case "fileType":
                try
                {
                currentTrack.fileType = Type.valueOf(text);
                }
                catch (IllegalArgumentException ex)
                {
                    Utilities.out("Unknown type, set to .wav:" + currentTrack.uniqueId + " " + currentTrack.title);
                    currentTrack.fileType = Type.WAV;
                }
                break;
            case "comment":
                currentTrack.comment = text;
                break;
            case "calculatedTime":
                currentTrack.calculatedTime = Float.parseFloat(text);
                // parseDouble(text);
                break;
            case "status":
                String status = text;
                try
                {
                currentTrack.status = Status.valueOf(text);
                }
                catch (IllegalArgumentException ex)
                {
                    Utilities.out("Unknown status, set to Invalid:" + currentTrack.uniqueId + " " + currentTrack.title);
                    currentTrack.status = Status.Invalid;
                }
                break;
            case "Version":
                currentTrack.Version = text;
                break;
            case "spoof":
                currentTrack.spoof = parseInt(text);
                break;
            case "year":
                currentTrack.year = text;
                break;
            // nested list
            case "long":
                if (insideInTandas)
                    currentTrack.inTandas.add(parseLong(text));
                break;
            case "inTandas":
                insideInTandas = false;
                break;
            case "Track":
                // Finished a full track
                trackQueue.add(currentTrack);
                currentTrack = null;
                insideTrack = false;
                break;
            }
        }

        private Integer parseInt(String s)
        {
            try
            {
                return Integer.parseInt(s);
            }
            catch (Exception e)
            {
                return null;
            }
        }

        private Double parseDouble(String s)
        {
            try
            {
                return Double.parseDouble(s);
            }
            catch (Exception e)
            {
                return null;
            }
        }

        private Long parseLong(String s)
        {
            try
            {
                return Long.parseLong(s);
            }
            catch (Exception e)
            {
                return null;
            }
        }

        public boolean hasNextTrack()
        {
            return !trackQueue.isEmpty();
        }

        public Track getNextTrack()
        {
            return trackQueue.poll();
        }
    }
}