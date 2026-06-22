package pkgForTTNew;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.JOptionPane;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import pkgForTTNew.Track.Style;

public class TandaTreeModel implements TreeModel
{
    TrackTableModel mTrackTableModel;
    String mMusicBasePath;
    // ArrayList<Playlist> mPlaylists = new ArrayList<Playlist>();
    Vector<TreeModelListener> tml = new Vector<TreeModelListener>();
    DefaultMutableTreeNode root = new DefaultMutableTreeNode("Tandas");
    // Vector<Playlist> mPlaylists = new Vector<Playlist>();
    ArrayList<Tanda> mTandas = new ArrayList<Tanda>();
    // Vector<Tanda> mTangos = new Vector<Tanda>();
    // Vector<Tanda> mMilongas = new Vector<Tanda>();
    // Vector<Tanda> mValses = new Vector<Tanda>();
    // Vector<Tanda> mOther = new Vector<Tanda>();
    ArrayList<Tanda> mSubsetOfTandas;
    DefaultMutableTreeNode tnTango = new DefaultMutableTreeNode("Tango");
    DefaultMutableTreeNode tnValse = new DefaultMutableTreeNode("Valse");
    DefaultMutableTreeNode tnMilonga = new DefaultMutableTreeNode("Milonga");
    DefaultMutableTreeNode tnOther = new DefaultMutableTreeNode("Other");
    long nextAvailableTandaUID;
    long trackBeingShown = -1;
    private boolean bChanged = false;

    public TandaTreeModel(String musicBasePath)
    {
        mMusicBasePath = musicBasePath;
    }
    public long getFirstTrackID(long tandaID)
    {
        for (Tanda tanda : mTandas)
        {
            if (tanda.uniqueId == tandaID)
                return tanda.getTrackID(0);
        }
        return -1l;
    }
    public long getNextTrackID(long tandaID, long trackID)
    {
        for (int i = 0 ; i < mTandas.size() ; i++)
        {
            Tanda tanda = mTandas.get(i);
            if (tanda.uniqueId == tandaID)
            {
                long tid = tanda.getNextTrackID(trackID);
                return tid;
            }
        }
        return -1l;
    }

    public long getPreviousTrackID(long tandaID, long trackID)
    {
        for (Tanda tanda : mTandas)
        {
            if (tanda.uniqueId == tandaID)
                return tanda.getPreviousTrackID(trackID);
        }
        return -1L;
    }

    public long getLastTrackID(long tandaID)
    {
        for (Tanda tanda : mTandas)
        {
            if (tanda.uniqueId == tandaID)
                return tanda.getLastTrackID();
        }
        return -1L;
    }

    public boolean isChanged()
    {
        for (Tanda tanda : mTandas)
        {
            if (tanda.bChanged)
                return true;
        }
        return bChanged;
    }

    public void setChanged(boolean changed)
    {
        bChanged = changed;
    }

    public void setTandas(ArrayList<Tanda> tandas)
    {
        mTandas = tandas;
    }

    public void setTrackTableModel(TrackTableModel tracks)
    {
        mTrackTableModel = tracks;
    }

    public boolean write(BufferedWriter writer)
    {
        try
        {
            writer.write("***tandas***\n");
            for (Tanda tanda : mTandas)
            {
                String str = tanda.convertToXML();
                writer.write(str + "\n"); // delimiter between tracks
            }
        }
        catch (IOException e)
        {
            out("TrackDatabase.writeOutDatabase() IOException:" + e.getMessage());
        }
        return true;
    }

    public void reset()
    {
        mSubsetOfTandas = new ArrayList<Tanda>();
    }

    public ArrayList<Long> trackInTandas(long trackUID)
    {
        ArrayList<Long> inTandas = new ArrayList<Long>();
        for (Tanda tanda : mTandas)
        {
            if (tanda.trackInTanda(trackUID))
                inTandas.add(tanda.uniqueId);
        }
        return inTandas;
    }

    public DefaultMutableTreeNode getTandaTypeNode(Track.Style style)
    {
        if (style == Track.Style.Tango)
            return tnTango;
        if (style == Track.Style.Valse)
            return tnValse;
        if (style == Track.Style.Milonga)
            return tnMilonga;
        if (style == Track.Style.Other)
            return tnOther;
        return null;
    }

