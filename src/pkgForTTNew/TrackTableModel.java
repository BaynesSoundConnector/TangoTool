package pkgForTTNew;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import org.apache.commons.text.similarity.*;
import org.apache.commons.text.similarity.LevenshteinDistance;

import javax.swing.JOptionPane;
import javax.swing.table.AbstractTableModel;

public class TrackTableModel extends AbstractTableModel
{
    public static final String START_TAG = "<TT2.Track>";
    public static final String END_TAG = "</TT2.Track>";
    public static final String NEW_START_TAG = "<pkgForTTNew.Track>";
    public static final String NEW_END_TAG = "</pkgForTTNew.Track>";
    // Named so callers outside this class (double-click play, drag-and-drop) don't
    // hardcode the column index -- it has shifted before (LUFS inserted before UID)
    // and will again if columns change.
    public static final int COL_UID = 7;
    String[] columnNames =
    { "Title", "Orchestra", "Style", "Rating", "Time", "Album", "LUFS", "UID", "Tandas" };
    HashMap<Long, Track> hmTracks = new HashMap<>();
    ArrayList<Track> mTracks = new ArrayList<Track>();
    Vector<Track> mTracksSubset;
    long nextAvailableUniqueId = 0L;
    String musicBasePath;
    private boolean bChanged = false;

    public TrackTableModel(String musicBasePath)
    {
        this.musicBasePath = musicBasePath;
    }

    public boolean isChanged()
    {
        return bChanged;
    }

    public void setChanged(boolean changed)
    {
        bChanged = changed;
    }

    public void addTrack(Track track)
    {
        track.uniqueId = getNextUID();
        Utilities.out("New track added, UID=" + track.uniqueId);
        mTracks.add(track);
        bChanged = true;
    }

    public void delete(Track track)
    {
        mTracks.remove(track);
        bChanged = true;
    }

    private long getNextUID()
    {
        long hiUID = 0l;
        for (Track track : mTracks)
        {
            if (track.uniqueId > hiUID)
                hiUID = track.uniqueId;
        }
        hiUID++;
        return hiUID;
    }

    public void setTracks(ArrayList<Track> tracks)
    {
        Utilities.out("Loading " + tracks.size() + " tracks into TrackDataModel");
        // if (fixDirectories(tracks))
        // setChanged(true);
        if (fixTracks(tracks))
            setChanged(true);
        mTracks = tracks;
        listDuplicateTrackUIDs();
    }

    private boolean fixDirectories(ArrayList<Track> tracks)
    {
        int invalidDirectories = 0;
        for (Track track : tracks)
        {
            File file = new File(musicBasePath + "\\" + track.relativePath);
            if (file.exists())
                continue;
            String newDir = Utilities.findDirectory(musicBasePath, track.relativePath);
            if (newDir != null)
            {
                invalidDirectories++;
                track.relativePath = newDir;
                Utilities.out("Incorrect directory corrected to:" + newDir);
            }
        }
        if (invalidDirectories > 0)
        {
            JOptionPane.showMessageDialog(null, "fixDirectories() " + invalidDirectories + " directories changed");
        }
        return false;
    }

    // Repair missing data in a track
    // Returns true if anything changed;
    private boolean fixTracks(ArrayList<Track> tracks)
    {
        int changed = 0;
        int invalid = 0;
        int fileNoMatch = 0;
        boolean reportbutdontchange = false;
        for (Track track : tracks)
        {
            File file = new File(musicBasePath + "\\" + track.relativePath + "\\" + track.fileName);
            if (!file.exists())
            {
                Utilities.out("File:" + file.getPath() + " doesn't exist");
                fileNoMatch++;
                String actualFileName = Utilities.findFile(musicBasePath, track.relativePath, track.fileName);
                if (actualFileName != null)
                {
                    if (actualFileName.equalsIgnoreCase(track.fileName))
                    {
                        Utilities.out("Not a file name mismatch, check directory name");
                        continue;
                    }
                    if (!reportbutdontchange)
                    {
                        Utilities.out("File name changed to:" + actualFileName);
                        track.fileName = actualFileName;
                        changed++;
                    }
                }
            }
            if (track.calculatedTime == 0f)
            {
                if (track.songTime == null)
                {
                    track.songTime = "";
                    changed++;
                }
                else if (!track.songTime.equals(""))
                {
                    track.songTime = "";
                    changed++;
                }
                float time = SoundUtils.musicFileValid(file);
                if (time == 0f)
                {
                    invalid++;
                    if (track.status != Track.Status.Invalid)
                    {
                        track.status = Track.Status.Invalid;
                        changed++;
                        continue;
                    }
                    continue;
                }
                int seconds = (int) time;
                track.calculatedTime = time;
                track.songTime = SoundUtils.formatIntoHHMMSS(seconds);
                changed++;
            }
            if (track.songTime == null || track.songTime.equals(""))
            {
                int tme = (int) track.calculatedTime;
                track.songTime = Utilities.formatSecTohhmmss(tme);
                changed++;
            }
            if (track.searchTerm == null || track.searchTerm.equals(""))
            {
                track.searchTerm = SearchTermBuilder.buildSearchTerm(track.title, track.orchestra);
                changed++;
            }
        }
        Utilities.out("fixTracks() " + changed + " tracks changed");
        Utilities.out("fixTracks() " + invalid + " invalid tracks");
        Utilities.out("File name mismatches: " + fileNoMatch);
        if (changed > 0)
            JOptionPane.showMessageDialog(null, "fixTracks() " + changed + " tracks changed");
        return (changed > 0);
    }

