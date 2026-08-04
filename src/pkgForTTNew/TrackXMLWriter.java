package pkgForTTNew;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class TrackXMLWriter
{
    public static void writeTracksToXml_old(ArrayList<Track> tracks, String filePath) throws IOException
    {
        Utilities.out("Writing:"+filePath);
        try (FileWriter out = new FileWriter(filePath))
        {
            out.write("<Tracks>\n");
            for (Track t : tracks)
            {
                out.write("  <Track>\n");
                // Basic scalar fields
                out.write(tag("uniqueId", t.uniqueId));
                out.write(tag("sequenceInAlbum", t.sequenceInAlbum));
                out.write(tag("title", t.title));
                out.write(tag("orchestra", t.orchestra));
                out.write(tag("normalizedOrchestra", t.normalizedOrchestra));
                out.write(tag("searchTerm", t.searchTerm));
                out.write(tag("rating", t.rating));
                out.write(tag("tempo", t.tempo));
                // Enum → string
                if (t.style != null)
                    out.write(tag("style", t.style.toString()));
                out.write(tag("songTime", t.songTime));
                out.write(tag("album", t.album));
                out.write(tag("relativePath", t.relativePath));
                out.write(tag("fileName", t.fileName));
                out.write(tag("suffix", t.suffix));
                out.write(tag("comment", t.comment)); // will write empty if null
                out.write(tag("calculatedTime", t.calculatedTime));
                out.write(tag("status", t.status));
                out.write(tag("Version", t.Version));
                out.write(tag("spoof", t.spoof));
                // Nested ArrayList<Long>: <inTandas><long>...</long></inTandas>
                out.write("    <inTandas>\n");
                if (t.inTandas != null)
                {
                    for (Long val : t.inTandas)
                    {
                        out.write("      <long>" + val + "</long>\n");
                    }
                }
                out.write("    </inTandas>\n");
                out.write("  </Track>\n");
            }
            out.write("</Tracks>\n");
        }
    }
    
    public static void writeTracksToXml(ArrayList<Track> tracks, String filePath) throws IOException
    {
        Utilities.out("Writing:"+filePath);

        // Write to a temp file in the same directory and only replace the real file
        // once the write has fully succeeded. Opening filePath directly (the old
        // approach) truncates it immediately, so any failure partway through --
        // e.g. another process briefly holding the file open -- left tracks.xml
        // empty or missing with no way back short of an old backup. This never
        // touches the real file at all unless the new content is complete.
        File target = new File(filePath);
        File tempFile = File.createTempFile("tracks-", ".xml.tmp", target.getAbsoluteFile().getParentFile());
        try (Writer out = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(tempFile), StandardCharsets.UTF_8)))
        {
            out.write("<Tracks>\n");
            for (Track t : tracks)
            {
                out.write("  <Track>\n");
                out.write(tag("uniqueId", t.uniqueId));
                out.write(tag("sequenceInAlbum", t.sequenceInAlbum));
                out.write(tag("title", t.title));
                out.write(tag("orchestra", t.orchestra));
                out.write(tag("normalizedOrchestra", t.normalizedOrchestra));
                out.write(tag("searchTerm", t.searchTerm));
                out.write(tag("rating", t.rating));
                out.write(tag("tempo", t.tempo));

                if (t.style != null)
                    out.write(tag("style", t.style.toString()));

                out.write(tag("year", t.year));
                out.write(tag("songTime", t.songTime));
                out.write(tag("album", t.album));
                out.write(tag("relativePath", t.relativePath));
                out.write(tag("fileName", t.fileName));
                out.write(tag("suffix", t.suffix));
                if (t.fileType != null)
                    out.write(tag("fileType", t.fileType.toString()));
                out.write(tag("comment", t.comment));
                out.write(tag("calculatedTime", t.calculatedTime));
                if (t.measuredLufs != null)
                    out.write(tag("measuredLufs", t.measuredLufs));
                out.write(tag("status", t.status));
                out.write(tag("Version", t.Version));
                out.write(tag("spoof", t.spoof));

                out.write("    <inTandas>\n");
                if (t.inTandas != null)
                {
                    for (Long val : t.inTandas)
                    {
                        out.write("      <long>" + val + "</long>\n");
                    }
                }
                out.write("    </inTandas>\n");
                out.write("  </Track>\n");
            }
            out.write("</Tracks>\n");
        }
        catch (IOException ex)
        {
            tempFile.delete();
            throw ex;
        }
        Files.move(tempFile.toPath(), target.toPath(),
                StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }


    // Helper method to create a simple XML tag
    private static String tag(String name, Object value)
    {
        if (value == null)
        {
            return "    <" + name + "></" + name + ">\n";
        }
        return "    <" + name + ">" + escape(value.toString()) + "</" + name + ">\n";
    }

    // Very basic XML escaping
    private static String escape(String s)
    {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}