    public int selectTandas(String searchTerm)
    {
        mSubsetOfTandas = new ArrayList<Tanda>();
        if (searchTerm == null)
            return 0;
        if (Utilities.isNumeric(searchTerm))
        {
            long lst = Long.parseLong(searchTerm);
            
            for (Tanda tanda : mTandas)
            {
                if (tanda.uniqueId == lst)
                {
                    mSubsetOfTandas.add(tanda);
                    return 1;
                }
            }
            return 0;
        }
        else
        {
            int count = 0;
            for (Tanda tanda : mTandas)
            {
                if (-1 != (tanda.orchestra.toLowerCase().indexOf(searchTerm.toLowerCase())))
                {
                    mSubsetOfTandas.add(tanda);
                    count++;
                }
                if (tanda.description != null && -1 != (tanda.description.toLowerCase().indexOf(searchTerm.toLowerCase())))
                {
                    mSubsetOfTandas.add(tanda);
                    count++;
                }
            }
            return count;
        }
    }

    // Collect all of the tandas containing a specific in a vector called mSubset
    public int selectTandas(long trackUID)
    {
        mSubsetOfTandas = new ArrayList<Tanda>();
        if (trackUID == -1)
        {
            return 0;
        }
        int count = 0;
        count = subset(mSubsetOfTandas, mTandas, trackUID);
        if (count == 0)
        {
            mSubsetOfTandas = new ArrayList<Tanda>();
        }
        return count;
    }

    private int subset(ArrayList<Tanda> subset, ArrayList<Tanda> source, long trackUID)
    {
        int count = 0;
        Iterator<Tanda> it = source.iterator();
        while (it.hasNext())
        {
            Tanda tan = it.next();
            long[] tids = tan.getTrackIDs();
            for (int i = 0; i < tids.length; i++)
            {
                if (tids[i] == trackUID)
                {
                    subset.add(tan);
                    count++;
                    break;
                }
            }
        }
        return count;
    }

    public void notifyTreeModelHasChanged()
    {
        Iterator<TreeModelListener> it = tml.iterator();
        while (it.hasNext())
        {
            TreeModelEvent event = new TreeModelEvent(root, new Object[]
            { root });
            it.next().treeStructureChanged(event);
        }
    }

    public TreePath getPathForTanda(Tanda tanda)
    {
        Object[] objs = new Object[3];
        objs[0] = root;
        if (tanda.style == Track.Style.Milonga)
            objs[1] = tnMilonga;
        else if (tanda.style == Track.Style.Tango)
            objs[1] = tnTango;
        else if (tanda.style == Track.Style.Valse)
            objs[1] = tnValse;
        else if (tanda.style == Track.Style.Other)
            objs[1] = tnOther;
        objs[2] = tanda;
        return new TreePath(objs);
    }

    // Add a tanda to the bottom of the tanda list
    public boolean addNewTanda(Tanda tanda)
    {
        tanda.uniqueId = getNextUniqueTandaID();
        mTandas.add(tanda);
        if (isSubsetting())
            mSubsetOfTandas.add(tanda);
        setChanged(true);
        return true;
    }

    public boolean deleteTanda(long tandaUID)
    {
        int index = getTandaIndex(tandaUID, mTandas);
        mTandas.remove(index);
        if (mSubsetOfTandas != null)
        {
            index = getTandaIndex(tandaUID, mSubsetOfTandas);
            mSubsetOfTandas.remove(index);
        }
        setChanged(true);
        return true;
    }

    private int getTandaIndex(long tandaUID, ArrayList<Tanda> tandas)
    {
        int i = 0;
        for (Tanda tanda : tandas)
        {
            if (tanda.uniqueId == tandaUID)
                return i;
            i++;
        }
        return -1;
    }

    private long getNextUniqueTandaID()
    {
        long nextUID = 1l;
        for (Tanda tanda : mTandas)
        {
            if (tanda.uniqueId > nextUID)
            {
                nextUID = tanda.uniqueId;
            }
        }
        nextUID++;
        return nextUID;
    }

    @Override
    public Object getRoot()
    {
        return root;
    }

