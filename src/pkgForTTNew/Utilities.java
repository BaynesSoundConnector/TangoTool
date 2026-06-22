package pkgForTTNew;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JButton;
import javax.swing.JOptionPane;

import org.apache.commons.text.similarity.LevenshteinDistance;

public class Utilities
{
    public static String getTimeString()
    {
        LocalDateTime ldt = LocalDateTime.now();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm");
        String formattedString = ldt.format(dtf);
        return formattedString;
    }

    public static String xmlEscape(String s)
    {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    public static boolean isNumeric(String str)
    {
        try
        {
            Double.parseDouble(str);
            return true;
        }
        catch (NumberFormatException e)
        {
            return false;
        }
    }

    public static String formatSecTohhmmss(int seconds)
    {
        long HH = seconds / 3600;
        long MM = (seconds % 3600) / 60;
        long SS = seconds % 60;
        String time;
        if (HH == 0)
            time = String.format("%2d:%02d", MM, SS);
        else
            time = String.format("%2d:%02d:%02d", HH, MM, SS);
        return time;
    }

    public static String xformatSecTohhmmss(int seconds)
    {
        long HH = seconds / 3600;
        long MM = (seconds % 3600) / 60;
        long SS = seconds % 60;
        String time;
        if (HH == 0)
            time = String.format("%2d:%02d", MM, SS);
        else
            time = String.format("%2d:%02d:%02d", HH, MM, SS);
        return time;
    }

    private static ArrayList<Track> getRemainingTracksInPlaylist(Playlist playlist, Track track,
            TrackTableModel trackTableModel)
    {
        Iterator<Long> it = playlist.tandaIDs.iterator();
        while (it.hasNext())
        {
            Long lll = it.next();
        }
        return null;
    }

    public static float calculateRemainingTimeInPlaylist(Object[] objs, TrackTableModel trackTableModel,
            TandaTreeModel tandaTreeModel)
    {
        if (objs.length == 2)
        {
            Playlist playlist = (Playlist) objs[1];
            Float time = calculateTimeInWholePlaylist(playlist, trackTableModel, tandaTreeModel);
            return time;
        }
        if (objs.length == 3)
        {
            Playlist playlist = (Playlist) objs[1];
            Tanda tanda = (Tanda) objs[2];
            Float time = calculatePlaylistTimeFromTanda(playlist, tanda, trackTableModel, tandaTreeModel);
            return time;
        }
        if (objs.length == 4)
        {
            Playlist playlist = (Playlist) objs[1];
            Tanda tanda = (Tanda) objs[2];
            Track track = (Track) objs[3];
            Float time = calculatePlaylistTimeFromTrack(playlist, tanda, track, trackTableModel, tandaTreeModel);
            return time;
        }
        return 0f;
    }

    private static float calculateTimeInWholePlaylist(Playlist playlist, TrackTableModel trackTableModel,
            TandaTreeModel tandaTreeModel)
    {
        float totalTime = 0f;
        long[] tandas = playlist.getTandaIDs();
        for (int i = 0; i < tandas.length; i++)
        {
            Tanda tanda = tandaTreeModel.getTandaByUniqueId(tandas[i]);
            if (tanda == null)
            {
                Utilities.out("null tanda, playlist=" + playlist.uniqueId + ", tanda=" + tandas[i]);
                return 0l;
            }
            long[] tracks = tanda.getTrackIDs();
            for (int j = 0; j < tracks.length; j++)
            {
                Track track = trackTableModel.getTrackbyUniqueId(tracks[j]);
                totalTime += track.calculatedTime;
            }
        }
        return totalTime;
    }

    private static float calculatePlaylistTimeFromTanda(Playlist playlist, Tanda tandaLoc,
            TrackTableModel trackTableModel, TandaTreeModel tandaTreeModel)
    {
        boolean bAccumulate = false;
        float totalTime = 0f;
        long[] tandaIDs = playlist.getTandaIDs();
        for (int i = 0; i < tandaIDs.length; i++)
        {
            Tanda tanda = tandaTreeModel.getTandaByUniqueId(tandaIDs[i]);
            if (bAccumulate == true || tandaIDs[i] == tandaLoc.uniqueId)
            {
                bAccumulate = true;
                totalTime += getTimeForTanda(tanda, trackTableModel);
            }
        }
        return totalTime;
    }

    private static float calculatePlaylistTimeFromTrack(Playlist playlist, Tanda tandaLoc, Track track,
            TrackTableModel trackTableModel, TandaTreeModel tandaTreeModel)
    {
        boolean bAccumulate1 = false;
        boolean bAccumulate2 = false;
        float totalTime = 0f;
        long[] tandaIDs = playlist.getTandaIDs();
        for (int i = 0; i < tandaIDs.length; i++)
        {
            Tanda tanda = tandaTreeModel.getTandaByUniqueId(tandaIDs[i]);
            if (bAccumulate1 || tanda.uniqueId == tandaLoc.uniqueId)
            {
                bAccumulate1 = true;
                long[] trackIDs = tanda.getTrackIDs();
                for (int j = 0; j < trackIDs.length; j++)
                {
                    if (bAccumulate2 == true || trackIDs[j] == track.uniqueId)
                    {
                        bAccumulate2 = true;
                        totalTime += trackTableModel.getTrackbyUniqueId(trackIDs[j]).calculatedTime;
                    }
                }
            }
        }
        return totalTime;
    }

    private static float getTimeForTanda(Tanda tanda, TrackTableModel trackTableModel)
    {
        float totalTime = 0f;
        long[] trackIDs = tanda.getTrackIDs();
        for (int i = 0; i < trackIDs.length; i++)
        {
            Track track = trackTableModel.getTrackbyUniqueId(trackIDs[i]);
            totalTime += track.calculatedTime;
        }
        return totalTime;
    }
    
    public static float calculateRemainingTimeInPlaylist(Playlist playlist, TrackTableModel trackTableModel,
            TandaTreeModel tandaTreeModel)
    {
        Track track = trackTableModel.getTrackbyUniqueId(playlist.currentlyPlayingTrack);
        Tanda tanda = tandaTreeModel.getTandaByUniqueId(playlist.currentlyPlayingTanda);
        Object[] objs = new Object[4];
        objs[1] = playlist;
        objs[2] = tanda;
        objs[3] = track;
        return calculatePlaylistTimeFromTrack( playlist,  tanda,  track,
                 trackTableModel,  tandaTreeModel);
    }
    public static float oldcalculateRemainingTimeInPlaylist(Playlist playlist, TrackTableModel trackTableModel,
            TandaTreeModel tandaTreeModel)
    {
        boolean bAccumulate = false;
        float totalTime = 0;
        // if (playlist.currentlyPlayingTanda == 0l)
        // return playlist.calculatedTime;
        long[] tandas = playlist.getTandaIDs();
        for (int i = 0; i < tandas.length; i++)
        {
            Tanda tanda = tandaTreeModel.getTandaByUniqueId(tandas[i]);
            if (tanda == null)
            {
                Utilities.out("null tanda, playlist=" + playlist.uniqueId + ", tanda=" + tandas[i]);
                return 0l;
            }
            long[] tracks = tanda.getTrackIDs();
            for (int j = 0; j < tracks.length; j++)
            {
                Track track = trackTableModel.getTrackbyUniqueId(tracks[j]);
                if (bAccumulate)
                    totalTime += track.calculatedTime;
                else
                {
                    if (tanda.uniqueId == playlist.currentlyPlayingTanda)
                    {
                        if (playlist.currentlyPlayingTrack == 0l || track.uniqueId == playlist.currentlyPlayingTrack)
                        {
                            bAccumulate = true;
                            Utilities.out("Time accumulate starting at tanda " + tanda.uniqueId + ", track:"
                                    + track.uniqueId);
                            totalTime += track.calculatedTime;
                        }
                    }
                }
            }
        }
        Utilities.out("Accumulated time:" + totalTime);
        return totalTime;
    }

    public static void out(String in)
    {
        System.out.println(in);
    }

    public static String validate(String musicBasePath, TrackTableModel trackTableModel, TandaTreeModel tandaTreeModel,
            PlaylistTreeModel playlistTreeModel, boolean fix)
    {
        // Validate tracks
        int notExistTracks = 0;
        int tandaTracksMissing = 0;
        int playlistTandaMissing = 0;
        StringBuffer sb = new StringBuffer();
        ArrayList<Track> badTracks = new ArrayList<Track>();
        for (Track track : trackTableModel.mTracks)
        {
            File file = new File(musicBasePath + "\\" + track.relativePath + "\\" + track.fileName);
            if (!file.exists())
            {
                badTracks.add(track);
                notExistTracks++;
                Utilities.out(file.getAbsolutePath() + " doesn't exist");
            }
        }
        if (fix)
        {
            for (Track track : badTracks)
            {
                trackTableModel.delete(track);
            }
            sb.append(badTracks.size() + " tracks deleted\n");
        }
        // Validate tandas
        for (Tanda tanda : tandaTreeModel.mTandas)
        {
            for (Long lll : tanda.mTracks)
            {
                if (trackTableModel.getTrackbyUniqueId(lll) == null)
                {
                    Utilities.out("Tanda:" + tanda.uniqueId + " track missing:" + lll);
                    if (fix)
                    {
                        tanda.deleteTrack(lll);
                    }
                    tandaTracksMissing++;
                }
            }
        }
        // Validate Playlists
        {
            for (Playlist playlist : playlistTreeModel.mPlaylists)
            {
                ArrayList<Long> todel = new ArrayList<Long>();
                for (long lll : playlist.getTandaIDs())
                {
                    todel = new ArrayList<Long>();
                    if (tandaTreeModel.getTandaByUniqueId(lll) == null)
                    {
                        playlistTandaMissing++;
                        todel.add(lll);
                    }
                }
                if (fix && todel.size() > 0)
                {
                    Utilities.out(todel.size() + " tandas to delete in playlist:" + playlist.uniqueId);
                    for (Long td : todel)
                    {
                        if (!playlist.deleteTanda(td))
                            Utilities.out("Failed to delete playlist=" + playlist.uniqueId + " tanda=" + td);
                    }
                }
            }
        }
        if (fix)
        {
            sb.append(notExistTracks + " missing music file references deleted\n");
            sb.append(tandaTracksMissing + " missing tanda tracks deleted\n");
            sb.append(playlistTandaMissing + " missing playlist tandas deleted");
        }
        else
        {
            sb.append(notExistTracks + " missing music file references\n");
            sb.append(tandaTracksMissing + " missing tanda tracks\n");
            sb.append(playlistTandaMissing + " missing playlist tandas");
        }
        Utilities.out(notExistTracks + " missing music files");
        Utilities.out(tandaTracksMissing + " missing tanda tracks");
        Utilities.out(playlistTandaMissing + " missing playlist tandas");
        return sb.toString();
    }

    public static Clip play(long trackUID, String musicBasePath, TrackTableModel trackTableModel, Clip clipIn,
            LineListener ll, JButton playButton, int framePosition)
    {
        Clip clipOut = null;
        if (trackUID == -1)
        {
            JOptionPane.showMessageDialog(null, "No row selected");
            return null;
        }
        Track track = trackTableModel.getTrackbyUniqueId(trackUID);
        if (track == null)
        {
            String msg = "Track is null:" + trackUID;
            JOptionPane.showMessageDialog(null, msg);
            return null;
        }
        String waveFileName = musicBasePath + "\\" + track.relativePath + "\\" + track.fileName;
        File file = new File(waveFileName);
        if (!file.exists())
        {
            String actualFileName = findFile(musicBasePath, track.relativePath, track.fileName);
            waveFileName = subtractBasePath(musicBasePath, waveFileName);
            if (waveFileName == null)
            {
                out("file doesn't exist");
                return null;
            }
        }
        clipOut = playFile2(waveFileName, clipIn, ll, framePosition);
        playButton.setText("Stop");
        return clipOut;
    }

    public static String findFile(String musicBasePath, String subDir, String fileName)
    {
        String dir = musicBasePath + "\\" + subDir;
        File fDir = new File(dir);
        File[] files = fDir.listFiles();
        if (files == null)
        {
            Utilities.out("fileFile() Directory is null:" + fDir.getPath());
            return null;
        }
        LevenshteinDistance levenshtein = new LevenshteinDistance();
        String match = "";
        int strDistance = 100;
        // Utilities.out("Matching:"+fileName);
        for (File file : files)
        {
            String str1 = file.getName();
            int distance = levenshtein.apply(str1, fileName);
            if (distance < strDistance)
            {
                match = str1;
                strDistance = distance;
                // Utilities.out("Matched:"+match+" distance="+strDistance);
            }
            // Utilities.out("Distance="+distance+" for:"+str1);
        }
        if (strDistance < 100)
        {
            Utilities.out("Final match for fileName:" + fileName + "\n is " + match + "\n distance is " + strDistance);
            return match;
        }
        else
        {
            Utilities.out("No match for " + fileName);
            return null;
        }
    }

    public static String findDirectory(String musicBasePath, String subDir)
    {
        File fDir = new File(musicBasePath);
        File[] files = fDir.listFiles();
        if (files == null)
        {
            Utilities.out("findDirectory() No files in musicBasePath:" + fDir.getPath());
            return null;
        }
        LevenshteinDistance levenshtein = new LevenshteinDistance();
        String match = "";
        int strDistance = 100;
        // Utilities.out("Matching:"+fileName);
        for (File file : files)
        {
            String str1 = file.getName();
            int distance = levenshtein.apply(str1, subDir);
            if (distance < strDistance)
            {
                match = str1;
                strDistance = distance;
                // Utilities.out("Matched:"+match+" distance="+strDistance);
            }
            // Utilities.out("Distance="+distance+" for:"+str1);
        }
        if (strDistance < 100)
        {
            Utilities.out("Final match for fileName:" + subDir + "\n is " + match + "\n distance is " + strDistance);
            return match;
        }
        else
        {
            Utilities.out("No match for " + subDir);
            return null;
        }
    }

    private static String subtractBasePath(String basePath, String pathIn)
    {
        int index = pathIn.indexOf(basePath);
        if (index == -1 || index != 0)
            return null;
        String out = pathIn.substring(basePath.length() + 1);
        File file = new File(out);
        if (!file.exists())
            return null;
        return out;
    }

    private static Clip playFile2(String fileName, Clip clipIn, LineListener ll, int framePosition)
    {
        AudioInputStream audioInputStream;
        Clip clipOut;
        if (clipIn != null)
        {
            clipIn.stop();
            clipIn.close();
        }
        try
        {
            File audioFile = new File(fileName);
            if (!audioFile.exists())
            {
                JOptionPane.showMessageDialog(null, "Cannot find file:" + audioFile.getName());
                return null;
            }
            Utilities.out("Play:" + fileName);
            audioInputStream = AudioSystem.getAudioInputStream(audioFile.getAbsoluteFile());
            AudioFormat baseFormat = audioInputStream.getFormat();
            if (baseFormat.getEncoding() != AudioFormat.Encoding.PCM_SIGNED)
            {
                AudioFormat pcmFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                        baseFormat.getSampleRate(), 16, baseFormat.getChannels(),
                        baseFormat.getChannels() * 2, baseFormat.getSampleRate(), false);
                audioInputStream = AudioSystem.getAudioInputStream(pcmFormat, audioInputStream);
            }
            // create clip reference
            clipOut = AudioSystem.getClip();
            clipOut.addLineListener(ll);
            // open audioInputStream to the clip
            clipOut.open(audioInputStream);
            int frameLength = clipOut.getFrameLength();
            long time = clipOut.getMicrosecondLength();
            if (framePosition > 0l)
                clipOut.setFramePosition(frameLength - framePosition);
            clipOut.loop(0);
            return clipOut;
        }
        catch (UnsupportedAudioFileException e)
        {
            String msg = "Unsupported Audio File Format:" + fileName;
            JOptionPane.showMessageDialog(null, msg);
        }
        catch (IOException e)
        {
            Utilities.out("playFile2 IOException:" + fileName);
            out(e.getMessage());
            e.printStackTrace();
        }
        catch (LineUnavailableException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    public static boolean insertAtTop(ArrayList<Long> list, long toBeInserted)
    {
        if (exists(list, toBeInserted))
            return false;
        try
        {
            int s1 = list.size();
            list.add(0, (Long) toBeInserted);
            Long lll = list.get(0);
            int s2 = list.size();
            if (s2 == (s1 + 1) && lll == toBeInserted)
                return true;
        }
        catch (IndexOutOfBoundsException ex)
        {
            return false;
        }
        return false;
    }

    public static int insertAfter(ArrayList<Long> list, long insertAfterTandaUID, long newTandaUID)
    {
        // Don't allow duplicates
        for (Long ll : list)
        {
            if (ll.longValue() == newTandaUID)
                return -1;
        }
        int s1 = list.size();
        try
        {
            for (int i = 0; i < list.size(); i++)
            {
                Long ll = list.get(i);
                if (ll.longValue() == insertAfterTandaUID)
                {
                    list.add(i + 1, (Long) newTandaUID);
                    break;
                }
            }
        }
        catch (IndexOutOfBoundsException ex)
        {
            return -2;
        }
        int s2 = list.size();
        if (s1 + 1 == s2)
            return 1;
        return -3;
    }

    public static boolean move(ArrayList<Long> list, long fromUID, long toUID)
    {
        boolean rc = true;
        if (!exists(list, fromUID) || !exists(list, toUID) || fromUID == toUID)
            return false;
        int s1 = list.size();
        for (int i = 0; i < list.size(); i++)
        {
            if (list.get(i).longValue() == fromUID)
            {
                list.remove(i);
                break;
            }
        }
        for (int i = 0; i < list.size(); i++)
        {
            if (list.get(i).longValue() == toUID)
            {
                list.add(i + 1, fromUID);
                break;
            }
        }
        int s2 = list.size();
        if (s1 == s2)
            return true;
        return false;
    }

    public static boolean moveToTop(ArrayList<Long> list, long fromUID)
    {
        if (!exists(list, fromUID))
            return false;
        int s1 = list.size();
        for (int i = 0; i < list.size(); i++)
        {
            if (list.get(i).longValue() == fromUID)
            {
                list.remove(i);
                break;
            }
        }
        list.add(0, fromUID);
        int s2 = list.size();
        if (s1 == s2)
            return true;
        return false;
    }

    public static boolean exists(ArrayList<Long> list, long UID)
    {
        for (Long lll : list)
            if (lll.longValue() == UID)
                return true;
        return false;
    }

    public static boolean delete(ArrayList<Long> list, long UID)
    {
        boolean rc = false;
        for (int i = 0; i < list.size(); i++)
        {
            if (list.get(i).longValue() == UID)
            {
                list.remove(i);
                return true;
            }
        }
        return rc;
    }

    public static long[] getUIDs(ArrayList<Long> list)
    {
        long[] out = new long[list.size()];
        for (int i = 0; i < out.length; i++)
            out[i] = list.get(i);
        return out;
    }

    public static boolean addAtBottom(ArrayList<Long> list, long UID)
    {
        boolean rc = false;
        if (!exists(list, UID))
        {
            list.add(UID);
            return true;
        }
        return rc;
    }

    public static void msg(String message)
    {
        JOptionPane.showMessageDialog(null, message);
    }

    public static boolean deleteDir(File file)
    {
        File[] contents = file.listFiles();
        if (contents != null)
        {
            for (File f : contents)
            {
                deleteDir(f);
            }
        }
        if (file.delete())
            return true;
        return false;
    }
}
