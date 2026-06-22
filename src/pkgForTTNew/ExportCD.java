package pkgForTTNew;

import java.io.File;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import javax.swing.JOptionPane;

public class ExportCD
{
    public static boolean exportPlaylist(String musicBasePath, ArrayList<Track> tracks, String playlistName)
    {
        String outputBase = musicBasePath+"\\playlists\\CDImages";
        File bp = new File(outputBase);
        bp.mkdir();
        String outputDirectory = musicBasePath + "\\playlists\\CDimages\\" + playlistName;
        int rc = JOptionPane.showConfirmDialog(null, "Create CD images for:" + playlistName + "?", "Query:",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (rc == JOptionPane.NO_OPTION)
            return false;
        {
            File fil = new File(outputDirectory);
            if (fil.exists())
            {
                rc = JOptionPane.showConfirmDialog(null, playlistName + " CD images already exist. Overwrite?", "Query:",
                        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (rc == JOptionPane.NO_OPTION)
                    return false;
                if (!Utilities.deleteDir(fil))
                {
                    Utilities.msg("Could not delete "+fil.getName());
                    return false;
                }
            }
            fil.mkdir();
            int disk = 1;
            String outPath = fil.getPath();
            File dir = new File(outPath + "\\" + "disk" + disk);
            dir.mkdir();
            float duration = 0l;
            int sequence = 1;
            Utilities.out("disk " + disk);
            for (Track track : tracks)
            {
                Utilities.out(track.fileName);
                if (duration > 60 * 60)
                {
                    disk++;
                    Utilities.out("disk " + disk);
                    dir = new File(outPath + "\\" + "disk" + disk);
                    dir.mkdir();
                    duration = 0l;
                    sequence = 1;
                }
                duration += track.calculatedTime;
                Utilities.out("time:" + duration);
                String fIn = musicBasePath + "\\" + track.relativePath + "\\" + track.fileName;
                String nSeq = String.format("%02d", sequence);
                String fOut = outPath + "\\" + "disk" + disk + "\\" + nSeq + " " + track.fileName;
                //String fOut = outPath + "\\" + "disk" + disk + "\\" + nSeq + " " + track.orchestra + " - " + track.title
                        //+ track.suffix;
                copyFile(fIn, fOut);
                sequence++;
            }
            String msg = "Done. " + tracks.size() + " tracks exported to " + disk + " disks.";
            JOptionPane.showMessageDialog(null, msg);
        }
        return true;
    }
    

    private static boolean copyFile(String in, String out)
    {
        try
        {
            Path pIn = Paths.get(in);
            Path pOut = Paths.get(out);
            java.nio.file.Files.copy(pIn, pOut, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.COPY_ATTRIBUTES);
        }
        catch (IOException ex)
        {
            Utilities.out("IOException:" + ex.getMessage());
            return false;
        }
        catch (InvalidPathException ex)
        {
            Utilities.out("InvalidPathException:" + ex.getMessage());
            Utilities.out("***** ABNORMAL EXIT EXIT EXIT *****");
            System.exit(0);
        }
        return true;
    }
}