    @Override
    public int getChildCount(Object parent)
    {
        if (isSubsetting())
        {
            // JOptionPane.showMessageDialog(null, "Subsetting");
            if (parent instanceof DefaultMutableTreeNode
                    && ((DefaultMutableTreeNode) parent).getUserObject().equals("Tandas"))
            {
                // Root has (number of subsetted tandas) children
                return mSubsetOfTandas.size();
            }
            if (parent instanceof Tanda)
            {
                Tanda tanda = (Tanda) parent;
                return tanda.getTrackCount();
            }
            out("shouldn't happen 2");
            return 0;
        }
        // Not subsetting
        else
        {
            // JOptionPane.showMessageDialog(null, "Not subsetting");
            // Is this root?
            if (parent instanceof DefaultMutableTreeNode
                    && ((DefaultMutableTreeNode) parent).getUserObject().equals("Tandas"))
            {
                return 4;
            }
            // Is this a tanda?
            else if (parent instanceof Tanda)
            {
                Tanda tanda = (Tanda) parent;
                return tanda.getTrackCount();
            }
            // Or it's a tanda type?
            else if (parent.toString().equalsIgnoreCase("Tango"))
                return getTrackCount(Track.Style.Tango);
            else if (parent.toString().equalsIgnoreCase("Valse"))
                return getTrackCount(Track.Style.Valse);
            else if (parent.toString().equalsIgnoreCase("Milonga"))
                return getTrackCount(Track.Style.Milonga);
            else if (parent.toString().equalsIgnoreCase("Other"))
                return getTrackCount(Track.Style.Other);
            else
            {
                JOptionPane.showMessageDialog(null, "Invalid Tanda Tree response 2	");
                return 0;
            }
        }
    }

    private int getTrackCount(Track.Style style)
    {
        int count = 0;
        for (Tanda tanda : mTandas)
        {
            if (tanda.style == style)
                count++;
            else if (style == Track.Style.Other)
            {
                if (tanda.style != Track.Style.Tango && tanda.style != Track.Style.Valse
                        && tanda.style != Track.Style.Milonga)
                    count++;
            }
        }
        return count;
    }

