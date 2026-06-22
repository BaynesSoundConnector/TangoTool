package pkgForTTNew;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JOptionPane;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
//import TTDND.Aardvark;

public class PlaylistTreeModel implements TreeModel, TreeModelListener
{
    TandaTreeModel mTandaTreeModel;
    TrackTableModel mTrackTableModel;
    DefaultMutableTreeNode root = new DefaultMutableTreeNode("Playlists");
    ArrayList<Playlist> mPlaylists;
    Vector<TreeModelListener> tml = new Vector<TreeModelListener>();
    Playlist showOnlyThisPlaylist = null;
    int trackBeingShown = -1;
    ArrayList<Playlist> mSubsetPlaylists = null;
    private boolean bChanged = false;

    public PlaylistTreeModel(TrackTableModel trackTableModel, TandaTreeModel tandaTreeModel)
    {
        mTandaTreeModel = tandaTreeModel;
        mTrackTableModel = trackTableModel;
        mPlaylists = new ArrayList<Playlist>();
    }

    public int getPlaylistCount()
    {
        return mPlaylists.size();
    }

    public boolean deleteTanda(Playlist playlist, Tanda tanda)
    {
        boolean rc = playlist.deleteTanda(tanda.uniqueId);
        if (rc)
            bChanged = true;
        return rc;
    }

    public boolean delete(Playlist playlist)
    {
        mPlaylists.remove(playlist);
        bChanged = true;
        return true;
    }

    public boolean isChanged()
    {
        for (Playlist playlist : mPlaylists)
        {
            if (playlist.isChanged())
                return true;
        }
        return bChanged;
    }

    public void setChanged(boolean changed)
    {
        bChanged = changed;
        for (Playlist playlist : mPlaylists)
        {
            playlist.setChanged(changed);
        }
    }

    public void setPlaylists(ArrayList<Playlist> playlists)
    {
        mPlaylists = playlists;
    }

    public boolean addPlaylist(Playlist playlist)
    {
        bChanged = true;
        playlist.uniqueId = getNextPlaylistUID();
        mPlaylists.add(playlist);
        checkForDuplicateTracks(playlist.uniqueId);
        return true;
    }

    public boolean insertNewPlaylistAtTop(Playlist playlist)
    {
        bChanged = true;
        playlist.uniqueId = getNextPlaylistUID();
        mPlaylists.add(0, playlist);
        checkForDuplicateTracks(playlist.uniqueId);
        return true;
    }

    private long getNextPlaylistUID()
    {
        long nextUID = 1l;
        for (Playlist playlist : mPlaylists)
        {
            if (playlist.uniqueId > nextUID)
            {
                nextUID = playlist.uniqueId;
                // break;
            }
            // nextUID++;
        }
        nextUID++;
        return nextUID;
    }

    public boolean write(BufferedWriter writer)
    {
        try
        {
            writer.write("***playlists***\n");
            for (Playlist playlist : mPlaylists)
            {
                String str = playlist.convertToXML();
                writer.write(str + "\n"); // delimiter between tracks
            }
        }
        catch (IOException e)
        {
            out("TrackDatabase.writeOutDatabase() IOException:" + e.getMessage());
        }
        return true;
    }

    public int showTrack(long trackUID)
    {
        if (trackUID == -1)
        {
            mSubsetPlaylists = null;
            return 0;
        }
        mSubsetPlaylists = getSubsetPlaylists(trackUID);
        return mSubsetPlaylists.size();
    }

    private ArrayList<Playlist> getSubsetPlaylists(long trackUID)
    {
        ArrayList<Playlist> subset = new ArrayList<Playlist>();
        Iterator<Playlist> it = mPlaylists.iterator();
        while (it.hasNext())
        {
            Playlist pl = it.next();
            long[] tandaIDs = pl.getTandaIDs();
            for (long tid : tandaIDs)
            {
                Tanda tanda = mTandaTreeModel.getTandaByUniqueId(tid);
                if (tanda == null)
                    continue;
                if (tanda.trackInTanda(trackUID))
                    subset.add(pl);
            }
        }
        return subset;
    }

    public TreePath getPathForPlaylist(Playlist playlist)
    {
        Object[] objs = new Object[2];
        objs[0] = root;
        objs[1] = playlist;
        TreePath path = new TreePath(objs);
        return path;
    }

    @Override
    public Object getRoot()
    {
        return root;
    }

    public boolean showOnlyOnePlaylist(Playlist playlist)
    {
        showOnlyThisPlaylist = playlist;
        notifyTreeModelHasChanged();
        return true;
    }

    public void deleteTanda(long playlistUID, long tandaUID)
    {
        out("deleteTanda()");
        Playlist playlist = getPlaylistByUniqueId(playlistUID);
        playlist.deleteTanda(tandaUID);
        bChanged = true;
    }

