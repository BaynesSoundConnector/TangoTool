package pkgForTTNew;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.SwingWorker;


// A container for all of the tandas in a playlist. Also static functions for playlist I/O

public class Playlist implements PropertyChangeListener
{
    public ArrayList<Long> tandaIDs;
    // TandaIDContainer mTandaIDContainer = new TandaIDContainer();
    public String name;
    public long lastChangedDateTime;
    public long uniqueId;
    // public int seq;
    public long currentlyPlayingTrack;
    public long currentlyPlayingTanda;
    public float calculatedTime = 0f;
    //private Clip clip;
    private boolean bChanged = false;

    public Playlist(String name)
    {
        tandaIDs = new ArrayList<Long>();
        this.name = name;
        bChanged = false;
    }
    
    public long getFirstTandaID()
    {
        return (tandaIDs.get(0));
    }
    public boolean nextTanda()
    {
        for (int i = 0 ; i < tandaIDs.size()-1 ; i++)
        {
           if (tandaIDs.get(i) == currentlyPlayingTanda)
           {
               currentlyPlayingTanda = tandaIDs.get(i+1);
               return true;
           }
        }
        return false;
    }

    public boolean previousTanda()
    {
        for (int i = 1; i < tandaIDs.size(); i++)
        {
            if (tandaIDs.get(i) == currentlyPlayingTanda)
            {
                currentlyPlayingTanda = tandaIDs.get(i - 1);
                return true;
            }
        }
        return false;
    }
    public boolean xxxnextTanda()
    {
        for (int i = 0 ; i < tandaIDs.size() ; i++)
        {
            if (tandaIDs.get(i) == currentlyPlayingTanda)
            {
                if (i >= tandaIDs.size())
                    return false;
            }
            currentlyPlayingTanda = tandaIDs.get(i+1);
            return true;
        }
        return false;
    }

    public void setChanged(boolean flag)
    {
        bChanged = flag;
    }

    public boolean isChanged()
    {
        return bChanged;
    }

    public int getTandaCount()
    {
        return tandaIDs.size();
    }

    public String convertToXML()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("<Playlist>\n");
        sb.append("  <uniqueId>").append(uniqueId).append("</uniqueId>\n");
        sb.append("  <name>").append(Utilities.xmlEscape(name)).append("</name>\n");
        sb.append("  <lastChangedDateTime>").append(lastChangedDateTime).append("</lastChangedDateTime>\n");
        sb.append("  <currentlyPlayingTrack>").append(currentlyPlayingTrack).append("</currentlyPlayingTrack>\n");
        sb.append("  <currentlyPlayingTanda>").append(currentlyPlayingTanda).append("</currentlyPlayingTanda>\n");
        sb.append("  <calculatedTime>").append(calculatedTime).append("</calculatedTime>\n");
        sb.append("  <tandas>\n");
        if (tandaIDs != null)
            for (Long id : tandaIDs)
                sb.append("    <id>").append(id).append("</id>\n");
        sb.append("  </tandas>\n");
        sb.append("</Playlist>");
        return sb.toString();
    }

    public boolean insertTandaAtTop(long tandaUID)
    {
        boolean rc = Utilities.insertAtTop(tandaIDs, tandaUID);
        if (rc)
            bChanged = true;
        else
            Utilities.msg("Could not move");
        return rc;
    }

    public boolean insertTanda(long newTandaUID, long insertAfterTandaUID)
    {
        if (insertAfterTandaUID == -1)
        {
            Utilities.msg("not allowed any more");
            return false;
            // return Utilities.insertAtTop(tandaIDs, newTandaUID);
        }
        int rc = Utilities.insertAfter(tandaIDs, insertAfterTandaUID, newTandaUID);
        if (rc == 1)
        {
            setChanged(true);
            return true;
        }
        
        else
        {
            if (rc == -1)
                Utilities.msg("Can't insert, duplicate tanda in the playlist");
            else 
                Utilities.msg("Can't insert, internal error");
                
        }
        return false;
    }

    public boolean exists(long uid)
    {
        for (Long tid : tandaIDs)
        {
            if (tid.longValue() == uid)
                return true;
        }
        return false;
    }

    public boolean moveTanda(long fromTid, long insertTrackLocation)
    {
        boolean rc = Utilities.move(tandaIDs, fromTid, insertTrackLocation);
        if (!rc)
            Utilities.msg("Couldn't move");
        else
        {
            setChanged(true);
            return true;
        }
        return false;
    }

    public boolean deleteTanda(long UID)
    {
        boolean rc = Utilities.delete(tandaIDs, UID);
        if (rc)
            setChanged(true);
        return rc;
    }

    public long[] getTandaIDs()
    {
        return Utilities.getUIDs(tandaIDs);
    }

    public boolean addTandaID(long tid)
    {
        Boolean rc = Utilities.addAtBottom(tandaIDs, tid);
        if (rc)
            setChanged(true);
        return rc;
    }

    public String toString()
    {
        String time = SoundUtils.formatSecTohhmmss((int) calculatedTime);
        // String time = SoundUtils.formatIntoHHMMSS((int) calculatedTime);
        // String time = String.format("%.2f", calculatedTime);
        return name + " (" + uniqueId + ") " + time;
    }

    private static void out(String in)
    {
        System.out.println(in);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        Object s1 = evt.getOldValue();
        // SwingWorker.StateValue.DONE;
        Object s2 = evt.getNewValue();
        out("propertychange");
    }
}
