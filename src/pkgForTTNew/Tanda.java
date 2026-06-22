package pkgForTTNew;

import java.time.LocalDateTime;
import java.util.ArrayList;


public class Tanda implements Comparable<Tanda>
{
    public String orchestra;
    public String description;
    public String source;
    // public TrackIDContainer mTrackIDContainer = new TrackIDContainer();
    ArrayList<Long> mTracks = new ArrayList<Long>();
    Track.Style style;
    // int seq;
    float calculatedTime = 0f;
    public LocalDateTime lastChangedDateTime;
    public LocalDateTime lastPlayedDateTime;
    long uniqueId;
    boolean bChanged = false;

    public Tanda(String string)
    {
        orchestra = string;
    }

    public long getNextTrackID(long trackID)
    {
        long nextTrackID = -1;
        for (int i = 0 ; i < mTracks.size() ; i++)
        {
            if (mTracks.get(i) == trackID)
            {
                if (i+1 >= mTracks.size())
                    break;
                nextTrackID = mTracks.get(i+1);
                break;
            }
        }
        return nextTrackID;
    }

    public long getPreviousTrackID(long trackID)
    {
        for (int i = 1; i < mTracks.size(); i++)
        {
            if (mTracks.get(i) == trackID)
                return mTracks.get(i - 1);
        }
        return -1L;
    }

    public long getLastTrackID()
    {
        if (mTracks.isEmpty()) return -1L;
        return mTracks.get(mTracks.size() - 1);
    }

    public boolean isChanged()
    {
        return bChanged;
    }

    public void setChanged(boolean chg)
    {
        bChanged = chg;
    }

    public boolean insertTrack(long newTrackId, long insertAfterTrackId)
    {
        int rc = Utilities.insertAfter(mTracks, insertAfterTrackId, newTrackId);
        if (rc == 1)
        {
            setChanged(true);
            return true;
        }
        if (rc == -1)
            Utilities.msg("Can't insert  - duplicate");
        else
            Utilities.msg("Can't insert  - internal error");
        return false;
    }

    public boolean moveTrack(long sourceTrackID, long targetTrackID)
    {
        boolean rc = Utilities.move(mTracks, sourceTrackID, targetTrackID);
        if (rc)
            setChanged(true);
        return rc;
    }

    public boolean moveTrackToTop(long tid)
    {
        boolean rc = Utilities.moveToTop(mTracks, tid);
        if (rc)
            setChanged(true);
        return rc;
    }

    public long[] getTrackIDs()
    {
        return Utilities.getUIDs(mTracks);
    }

    public boolean deleteTrack(long UID)
    {
        boolean rc = Utilities.delete(mTracks, UID);
        if (rc)
            setChanged(true);
        return rc;
    }

    public boolean trackInTanda(long tid)
    {
        return Utilities.exists(mTracks, tid);
    }

    public boolean insertTrackAtTop(long tid)
    {
        boolean rc = Utilities.insertAtTop(mTracks, tid);
        if (rc)
            setChanged(true);
        return rc;
    }

    public int getTrackCount()
    {
        return mTracks.size();
    }

    public long getTrackID(int index)
    {
        return mTracks.get(index);
    }

    public String createExportString(long playlistUID)
    {
        StringBuffer sb = new StringBuffer();
        // Add in playlist sequence too facilitate move within a playlist
        sb.append(playlistUID + ",");
        sb.append(orchestra + ",");
        sb.append(style + ",");
        sb.append(description + ",");
        sb.append(uniqueId + ",");
        sb.append(mTracks.size() + ",");
        long[] trackIDs = Utilities.getUIDs(mTracks);
        for (int i = 0; i < trackIDs.length; i++)
        {
            sb.append(trackIDs[i] + ",");
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    // void addTrack(Track track)
    // {
    // mTrackIDContainer.add(track.uniqueId);
    // }
    public boolean addTrackID(long tid)
    {
        boolean rc = Utilities.addAtBottom(mTracks, tid);
        if (rc)
            setChanged(true);
        return rc;
    }

    public String toString()
    {
        String time = SoundUtils.formatSecTohhmmss((int) calculatedTime);
        // String time = SoundUtils.formatIntoHHMMSS((int)calculatedTime);
        return orchestra + " (" + uniqueId + ") " + style + " " + time;
    }

    boolean isFullTanda()
    {
        return mTracks.size() >= 3;
    }

    private void out(String in)
    {
        System.out.println(in);
    }

    @Override
    public int compareTo(Tanda o)
    {
        if (o.orchestra == null)
            out("orchestra is null (1)");
        if (this.orchestra == null)
            out("orchestra is null (2)");
        int ret = this.orchestra.compareToIgnoreCase(o.orchestra);
        return ret;
    }

    public String convertToXML()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("<Tanda>\n");
        sb.append("  <uniqueId>").append(uniqueId).append("</uniqueId>\n");
        sb.append("  <orchestra>").append(Utilities.xmlEscape(orchestra)).append("</orchestra>\n");
        sb.append("  <description>").append(Utilities.xmlEscape(description)).append("</description>\n");
        sb.append("  <source>").append(Utilities.xmlEscape(source)).append("</source>\n");
        sb.append("  <style>").append(style != null ? style.toString() : "").append("</style>\n");
        sb.append("  <calculatedTime>").append(calculatedTime).append("</calculatedTime>\n");
        sb.append("  <tracks>\n");
        if (mTracks != null)
            for (Long id : mTracks)
                sb.append("    <id>").append(id).append("</id>\n");
        sb.append("  </tracks>\n");
        sb.append("</Tanda>");
        return sb.toString();
    }

    public static int buildATanda(String str, Tanda tanda)
    {
        Utilities.msg("buildATanda");
        int returnPlaylistSequence = 0;
        String[] res = str.split("[,]");
        returnPlaylistSequence = Integer.parseInt(res[0]);
        tanda.orchestra = res[1];
        tanda.style = Track.Style.valueOf(res[2]);
        tanda.description = res[3];
        tanda.uniqueId = Long.parseLong(res[4]);
        int trackCount = Integer.parseInt(res[5]);
        for (int i = 0; i < trackCount; i++)
        {
            Long UID = Long.parseLong(res[i + 6]);
            tanda.addTrackID(UID);
        }
        return returnPlaylistSequence;
    }
}