    @Override
    public Object getChild(Object parent, int index)
    {
        if (isSubsetting())
        {
            // Is this root?
            if (parent instanceof DefaultMutableTreeNode
                    && ((DefaultMutableTreeNode) parent).getUserObject().equals("Tandas"))
            {
                return mSubsetOfTandas.get(index);
            }
            else
            {
                if (parent instanceof Tanda)
                {
                    Tanda tanda = (Tanda) parent;
                    Long tid = tanda.getTrackID(index);
                    Track track = mTrackTableModel.getTrackbyUniqueId(tid);
                    if (track == null)
                    {
                        JOptionPane.showMessageDialog(null, "Track " + tid + " not found");
                        return "ERROR";
                    }
                    return track;
                }
                else
                    out("shouldn't happen");
            }
        }
        if (parent instanceof Tanda)
        {
            Tanda tan = (Tanda) parent;
            if (tan.uniqueId == 533)
                out("unique");
            long tid = tan.getTrackID(index);
            Track track = mTrackTableModel.getTrackbyUniqueId2(tid);
            if (track == null)
            {
                JOptionPane.showMessageDialog(null, "Track " + index + " not found");
                return "ERROR";
            }
            return track;
        }
        else if (parent instanceof DefaultMutableTreeNode)
        {
            DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode) parent;
            // If this is the root, return styles
            if (dmtn.toString().equalsIgnoreCase("Tandas"))
            {
                if (mSubsetOfTandas != null && mSubsetOfTandas.size() > 0)
                {
                    out("returning child of subset index " + index);
                    return mSubsetOfTandas.get(index);
                }
                if (index == 0)
                    return tnTango;
                else if (index == 1)
                    return tnValse;
                else if (index == 2)
                    return tnMilonga;
                else
                    return tnOther;
            }
            // If this is a style, return tandas of that style
            else if (dmtn.toString().equalsIgnoreCase("Tango"))
            {
                return getTanda(index, Track.Style.Tango);
            }
            else if (dmtn.toString().equalsIgnoreCase("Milonga"))
            {
                return getTanda(index, Track.Style.Milonga);
            }
            else if (dmtn.toString().equalsIgnoreCase("Valse"))
            {
                return getTanda(index, Track.Style.Valse);
            }
            else if (dmtn.toString().equalsIgnoreCase("Other"))
            {
                return getTanda(index, Track.Style.Other);
            }
        }
        return new DefaultMutableTreeNode("???");
    }

    public boolean isSubsetting()
    {
        if (mSubsetOfTandas != null && mSubsetOfTandas.size() > 0)
            return true;
        return false;
    }

    private Tanda getTanda(int indexIn, Track.Style styleIn)
    {
        int index = 0;
        if (styleIn == Track.Style.Other)
        {
            for (Tanda tanda : mTandas)
            {
                if (tanda.style == Track.Style.Tango || tanda.style == Track.Style.Valse
                        || tanda.style == Track.Style.Milonga)
                    continue;
                if (indexIn == index)
                    return tanda;
                index++;
            }
        }
        else
        {
            for (Tanda tanda : mTandas)
            {
                if (tanda.style == styleIn)
                {
                    if (indexIn == index)
                        return tanda;
                    index++;
                }
            }
        }
        JOptionPane.showMessageDialog(null, "GetTanda(" + index + ") " + styleIn + " fail");
        Tanda tanda = new Tanda("Error");
        return tanda;
    }

    @Override
    public boolean isLeaf(Object node)
    {
        if (isSubsetting())
        {
            if (node instanceof Track)
                return true;
            return false;
        }
        if (node instanceof Tanda)
            return false;
        else if (node instanceof Track)
            return true;
        else
            return false;
    }

    // Insert a track after a specified track in a specified tanda
    public boolean insertTrack(Track newTrack, Tanda tanda, Track locationTrack)
    {
        boolean rc = tanda.insertTrack(newTrack.uniqueId, locationTrack.uniqueId);
        if (rc)
            setChanged(true);
        else
            Utilities.msg("Couldn't insert track");
        return rc;
    }

    // Insert a tanda as the first tanda of it's style
    public int insertTanda(Tanda newTanda)
    {
        int index = 0;
        boolean rc = false;
        if (newTanda.uniqueId == 0l)
            newTanda.uniqueId = getNextUniqueTandaID();
        for (Tanda tanda : mTandas)
        {
            if (tanda.style == newTanda.style)
            {
                index = mTandas.indexOf(tanda);
                mTandas.add(index, newTanda);
                rc = true;
                break;
            }
        }
        if (!rc)
            return -1;
        if (isSubsetting())
        {
            mSubsetOfTandas.add(newTanda);
        }
        return index;
    }

    public Tanda getTandaByUniqueId(long uid)
    {
        for (Tanda tanda : mTandas)
        {
            if (tanda.uniqueId == uid)
                return tanda;
        }
        Utilities.out("TandaTreeModel.getTandaByUniqueID() tanda " + uid + " not found");
        return null;
    }

    public long getSeconds(long UID)
    {
        long seconds = 0;
        Tanda tanda = getTandaByUniqueId(UID);
        for (long lll : tanda.getTrackIDs())
        {
            Track track = mTrackTableModel.getTrackbyUniqueId(lll);
            seconds += track.calculatedTime;
        }
        return seconds;
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue)
    {
        out("TandaTreeModel.valueForPathChanged");
    }

    @Override
    public int getIndexOfChild(Object parent, Object child)
    {
        // out("getIndexOfChild");
        return 0;
    }

    @Override
    public void addTreeModelListener(TreeModelListener l)
    {
        tml.add(l);
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l)
    {
        // TODO Auto-generated method stub
    }

    public void sortAndEliminateDuplicateTandas()
    {
        // mTandas = eliminateDuplicateTandas(mTandas);
        setChanged(true);
        Collections.sort(mTandas);
    }

    private void out(String in)
    {
        System.out.println(in);
    }

    private ArrayList<Tanda> xxxeliminateDuplicateTandas(ArrayList<Tanda> tandas)
    {
        out(tandas.size() + " tandas in");
        TreeMap<String, Tanda> unique = new TreeMap<String, Tanda>();
        Iterator<Tanda> it = tandas.iterator();
        while (it.hasNext())
        {
            Tanda tanda = it.next();
            StringBuffer sb = new StringBuffer();
            long[] tids = tanda.getTrackIDs();
            for (int i = 0; i < tids.length; i++)
                sb.append(tids[i]);
            String key = sb.toString();
            // String key = ""+tanda.id1+tanda.id2+tanda.id3+tanda.id4;
            // Ignores duplicates
            unique.put(key, tanda);
        }
        ArrayList<Tanda> tandasOut = new ArrayList<Tanda>();
        for (Map.Entry<String, Tanda> entry : unique.entrySet())
        {
            String key = entry.getKey();
            Tanda t = entry.getValue();
            tandasOut.add(t);
        }
        out(tandasOut.size() + " tandas out after removing duplicates");
        return tandasOut;
    }
}