    private void listDuplicateTrackUIDs()
    {
        UIDComp comp = new UIDComp();
        mTracks.sort(comp);
        Track previousTrack = null;
        Iterator<Track> it = mTracks.iterator();
        while (it.hasNext())
        {
            Track track = it.next();
            if (previousTrack == null)
                previousTrack = track;
            else
            {
                if (track.uniqueId == previousTrack.uniqueId)
                {
                    out("dup" + track.uniqueId + " " + track.title);
                    previousTrack = track;
                }
            }
        }
    }

    private class UIDComp implements Comparator
    {
        @Override
        public int compare(Object o1, Object o2)
        {
            Track t1 = (Track) o1;
            Track t2 = (Track) o2;
            long res = t1.uniqueId - t2.uniqueId;
            if (res < 0)
                return -1;
            if (res == 0)
                return 0;
            return 1;
        }
    }

    public int getTrackCount()
    {
        return mTracks.size();
    }

    public int getSubsetTrackCount()
    {
        if (mTracksSubset != null)
            return mTracksSubset.size();
        else
            return 0;
    }

    public String getColumnName(int col)
    {
        return columnNames[col].toString();
    }

    @Override
    public int getRowCount()
    {
        if (mTracksSubset != null)
            return mTracksSubset.size();
        else
        {
            // Utilities.out("GetRowCount() "+mTracks.size()+" tracks");
            return mTracks.size();
        }
    }

    @Override
    public int getColumnCount()
    {
        // TODO Auto-generated method stub
        return columnNames.length;
    }

    @Override
    public Class<?> getColumnClass(int colIndex)
    {
        // TODO Auto-generated method stub
        if (colIndex == COL_UID)
            return Integer.class;
        Object obj = getValueAt(0, colIndex);
        if (obj == null)
            return String.class;
        return getValueAt(0, colIndex).getClass();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
        Track track;
        if (mTracksSubset != null)
            track = mTracksSubset.get(rowIndex);
        else
            track = mTracks.get(rowIndex);
        if (columnIndex == 0)
        {
            if (track.status == Track.Status.Invalid)
                return "(Invalid) " + track.title;
            return track.title;
        }
        if (columnIndex == 1)
            return track.orchestra; //***
        if (columnIndex == 2)
        {
            if (track.style == null)
                return "Unknown";
            return track.style.toString();
        }
        if (columnIndex == 3)
            return "" + track.rating;
        if (columnIndex == 4)
            return Utilities.formatSecTohhmmss((int) track.calculatedTime);
        // return track.songTime;
        if (columnIndex == 5)
            return track.album;
        if (columnIndex == 6)
            return track.measuredLufs == null ? "" : String.format("%.1f", track.measuredLufs);
        if (columnIndex == COL_UID)
            // return String.valueOf(track.uniqueId);
            return track.uniqueId;
        if (columnIndex == COL_UID + 1 && track.inTandas != null)
        {
            StringBuffer sb = new StringBuffer();
            Iterator<Long> it = track.inTandas.iterator();
            while (it.hasNext())
            {
                Long ll = it.next();
                sb.append(ll + ",");
            }
            if (sb.length() > 0)
                return sb.substring(0, sb.length() - 1);
            else
                return "";
        }
        return "";
    }