    public Playlist getPlaylist(long UID)
    {
        Playlist playlist = null;
        for (int i = 0; i < mPlaylists.size(); i++)
        {
            playlist = mPlaylists.get(i);
            if (playlist.uniqueId == UID)
                break;
        }
        return playlist;
    }
    public boolean checkForDuplicateTracks(long playlistUID)
    {
        Playlist playlist = getPlaylist(playlistUID);
        HashSet<Long> tracks = new HashSet<Long>();
        HashSet<Long> dups = new HashSet<Long>();
        long[] tandas = playlist.getTandaIDs();
        for (long tandaID : tandas)
        {
            Tanda tanda = mTandaTreeModel.getTandaByUniqueId(tandaID);
            long[] trackIDs  = tanda.getTrackIDs();
            for (long trackID : trackIDs)
            {
                if (mTrackTableModel.getTrackbyUniqueId(trackID).style == Track.Style.Cortina)
                    continue;
                if (!tracks.add(trackID))
                    dups.add(trackID);
            }
        }
        if (dups.size() > 0)
        {
            Utilities.msg("There are "+dups.size()+" duplicate tracks in that playlist");
            return false;
        }
        return true;
    }

    public long getSeconds(long UID)
    {
        Playlist playlist = getPlaylist(UID);
        long seconds = 0l;
        long[] tandaIDs = playlist.getTandaIDs();
        for (int i = 0; i < tandaIDs.length; i++)
            seconds += mTandaTreeModel.getSeconds(tandaIDs[i]);
        return seconds;
    }

    private Playlist getPlaylistByUniqueId(long uid)
    {
        Playlist pl = null;
        int plid = 0;
        for (; plid < mPlaylists.size(); plid++)
        {
            pl = mPlaylists.get(plid);
            if (pl.uniqueId == uid)
                break;
        }
        out("Playlist id=" + plid + " " + pl.toString());
        return pl;
    }

    @Override
    public Object getChild(Object parent, int index)
    {
        if (parent.toString().equalsIgnoreCase("Playlists"))
        {
            if (mSubsetPlaylists != null)
            {
                return mSubsetPlaylists.get(index);
            }
            if (showOnlyThisPlaylist != null)
            {
                return showOnlyThisPlaylist;
            }
            return mPlaylists.get(index);
        }
        else if (parent instanceof Playlist)
        {
            Playlist playlist = (Playlist) parent;
            long[] tandas = playlist.getTandaIDs();
            Tanda tanda = mTandaTreeModel.getTandaByUniqueId(tandas[index]);
            if (tanda != null)
                return tanda;
            else
            {
                Tanda mtanda = new Tanda("Missing Tanda");
                // Supply an id so it can be deleted
                mtanda.uniqueId = tandas[index];
                return mtanda;
            }
            // return playlist.getTandas().get(index);
        }
        else if (parent instanceof Tanda)
        {
            Tanda tanda = (Tanda) parent;
            // long uid = tanda.tracks.get(index);
            // Track track = mTrackTableModel.getTrackbyUniqueId(uid);
            long tid = tanda.getTrackID(index);
            Track track = mTrackTableModel.getTrackbyUniqueId(tid);
            if (track == null)
            {
                JOptionPane.showMessageDialog(null, "PlaylistTreeModel.getChild() track " + tid + " is null");
                return "ERROR";
            }
            if (track.orchestra == null)
            {
                track = mTrackTableModel.getTrackbyUniqueId(track.uniqueId);
                if (track == null)
                {
                    String str = "PlaylistTreeModel getChild() null track " + track.uniqueId;
                    JOptionPane.showMessageDialog(null, str);
                    return "";
                }
            }
            return track;
        }
        return "PlaylistTreeModel.getChild() unknown event";
    }

    @Override
    public int getChildCount(Object parent)
    {
        if (parent.toString().equalsIgnoreCase("Playlists"))
        {
            if (showOnlyThisPlaylist != null)
                return 1;
            if (mSubsetPlaylists != null)
                return mSubsetPlaylists.size();
            return mPlaylists.size();
        }
        else if (parent instanceof Playlist)
        {
            Playlist playlist = (Playlist) parent;
            long[] tids = playlist.getTandaIDs();
            return tids.length;
        }
        else if (parent instanceof Tanda)
        {
            Tanda tanda = (Tanda) parent;
            return tanda.getTrackCount();
        }
        return 0;
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

    @Override
    public boolean isLeaf(Object node)
    {
        if (node instanceof Track)
            return true;
        return false;
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue)
    {
        // TODO Auto-generated method stub
    }

    @Override
    public int getIndexOfChild(Object parent, Object child)
    {
        // TODO Auto-generated method stub
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

    @Override
    public void treeNodesChanged(TreeModelEvent e)
    {
        // TODO Auto-generated method stub
    }

    @Override
    public void treeNodesInserted(TreeModelEvent e)
    {
        // TODO Auto-generated method stub
    }

    @Override
    public void treeNodesRemoved(TreeModelEvent e)
    {
        // TODO Auto-generated method stub
    }

    @Override
    public void treeStructureChanged(TreeModelEvent e)
    {
        // TODO Auto-generated method stub
    }

    private void out(String in)
    {
        System.out.println(in);
    }
}