    public Track getTrack(int in)
    {
        if (in == -1)
        {
            JOptionPane.showMessageDialog(null, "Error:Can't get track " + in);
            return null;
        }
        Track track = mTracks.get(in);
        return mTracks.get(in);
    }

    public Track getTrackbyUniqueId(Long id)
    {
        Iterator<Track> it = mTracks.iterator();
        while (it.hasNext())
        {
            Track track = it.next();
            if ((long) id == track.uniqueId)
                return track;
        }
        return null;
    }

    public long getTrackUIDByFilename(String relativePath, String filename)
    {
        for (Track track : mTracks)
        {
            if (track.relativePath.equalsIgnoreCase(relativePath))
            {
                if (track.fileName.equalsIgnoreCase(filename))
                    return track.uniqueId;
            }
        }
        return -1l;
    }

    public long getTrackUIDByPartialFilename(String relativePath, String filename)
    {
        for (Track track : mTracks)
        {
            if (track.relativePath.equalsIgnoreCase(relativePath))
            {
                if (track.fileName.toLowerCase().startsWith(filename.toLowerCase()))
                    return track.uniqueId;
            }
        }
        return -1l;
    }

    public Track getTrackByFilename(String path, String filename)
    {
        for (Track track : mTracks)
        {
            if (track.relativePath.equalsIgnoreCase(path))
            {
                if (track.fileName.equalsIgnoreCase(filename))
                    return track;
            }
        }
        return null;
    }

    public long getTrackUIDByTitle(String title)
    {
        for (Track track : mTracks)
        {
            if (track.title == null)
                continue;
            if (track.title.toLowerCase().equals(title.toLowerCase()))
                return track.uniqueId;
        }
        return -1;
    }

    public long getTrackUIDByPartialTitle(String title)
    {
        for (Track track : mTracks)
        {
            if (track.title == null)
                continue;
            if (track.title.toLowerCase().startsWith(title.toLowerCase()))
                return track.uniqueId;
        }
        return -1;
    }

    public Track getTrackbyUniqueId2(long tid)
    {
        Iterator<Track> it = mTracks.iterator();
        while (it.hasNext())
        {
            Track track = it.next();
            if (tid == track.uniqueId)
                return track;
        }
        out("+++ERROR+++ TrackTableModel.getTrackByUniqueId2() track " + tid + " not found");
        return null;
    }

    public int search(String searchTerm)
    {
        mTracksSubset = new Vector<Track>();
        int count = 0;
        Iterator<Track> it = mTracks.iterator();
        while (it.hasNext())
        {
            Track track = it.next();
            if (track.searchTerm == null)
            {
                out("TrackTableModel.search() null searchterm");
                continue;
            }
            if (track.searchTerm.toLowerCase().contains(searchTerm.toLowerCase()))
            {
                mTracksSubset.add(track);
                count++;
            }
        }
        out(count + " found");
        if (count > 0)
        {
            this.fireTableDataChanged();
            return count;
        }
        else
        {
            mTracksSubset = null;
            return 0;
        }
    }

    public void reset()
    {
        if (mTracksSubset != null)
        {
            mTracksSubset = null;
        }
        this.fireTableDataChanged();
    }

    public void sort1(Vector<Track> tracks)
    {
        Comp2 comp = new Comp2();
        Collections.sort(tracks, comp);
    }

    private void out(String in)
    {
        System.out.println(in);
    }

    public class Comp2 implements Comparator
    {
        @Override
        public int compare(Object o1, Object o2)
        {
            Track t1 = (Track) o1;
            Track t2 = (Track) o2;
            if (isEmpty(t1.album) && !isEmpty(t2.album))
                return 1;
            if (!isEmpty(t1.album) && isEmpty(t2.album))
                return -1;
            if (isEmpty(t1.album) && isEmpty(t2.album))
                return (t1.relativePath.compareTo(t2.relativePath));
            if (t1.album.equals(t2.album))
            {
                return cmpr(t1.sequenceInAlbum, t2.sequenceInAlbum);
            }
            return (t1.album.compareTo(t2.album));
        }
    }

    public boolean writeNew(String DatabasePath)
    {
        try
        {
            TrackXMLWriter.writeTracksToXml(mTracks, DatabasePath);
            return true;
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
    }


    private boolean isEmpty(String str)
    {
        if (str == null || str.trim().length() == 0)
            return true;
        return false;
    }

    private int cmpr(int i1, int i2)
    {
        if (i1 > i2)
            return 1;
        if (i1 < i2)
            return -1;
        return 0;
    }
}
