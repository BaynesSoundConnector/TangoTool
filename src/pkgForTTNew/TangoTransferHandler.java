package pkgForTTNew;

import java.awt.Component;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DropTarget;
import java.io.IOException;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.TransferHandler.DropLocation;
import javax.swing.TransferHandler.TransferSupport;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

public class TangoTransferHandler extends TransferHandler
{
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public TangoTransferHandler()
    {
    }

    public enum SourceTarget
    {
        TrackTableTrack, TandasTree, TandasTreeStyle, TandasTreeStyleTanda, TandasTreeStyleTandaTrack, TandasTreeTanda,
        TandasTreeTandaTrack, PlaylistsTree, PlaylistsTreePlaylist, PlaylistsTreePlaylistTanda,
        PlaylistsTreePlaylistTandaTrack, Unknown
    }

    //
    // Data Export
    //
    // What actions are supported by the source component?
    public int getSourceActions(JComponent component)
    {
        Source source = getSource(component);
        if (source.st == SourceTarget.TandasTree || source.st == SourceTarget.TandasTreeStyle)
            return -1;
        if (source.st == SourceTarget.PlaylistsTree)
            return -1;
        if (source.st == SourceTarget.PlaylistsTreePlaylist)
            return -1;
        if (source.st == SourceTarget.Unknown)
            return -1;
        if (source.st == SourceTarget.TrackTableTrack || source.st == SourceTarget.TandasTreeStyleTanda)
        {
            // out("SourceAction:" + source + " COPY allowed");
            return COPY;
        }
        if (source.st == SourceTarget.TandasTreeStyleTanda || source.st == SourceTarget.TandasTreeTanda)
        {
            return COPY;
        }
        if (source.st == SourceTarget.TandasTreeStyleTandaTrack || source.st == SourceTarget.TandasTreeTandaTrack)
        {
            // out("SourceAction:" + source + " COPY_OR_MOVE allowed");
            return COPY_OR_MOVE;
        }
        if (source.st == SourceTarget.PlaylistsTreePlaylistTanda
                || source.st == SourceTarget.PlaylistsTreePlaylistTandaTrack)
        {
            // out("SourceAction:" + source + " MOVE allowed");
            return MOVE;
        }
        out("SourceAction:" + source + " unknown");
        return -1;
    }

    //
    // Can this target object import data?
    //
    public boolean canImport(TransferSupport support)
    {
        return canImportNew(support);
    }

    private boolean canImportNew(TransferSupport support)
    {
        Source source = getSourceNew(support);
        Target target = getTarget(support);
        out("canImport() Source=" + source.st + ", target=" + target.st);
        switch (target.st)
        {
        case TrackTableTrack:
            return false;
        case TandasTree:
            if (source.st == SourceTarget.TrackTableTrack)
                return true;
            else
                return false;
        case TandasTreeStyle:
            if (source.st == SourceTarget.TrackTableTrack)
                return true;
            return false;
        case TandasTreeStyleTanda:
        case TandasTreeTanda:
            if (source.st == SourceTarget.TrackTableTrack || source.st == SourceTarget.TandasTreeStyleTandaTrack
                    || source.st == SourceTarget.TandasTreeTandaTrack)
                return true;
            return false;
        case TandasTreeStyleTandaTrack:
        case TandasTreeTandaTrack:
            if (source.st == SourceTarget.TrackTableTrack)
                return true;
            if (source.st == SourceTarget.TandasTreeStyleTandaTrack || source.st == SourceTarget.TandasTreeTandaTrack)
                return true;
        case PlaylistsTree:
            if (source.st == SourceTarget.TandasTreeStyleTanda || source.st == SourceTarget.TandasTreeTanda)
                return true;
        case PlaylistsTreePlaylist:
            if (source.st == SourceTarget.TandasTreeStyleTanda || source.st == SourceTarget.TandasTreeTanda)
                return true;
        case PlaylistsTreePlaylistTanda:
            if (source.st == SourceTarget.TandasTreeStyleTanda || source.st == SourceTarget.TandasTreeTanda)
                return true;
            if (source.st == SourceTarget.TrackTableTrack)
                return true;
            if (source.st == SourceTarget.PlaylistsTreePlaylistTanda)
                return true;
        case PlaylistsTreePlaylistTandaTrack:
            if (source.st == SourceTarget.TrackTableTrack)
                return true;
            if (source.st == SourceTarget.PlaylistsTreePlaylistTandaTrack)
                return true;
        default:
            return false;
        }
    }

    private Source getSourceNew(TransferSupport support)
    {
        Source source = new Source();
        source.st = SourceTarget.Unknown;
        String data = getTransferData(support);
        if (data == null || data.equalsIgnoreCase("null"))
            return source;
        String[] sa = data.split(",");
        source.st = SourceTarget.valueOf(sa[0]);
        if (source.st == SourceTarget.TrackTableTrack)
        {
            String[] sa2 = sa[1].split("=");
            source.trackId = Long.parseLong(sa2[1]);
            return source;
        }
        return source;
    }

    private Target getTarget(TransferSupport support)
    {
        Target target = new Target();
        Component targetComponent = support.getComponent();
        target.st = SourceTarget.Unknown;
        DropLocation drop = support.getDropLocation();
        Point point = drop.getDropPoint();
        // Transferable trans = support.getTransferable();
        if (targetComponent instanceof JTable)
        {
            target.st = SourceTarget.TrackTableTrack;
            return target;
        }
        if (targetComponent instanceof JTree)
        {
            JTree tree = (JTree) targetComponent;
            // String treeName = tree.getName();
            DropLocation dl = support.getDropLocation();
            TreePath path = tree.getPathForLocation(point.x, point.y);
            if (path == null)
            {
                Utilities.out("GetTarget(), tree="+tree.getName()+", path=null, target=Unknown");
                target.st = SourceTarget.Unknown;
                return target;
            }
            Utilities.out("GetTarget(), tree="+tree.getName()+", path="+path.toString());
            if (path.toString().equalsIgnoreCase("playlists"))
                Utilities.out("jklhl");
            Object[] objs = path.getPath();
            target.st = getTypeFromPath(objs);
            if (target.st == SourceTarget.TandasTree || target.st == SourceTarget.PlaylistsTree)
                return target;
            if (target.st == SourceTarget.TandasTreeStyle)
                return target;
            if (target.st == SourceTarget.TandasTreeStyleTanda)
            {
                Tanda tanda = (Tanda) objs[2];
                target.tandaId = tanda.uniqueId;
                return target;
            }
            if (target.st == SourceTarget.TandasTreeStyleTandaTrack)
            {
                target.tandaId = ((Tanda) objs[2]).uniqueId;
                target.trackId = ((Track) objs[3]).uniqueId;
                return target;
            }
            if (target.st == SourceTarget.TandasTreeTanda)
            {
                target.tandaId = ((Tanda) objs[1]).uniqueId;
                return target;
            }
            if (target.st == SourceTarget.TandasTreeTandaTrack)
            {
                target.tandaId = ((Tanda) objs[1]).uniqueId;
                target.trackId = ((Track) objs[2]).uniqueId;
                return target;
            }
            if (target.st == SourceTarget.PlaylistsTreePlaylistTandaTrack)
            {
                if (objs.length != 4)
                    Utilities.out("oops");
                target.playlistId = ((Playlist) objs[1]).uniqueId;
                target.tandaId = ((Tanda) objs[2]).uniqueId;
                target.trackId = ((Track) objs[3]).uniqueId;
                return target;
            }
            if (target.st == SourceTarget.PlaylistsTreePlaylistTanda)
            {
                target.playlistId = ((Playlist) objs[1]).uniqueId;
                target.tandaId = ((Tanda) objs[2]).uniqueId;
                return target;
            }
            if (target.st == SourceTarget.PlaylistsTreePlaylist)
            {
                target.playlistId = ((Playlist) objs[1]).uniqueId;
                return target;
            }
        }
        return target;
    }

    // Create a string to transport the data from the source to the target
    // Can only transfer tandas not individual songs. It not a tanda
    // return null
    public Transferable createTransferable(JComponent component)
    {
        Source source = getSource(component);
        if (source.st == SourceTarget.TrackTableTrack)
            return createTrackTransferable(component, source);
        if (source.st == SourceTarget.TandasTreeStyleTanda || source.st == SourceTarget.TandasTreeStyleTandaTrack
                || source.st == SourceTarget.TandasTreeTanda || source.st == SourceTarget.TandasTreeTandaTrack)
            return createTandaTransferable(component);
        if (source.st == SourceTarget.PlaylistsTreePlaylistTanda
                || source.st == SourceTarget.PlaylistsTreePlaylistTandaTrack)
            return createPlaylistTransferable(component, source);
        return new StringSelection("null");
    }

    private Source getSource(JComponent component)
    {
        Source source = new Source();
        source.st = SourceTarget.Unknown;
        if (component instanceof JTable)
        {
            source.st = SourceTarget.TrackTableTrack;
            JTable table = (JTable) component;
            int row = table.getSelectedRow();
            if (row == -1)
                return source;
            int index = table.convertRowIndexToModel(row);
            TrackTableModel model = (TrackTableModel) table.getModel();
            Track track = model.getTrack(index);
            source.trackId = track.uniqueId;
        }
        else if (component instanceof JTree)
        {
            JTree tree = (JTree) component;
            String name = tree.getName();
            TreePath path = tree.getSelectionPath();
            if (path == null)
            {
                if (name.equalsIgnoreCase("tandas"))
                    source.st = SourceTarget.TandasTree;
                else if (name.equalsIgnoreCase("playlists"))
                    source.st = SourceTarget.PlaylistsTree;
                return source;
            }
            Object[] objs = path.getPath();
            source.st = getTypeFromPath(objs);
            if (source.st == SourceTarget.TandasTree || source.st == SourceTarget.TandasTreeStyle
                    || source.st == SourceTarget.PlaylistsTree)
                return source;
            if (source.st == SourceTarget.TandasTreeStyleTanda)
            {
                source.tandaId = ((Tanda) objs[2]).uniqueId;
                return source;
            }
            if (source.st == SourceTarget.TandasTreeStyleTandaTrack)
            {
                source.tandaId = ((Tanda) objs[2]).uniqueId;
                source.trackId = ((Track) objs[3]).uniqueId;
                return source;
            }
            if (source.st == SourceTarget.TandasTreeTanda)
            {
                source.tandaId = ((Tanda) objs[1]).uniqueId;
                return source;
            }
            if (source.st == SourceTarget.TandasTreeTandaTrack)
            {
                source.tandaId = ((Tanda) objs[1]).uniqueId;
                source.trackId = ((Track) objs[2]).uniqueId;
                return source;
            }
            if (source.st == SourceTarget.PlaylistsTreePlaylist)
            {
                source.playlistId = ((Playlist) objs[1]).uniqueId;
                return source;
            }
            if (source.st == SourceTarget.PlaylistsTreePlaylistTanda)
            {
                source.playlistId = ((Playlist) objs[1]).uniqueId;
                source.tandaId = ((Tanda) objs[2]).uniqueId;
                return source;
            }
            if (source.st == SourceTarget.PlaylistsTreePlaylistTandaTrack)
            {
                source.playlistId = ((Playlist) objs[1]).uniqueId;
                source.tandaId = ((Tanda) objs[2]).uniqueId;
                source.trackId = ((Track) objs[3]).uniqueId;
                return source;
            }
        }
        return source;
    }

    // Bring the data into the data model. If this is coming from another tree,
    // just being in the data. If the source is the same as the target (drag and
    // drop) then delete from the source
    public boolean importData(TransferSupport support)
    {
        if (!canImport(support))
            return false;
        Component targetComponent = support.getComponent();
        // Import into the tandas tree from the track table or into the playlists tree
        // from the tandas tree.
        if (targetComponent instanceof JTree)
        {
            JTree tree = (JTree) targetComponent;
            String name = tree.getName();
            if (name.equalsIgnoreCase("tandas"))
                return importToTandasTree(support);
            else if (name.equalsIgnoreCase("playlists"))
                return importToPlaylistsTreeNew(support);
        }
        return false;
    }

    // Remove data from source when complete. Only need to do this when
    // reordering the right tree. If the source is the left tree then
    // it's always a copy not move, so no need to remove anything.
    protected void exportDone(JComponent component, Transferable t, int action)
    {
        if (component instanceof JTree)
        {
            JTree tree = (JTree) component;
            try
            {
                Object obj = t.getTransferData(DataFlavor.stringFlavor);
                String trans = (String) obj;
                out("exportDone(), tree=" + tree.getName() + ", transferable=" + trans);
            }
            catch (UnsupportedFlavorException | IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return;
    }

    private Transferable createTrackTransferable(Component component, Source source)
    {
        JTable table = (JTable) component;
        //
        int row = table.getSelectedRow();
        int index = table.convertRowIndexToModel(row);
        //int test = table.convertRowIndexToView(row);
        Object obj = table.getModel().getValueAt(row, 6);
        Object obj2 = table.getModel().getValueAt(index, 6);
        //Object obj3 = table.getModel().getValueAt(test, 6);
        long UID = ((Long)obj2).longValue();
        //
        //int row = table.getSelectedRow();
        //int index = table.convertRowIndexToModel(row);
        TrackTableModel model = (TrackTableModel) table.getModel();
        //Track track = model.getTrackbyUniqueId(UID);
        // String UID = Long.toString(track.uniqueId);
        StringBuffer sb = new StringBuffer();
        sb.append(source.st.toString() + ",");
        sb.append("track=" + UID);
        out("TrackTransferable:" + sb.toString());
        return new StringSelection(sb.toString());
    }

    private Transferable createTandaTransferable(Component component)
    {
        JTree tree = (JTree) component;
        TreePath tp = tree.getSelectionPath();
        Object[] path = tp.getPath();
        SourceTarget sourceType = getTypeFromPath(path);
        StringBuffer sb = new StringBuffer();
        if (sourceType == SourceTarget.TandasTreeTanda)
        {
            Tanda tanda = (Tanda) path[1];
            sb.append(sourceType.toString() + "," + "tanda=" + tanda.uniqueId);
            return new StringSelection(sb.toString());
        }
        if (sourceType == SourceTarget.TandasTreeTandaTrack)
        {
            Tanda tanda = (Tanda) path[1];
            Track track = (Track) path[2];
            sb.append(sourceType.toString() + "," + "tanda=" + tanda.uniqueId);
            sb.append(",track=" + track.uniqueId);
            return new StringSelection(sb.toString());
        }
        // Regular processing
        if (sourceType == SourceTarget.TandasTreeStyleTanda)
        {
            Tanda tanda = (Tanda) path[2];
            sb.append(sourceType.toString() + "," + "tanda=" + tanda.uniqueId);
            return new StringSelection(sb.toString());
        }
        if (sourceType == SourceTarget.TandasTreeStyleTandaTrack)
        {
            Tanda tanda = (Tanda) path[2];
            Track track = (Track) path[3];
            sb.append(sourceType.toString() + "," + "tanda=" + tanda.uniqueId);
            sb.append(",track=" + track.uniqueId);
            return new StringSelection(sb.toString());
        }
        return new StringSelection("");
    }

    private Transferable createTandaTransferableOld(Component component, Source source)
    {
        JTree tree = (JTree) component;
        TreePath tp = tree.getSelectionPath();
        Object[] path = tp.getPath();
        SourceTarget type = getTypeFromPath(path);
        StringBuffer sb = new StringBuffer();
        // Subset processing
        if (path[1] instanceof Tanda)
        {
            Tanda tanda = (Tanda) path[1];
            if (source.st == SourceTarget.TandasTreeTanda)
            {
                sb.append(source.st.toString() + "," + "tanda=" + tanda.uniqueId);
                return new StringSelection(sb.toString());
            }
            else if (source.st == SourceTarget.TandasTreeTandaTrack)
            {
                Track track = (Track) path[2];
                sb.append(source.st.toString() + "," + "tanda=" + tanda.uniqueId);
                sb.append(",track=" + track.uniqueId);
                return new StringSelection(sb.toString());
            }
            else
                out("Strange error");
            return new StringSelection("Error");
        }
        // Regular processing
        Tanda tanda = (Tanda) path[2];
        if (source.st == SourceTarget.TandasTreeStyleTanda)
        {
            sb.append(source.st.toString() + "," + "tanda=" + tanda.uniqueId);
            return new StringSelection(sb.toString());
        }
        else if (source.st == SourceTarget.TandasTreeStyleTandaTrack)
        {
            Track track = (Track) path[3];
            sb.append(source.st.toString() + "," + "tanda=" + tanda.uniqueId);
            sb.append(",track=" + track.uniqueId);
            return new StringSelection(sb.toString());
        }
        return new StringSelection("");
    }

    private Transferable createPlaylistTransferable(Component component, Source source)
    {
        JTree tree = (JTree) component;
        TreePath tp = tree.getSelectionPath();
        Object[] path = tp.getPath();
        Playlist playlist = (Playlist) path[1];
        Tanda tanda = (Tanda) path[2];
        StringBuffer sb = new StringBuffer();
        sb.append(source.st.toString() + ",");
        sb.append("playlist=" + playlist.uniqueId + ",");
        sb.append("tanda=" + tanda.uniqueId);
        if (source.st == SourceTarget.PlaylistsTreePlaylistTanda)
        {
            return new StringSelection(sb.toString());
        }
        if (source.st == SourceTarget.PlaylistsTreePlaylistTandaTrack)
        {
            Track track = (Track) path[3];
            sb.append(",track=" + track.uniqueId);
            return new StringSelection(sb.toString());
        }
        return new StringSelection("");
    }

    private String getTransferData(TransferSupport support)
    {
        Transferable trans = support.getTransferable();
        try
        {
            Object obj = trans.getTransferData(DataFlavor.stringFlavor);
            if (obj instanceof String)
            {
                return (String) obj;
            }
            else
            {
                out("yikes!");
                return null;
            }
        }
        catch (UnsupportedFlavorException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    private boolean importToTandasTreeNew(TransferSupport support)
    {
        JTree tandasTree = (JTree) support.getComponent();
        // TandaTreeModel tandaTreeModel = (TandaTreeModel) tandasTree.getModel();
        // var transferable = support.getTransferable();
        String transferable = getTransferData(support);
        // String str = getTransferableString(transferable);
        // Parse the transferable
        String[] transferableParts = transferable.split(",");
        SourceTarget sourceType;
        sourceType = SourceTarget.valueOf(transferableParts[0]);
        // String sourceType = transferableParts[0];
        DropLocation location = tandasTree.getDropLocation();
        Point dropPoint = location.getDropPoint();
        TreePath path = tandasTree.getClosestPathForLocation(dropPoint.x, dropPoint.y);
        Object[] targetPath = path.getPath();
        SourceTarget targetType = getTypeFromPath(targetPath);
        out("importToTandasTree() source=" + sourceType + " target=" + path);
        if (sourceType == SourceTarget.TrackTableTrack)
            return moveTrackToTandaTree(transferableParts, targetPath);
        return true;
    }

    private SourceTarget getTypeFromPath(Object[] path)
    {
        DefaultMutableTreeNode root;
        if (!(path[0] instanceof DefaultMutableTreeNode))
            return SourceTarget.Unknown;
        root = (DefaultMutableTreeNode) path[0];
        String name = root.toString();
        if (name.equalsIgnoreCase("tandas"))
        {
            if (path.length == 1)
                return SourceTarget.TandasTree;
            if (path.length == 2)
            {
                if (path[1] instanceof Tanda)
                    return SourceTarget.TandasTreeTanda;
                else
                    return SourceTarget.TandasTreeStyle;
            }
            if (path.length == 3)
            {
                if (path[1] instanceof Tanda)
                    return SourceTarget.TandasTreeTandaTrack;
                else
                    return SourceTarget.TandasTreeStyleTanda;
            }
            if (path.length == 4)
                return SourceTarget.TandasTreeStyleTandaTrack;
        }
        // Playlist Tree
        else
        {
            if (path.length == 1)
                return SourceTarget.PlaylistsTree;
            if (path.length == 2)
                return SourceTarget.PlaylistsTreePlaylist;
            if (path.length == 3)
                return SourceTarget.PlaylistsTreePlaylistTanda;
            if (path.length == 4)
                return SourceTarget.PlaylistsTreePlaylistTandaTrack;
        }
        return SourceTarget.Unknown;
    }

    private boolean moveTrackToTandaTree(String[] transferableParts, Object[] targetPath)
    {
        String[] util = transferableParts[1].split("=");
        util = transferableParts[2].split("=");
        Long sourceTrackUID = Long.parseLong(util[1]);
        if (targetPath.length == 1)
            return createNewTanda(sourceTrackUID);
        if (targetPath.length == 2)
        {
            if (targetPath[1] instanceof Tanda)
                return insertTrackIntoTanda(sourceTrackUID, (Tanda) targetPath[1]);
            else
                return createNewTandaOfType(sourceTrackUID, ((DefaultMutableTreeNode) targetPath[1]).toString());
        }
        return true;
    }

    private boolean createNewTandaOfType(long sourceTrackUID, String tandaType)
    {
        // Track newTrack = tandaTreeModel.mTrackTableModel.getTrackbyUniqueId(UID);
        // Tanda tanda = new Tanda();
        return true;
    }

    private boolean insertTrackIntoTanda(long sourceTrackUID, Tanda targetTanda)
    {
        return true;
    }

    // Track dropped on tanda tree root. Create a new tanda of type based on track
    // type
    private boolean createNewTanda(long sourceTrackUID)
    {
        return true;
    }

    private TreePath dropTrackOnTandasTree(String transferable, TandaTreeModel tandaTreeModel, Object[] targetPath)
    {
        String[] transferableParts = transferable.split(",");
        String[] util = transferableParts[1].split("=");
        Long trackId = Long.parseLong(util[1]);
        Track track = tandaTreeModel.mTrackTableModel.getTrackbyUniqueId(trackId);
        Tanda tanda = new Tanda(track.orchestra);
        tanda.addTrackID(trackId);
        tanda.style = track.style;
        int rc = tandaTreeModel.insertTanda(tanda);
        if (rc == -1)
        {
            
        }
        Object[] newPath;
        if (!tandaTreeModel.isSubsetting())
        {
            newPath = new Object[targetPath.length + 3];
            newPath[0] = targetPath[0];
            if (tanda.style == Track.Style.Tango)
                newPath[1] = tandaTreeModel.tnTango;
            if (tanda.style == Track.Style.Valse)
                newPath[1] = tandaTreeModel.tnValse;
            if (tanda.style == Track.Style.Milonga)
                newPath[1] = tandaTreeModel.tnMilonga;
            if (tanda.style == Track.Style.Other)
                newPath[1] = tandaTreeModel.tnOther;
            newPath[2] = tanda;
            newPath[3] = track;
        }
        else
        {
            newPath = new Object[targetPath.length + 2];
            newPath[0] = targetPath[0];
            newPath[1] = tanda;
            newPath[2] = track;
        }
        TreePath tp = new TreePath(newPath);
        return tp;
    }

    private TreePath dropTrackOnTandasTreeStyle(String transferable, TandaTreeModel tandaTreeModel, Object[] targetPath)
    {
        String[] transferableParts = transferable.split(",");
        String[] util = transferableParts[1].split("=");
        Long trackId = Long.parseLong(util[1]);
        Track track = tandaTreeModel.mTrackTableModel.getTrackbyUniqueId(trackId);
        Tanda tanda = new Tanda(track.orchestra);
        tanda.addTrackID(trackId);
        tanda.style = track.style;
        tandaTreeModel.insertTanda(tanda);
        Object[] newPath = new Object[targetPath.length + 1];
        newPath[0] = targetPath[0];
        if (tanda.style == Track.Style.Tango)
            newPath[1] = tandaTreeModel.tnTango;
        if (tanda.style == Track.Style.Valse)
            newPath[1] = tandaTreeModel.tnValse;
        if (tanda.style == Track.Style.Milonga)
            newPath[1] = tandaTreeModel.tnMilonga;
        if (tanda.style == Track.Style.Other)
            newPath[1] = tandaTreeModel.tnOther;
        newPath[2] = tanda;
        TreePath tp = new TreePath(newPath);
        return tp;
    }

    private TreePath dropTrackOnTandasTreeStyleTanda(String transferable, TandaTreeModel tandaTreeModel,
            Object[] targetPath)
    {
        String[] transferableParts = transferable.split(",");
        String[] util = transferableParts[1].split("=");
        Long trackId = Long.parseLong(util[1]);
        Track track = tandaTreeModel.mTrackTableModel.getTrackbyUniqueId(trackId);
        Tanda tanda = (Tanda) targetPath[2];
        tanda.insertTrackAtTop(trackId);
        Object[] newPath = new Object[targetPath.length];
        newPath[0] = targetPath[0];
        newPath[1] = targetPath[1];
        newPath[2] = targetPath[2];
        TreePath tp = new TreePath(newPath);
        return tp;
    }

    private void scrollToPathAndExpand(TreePath tp, JTree tandasTree, TandaTreeModel tandaTreeModel)
    {
        tandaTreeModel.notifyTreeModelHasChanged();
        tandaTreeModel.mTrackTableModel.fireTableDataChanged();
        tandasTree.expandPath(tp);        
        tandasTree.scrollPathToVisible(tp);
    }

    // We can import to the tandas tree only from the track table
    // or from within the tanda tree
    private boolean importToTandasTree(TransferSupport support)
    {
        JTree tandasTree = (JTree) support.getComponent();
        TandaTreeModel tandaTreeModel = (TandaTreeModel) tandasTree.getModel();
        // TrackTableModel trackTableModel = tandaTreeModel.mTrackTableModel;
        // var transferable = support.getTransferable();
        String transferable = getTransferData(support);
        // String str = getTransferableString(transferable);
        // Parse the transferable
        String[] transferableParts = transferable.split(",");
        SourceTarget sourceType = SourceTarget.valueOf(transferableParts[0]);
        // String sourceType = transferableParts[0];
        DropLocation location = tandasTree.getDropLocation();
        Point dropPoint = location.getDropPoint();
        TreePath path = tandasTree.getClosestPathForLocation(dropPoint.x, dropPoint.y);
        Object[] targetPath = path.getPath();
        SourceTarget targetType = getTypeFromPath(targetPath);
        // Moving tracks within the tanda tree
        if (sourceType == SourceTarget.TandasTreeStyleTandaTrack || sourceType == SourceTarget.TandasTreeTandaTrack)
        {
            String[] util = transferableParts[1].split("=");
            Tanda sourceTanda = tandaTreeModel.getTandaByUniqueId(Long.parseLong(util[1]));
            util = transferableParts[2].split("=");
            Long sourceTrackId = Long.parseLong(util[1]);
            // moveTandaTracks(sourceTanda, sourceTrack, targetPath);
            moveTandaTracks(sourceTanda, sourceTrackId, targetPath);
            tandaTreeModel.notifyTreeModelHasChanged();
            // TreePath newPath = path.getParentPath();
            tandasTree.expandPath(path);
            tandasTree.scrollPathToVisible(path);
            return true;
        }
        //
        // Source is a track from the track table
        // SourceTarget.TrackTableTrack
        //
        // Drop a track on Tandas tree root or on Tandas->Style
        if (targetType == SourceTarget.TandasTree)
        {
            TreePath tp = dropTrackOnTandasTree(transferable, tandaTreeModel, targetPath);
            scrollToPathAndExpand(tp, tandasTree, tandaTreeModel);
            return true;
        }
        else if (targetType == SourceTarget.TandasTreeStyle)
        {
            TreePath tp = dropTrackOnTandasTreeStyle(transferable, tandaTreeModel, targetPath);
            scrollToPathAndExpand(tp, tandasTree, tandaTreeModel);
            return true;
        }
        else if (targetType == SourceTarget.TandasTreeStyleTanda)
        {
            TreePath tp = dropTrackOnTandasTreeStyleTanda(transferable, tandaTreeModel, targetPath);
        }
        else if (targetType == SourceTarget.TandasTreeStyleTandaTrack)
            Utilities.out(SourceTarget.TandasTreeStyleTandaTrack.toString());
        else if (targetType == SourceTarget.TandasTreeTanda)
            Utilities.out(SourceTarget.TandasTreeTanda.toString());
        else if (targetType == SourceTarget.TandasTreeTandaTrack)
            Utilities.out(SourceTarget.TandasTreeTandaTrack.toString());
        if (targetPath.length == 1 || targetPath.length == 2)
        {
            String[] kv1 = transferable.split(",");
            String[] kv2 = kv1[1].split("=");
            long UID = Long.parseLong(kv2[1]);
            Track newTrack = tandaTreeModel.mTrackTableModel.getTrackbyUniqueId(UID);
            Tanda newTanda = new Tanda(null);
            newTanda.addTrackID(UID);
            newTanda.style = newTrack.style;
            newTanda.orchestra = newTrack.orchestra;
            // newTanda.uniqueId = tandaTreeModel.getNextAvailableTandaUID();
            // Insert tanda as first of it's style (top of style)
            int index = tandaTreeModel.insertTanda(newTanda);
            Utilities.out("Inserted tanda at index=" + index);
            Object[] newPath = new Object[3];
            newPath[0] = targetPath[0];
            newPath[1] = tandaTreeModel.getTandaTypeNode(newTrack.style);
            newPath[2] = newTanda;
            TreePath tp = new TreePath(newPath);
            tandasTree.expandPath(tp);
            tandaTreeModel.notifyTreeModelHasChanged();
            tandaTreeModel.mTrackTableModel.fireTableDataChanged();
            tandasTree.scrollPathToVisible(tp);
            return true;
        }
        // Drop a track on an existing tanda
        Tanda targetTanda = null;
        Track targetTrack = null;
        String[] kv1 = transferable.split(",");
        String[] kv2 = kv1[1].split("=");
        long newTrackUID = Long.parseLong(kv2[1]);
        if (targetPath.length == 3)
        {
            // subset processing
            if (targetPath[1] instanceof Tanda)
            {
                targetTanda = (Tanda) targetPath[1];
                targetTrack = (Track) targetPath[2];
                targetTanda.insertTrack(newTrackUID, targetTrack.uniqueId);
            }
            else
            {
                targetTanda = (Tanda) targetPath[2];
                targetTanda.insertTrackAtTop(newTrackUID);
            }
            tandaTreeModel.notifyTreeModelHasChanged();
            //TreePath path = tandaTreeModel.getPathForTanda(targetTanda);
            tandasTree.expandPath(path);
            tandasTree.scrollPathToVisible(path);
            return true;
        }
        // Drop a track on a track in a tanda
        else if (targetPath.length == 4)
        {
            targetTanda = (Tanda) targetPath[2];
            Track track = (Track) targetPath[3];
            String[] util = transferableParts[1].split("=");
            newTrackUID = Long.parseLong(util[1]);
            // Track newTrack =
            // tandaTreeModel.mTrackTableModel.getTrackbyUniqueId(newTrackUID);
            if (!targetTanda.insertTrack(newTrackUID, track.uniqueId))
            {
                JOptionPane.showMessageDialog(null, "Cannot insert duplicate track");
                return false;
            }
            tandaTreeModel.notifyTreeModelHasChanged();
            TreePath newPath = tandaTreeModel.getPathForTanda(targetTanda);
            tandasTree.expandPath(newPath);
            tandasTree.scrollPathToVisible(newPath);
        }
        return true;
    }

    private boolean moveTandaTracks(Tanda sourceTanda, Long sourceTrackId, Object[] targetPath)
    {
        // Moving to top of current tanda
        Tanda targetTanda = null;
        Track targetTrack = null;
        // Standard Tandas/TandaType/Tanda/Track processing
        if (targetPath.length == 4)
        {
            targetTanda = (Tanda) targetPath[2];
            targetTrack = (Track) targetPath[3];
        }
        else if (targetPath.length == 3)
        {
            targetTanda = (Tanda) targetPath[1];
            targetTrack = (Track) targetPath[2];
        }
        else if (targetPath.length == 2)
        {
            targetTanda = (Tanda) targetPath[1];
        }
        // Move within a tanda
        if (sourceTanda.uniqueId == targetTanda.uniqueId)
        {
            if (targetTrack == null)
                targetTanda.moveTrackToTop(sourceTrackId);
            else
            {
                if (targetTrack.uniqueId == sourceTrackId)
                    return false;
                targetTanda.moveTrack(sourceTrackId, targetTrack.uniqueId);
            }
        }
        // Move from one tanda to another
        else
        {
            boolean rc;
            if (targetTrack == null)
                rc = targetTanda.insertTrackAtTop(sourceTrackId);
            else
                rc = targetTanda.insertTrack(sourceTrackId, targetTrack.uniqueId);
            if (rc != true)
                JOptionPane.showMessageDialog(null, "Track already exists");
        }
        return true;
    }

    private long extractLong(String str, String tok)
    {
        String[] kv = str.split("=");
        int index = str.indexOf(tok);
        String s1 = str.substring(index + tok.length(), index + 4);
        return Long.parseLong(s1);
    }
    /*
     * private void xbuildNewTanda(String str, String style, JTree tandasTree,
     * TreePath path) { long trackId = Long.parseLong(str.substring(6));
     * TandaTreeModel tandaTreeModel = (TandaTreeModel) tandasTree.getModel(); Tanda
     * newTanda = new Tanda(null); newTanda.uniqueId =
     * tandaTreeModel.getNextAvailableTandaUID(); Track track =
     * tandaTreeModel.mTrackTableModel.getTrackbyUniqueId(trackId);
     * newTanda.addTrackID(trackId); newTanda.orchestra = track.orchestra;
     * newTanda.style = track.style; tandaTreeModel.addNewTanda(newTanda);
     * tandaTreeModel.notifyTreeModelHasChanged(); TreePath newPath =
     * tandaTreeModel.getPathForTanda(newTanda); tandasTree.expandPath(newPath);
     * tandasTree.scrollPathToVisible(newPath); }
     */

    private Source parseSourceTransferable(String str)
    {
        Source source = new Source();
        String[] transferables = str.split(",");
        source.st = SourceTarget.valueOf(transferables[0]);
        if (transferables.length == 2)
        {
            String[] util = transferables[1].split("=");
            if (util[0].equalsIgnoreCase("tanda"))
                source.tandaId = Long.parseLong(util[1]);
            else if (util[0].equalsIgnoreCase("track"))
                source.trackId = Long.parseLong(util[1]);
        }
        else if (transferables.length == 3)
        {
            String[] pl = transferables[1].split("=");
            String[] tn = transferables[2].split("=");
            source.playlistId = Long.parseLong(pl[1]);
            source.tandaId = Long.parseLong(tn[1]);
        }
        else if (transferables.length == 4)
        {
            String[] pl = transferables[1].split("=");
            String[] tn = transferables[2].split("=");
            String[] tk = transferables[3].split("=");
            source.playlistId = Long.parseLong(pl[1]);
            source.tandaId = Long.parseLong(tn[1]);
            source.trackId = Long.parseLong(tk[1]);
        }
        return source;
    }

    private Object[] getTargetPath(JTree tree)
    {
        Object[] out = null;
        DropLocation location = tree.getDropLocation();
        Point dropPoint = location.getDropPoint();
        DropTarget dropTarget = tree.getDropTarget();
        TreePath path = tree.getClosestPathForLocation(dropPoint.x, dropPoint.y);
        out = path.getPath();
        return out;
    }

    private boolean importToPlaylistsTreeNew(TransferSupport support)
    {
        Target target = getTarget(support);
        String str = getTransferableString(support);
        Source source = parseSourceTransferable(str);
        JTree playlistsTree = (JTree) support.getComponent();
        Object[] targetPath = getTargetPath(playlistsTree);
        PlaylistTreeModel model = (PlaylistTreeModel) playlistsTree.getModel();
        // Import a tanda to the playlists tree, creating a new playlist
        if (target.st == SourceTarget.PlaylistsTree)
        {
            String inputValue = JOptionPane.showInputDialog("New playlist name:");
            if (inputValue == null)
                return false;
            Playlist playlist = new Playlist(inputValue);
            // playlist.uniqueId = model.getNextAvailablePlaylistUID();
            // Tanda tanda = model.mTandaTreeModel.getTandaByUniqueId(source.tandaId);
            playlist.addTandaID(source.tandaId);
            model.insertNewPlaylistAtTop(playlist);
            // model.mPlaylists.add(0, playlist);
            model.notifyTreeModelHasChanged();
            TreePath newPath = model.getPathForPlaylist(playlist);
            playlistsTree.expandPath(newPath);
            playlistsTree.scrollPathToVisible(newPath);
            return true;
        }
        if (target.st == SourceTarget.PlaylistsTreePlaylist)
        {
            // Import a tanda from tandas tree to playlist
            if (source.st == SourceTarget.TandasTreeStyleTanda)
            {
                Playlist targetPlaylist = model.getPlaylist(target.playlistId);
                boolean rc = targetPlaylist.insertTandaAtTop(source.tandaId);
                if (!rc)
                {
                    Utilities.msg("Insert failed");
                    return false;
                }
                model.notifyTreeModelHasChanged();
                TreePath newPath = model.getPathForPlaylist(targetPlaylist);
                playlistsTree.expandPath(newPath);
                playlistsTree.scrollPathToVisible(newPath);
                return true;
            }
            else if (source.st == SourceTarget.TandasTreeTanda)
            {
                Playlist targetPlaylist = model.getPlaylist(target.playlistId);
                targetPlaylist.insertTandaAtTop(source.tandaId);
                //targetPlaylist.insertTandaAtTop(model.mTandaTreeModel.getTandaByUniqueId(source.tandaId));
                model.notifyTreeModelHasChanged();
                TreePath newPath = model.getPathForPlaylist(targetPlaylist);
                playlistsTree.expandPath(newPath);
                playlistsTree.scrollPathToVisible(newPath);
                return true;
            }
            // Import a tanda at the top of the specified playlist
            if (source.st == SourceTarget.PlaylistsTreePlaylistTanda)
            {
                Tanda tanda = model.mTandaTreeModel.getTandaByUniqueId(source.tandaId);
                Playlist playlist = (Playlist) targetPath[1];
                playlist.insertTandaAtTop(tanda.uniqueId);
                Object[] objs2 = new Object[3];
                objs2[0] = targetPath[0];
                objs2[1] = targetPath[1];
                objs2[2] = tanda;
                model.notifyTreeModelHasChanged();
                TreePath newPath = new TreePath(objs2);
                playlistsTree.expandPath(newPath);
                playlistsTree.scrollPathToVisible(newPath);
            }
        }
        // Import a tanda, place below target tanda
        if (target.st == SourceTarget.PlaylistsTreePlaylistTanda)
        {
            if (targetPath.length == 3)
            {
                if (source.st == SourceTarget.TandasTreeStyleTanda)
                {
                    Tanda tanda = (Tanda) targetPath[2];
                    // long newTandaUID = Long.parseLong(kv2[1]);
                    Playlist playlist = (Playlist) targetPath[1];
                    //Tanda newTanda = model.mTandaTreeModel.getTandaByUniqueId(source.tandaId);
                    boolean rc = playlist.insertTanda(source.tandaId, tanda.uniqueId);
                    if (!rc)
                    {
                        Utilities.msg("Can't insert tanda");
                        return false;
                    }
                    model.notifyTreeModelHasChanged();
                    Object[] expandPath = getParentPath(targetPath);
                    playlistsTree.expandPath(new TreePath(expandPath));
                    playlistsTree.scrollPathToVisible(new TreePath(expandPath));
                }
                else if (source.st == SourceTarget.TandasTreeTanda)
                {
                    Tanda targetTanda = (Tanda) targetPath[2];
                    // long newTandaUID = Long.parseLong(kv2[1]);
                    Playlist playlist = (Playlist) targetPath[1];
                    //Tanda newTanda = model.mTandaTreeModel.getTandaByUniqueId(source.tandaId);
                    boolean rc = playlist.insertTanda(source.tandaId, targetTanda.uniqueId);
                    if (!rc)
                    {
                        Utilities.msg("Can't insert");
                        return false;
                    }
                    //playlist.insertTanda(source.tandaId, tanda.uniqueId);
                    model.notifyTreeModelHasChanged();
                    Object[] expandPath = getParentPath(targetPath);
                    playlistsTree.expandPath(new TreePath(expandPath));
                    playlistsTree.scrollPathToVisible(new TreePath(expandPath));
                }
                else if (source.st.equals(SourceTarget.PlaylistsTreePlaylistTandaTrack))
                // else if (kv2[0].equalsIgnoreCase("track"))
                {
                    Playlist playlist = (Playlist) targetPath[1];
                    Tanda tanda = (Tanda) targetPath[2];
                    if (tanda.uniqueId == source.tandaId)
                    {
                        tanda.moveTrackToTop(source.trackId);
                    }
                    // long UID = Long.parseLong(kv2[1]);
                    // Track newTrack = model.mTrackTableModel.getTrackbyUniqueId(source.trackId);
                    else if (!tanda.insertTrackAtTop(source.trackId))
                    {
                        JOptionPane.showMessageDialog(null, "Cannot insert duplicate track");
                        return false;
                    }
                    model.notifyTreeModelHasChanged();
                    // TreePath newPath = model.getPathForPlaylist(playlist);
                    TreePath path = new TreePath(targetPath);
                    playlistsTree.expandPath(path);
                    playlistsTree.scrollPathToVisible(path);
                }
                else if (source.st == SourceTarget.PlaylistsTreePlaylistTanda)
                // else if (kv3 != null && kv3[0].equalsIgnoreCase("tanda"))
                {
                    // Move tanda kv2[1] to tanda objs[2]
                    Tanda toTanda = (Tanda) targetPath[2];
                    Playlist playlist = (Playlist) targetPath[1];
                    // long UID = Long.parseLong(kv3[1]);
                    // Tanda fromTanda = playlist.getTanda(source.tandaId);
                    playlist.moveTanda(source.tandaId, toTanda.uniqueId);
                    TreePath thePath = new TreePath(targetPath);
                    TreePath parentPath = thePath.getParentPath();
                    model.notifyTreeModelHasChanged();
                    playlistsTree.expandPath(parentPath);
                    playlistsTree.scrollPathToVisible(parentPath);
                }
                else if (source.st == SourceTarget.TrackTableTrack)
                {
                    long trackUID = source.trackId;
                    Tanda targetTanda = (Tanda)targetPath[2];
                    boolean rc = targetTanda.insertTrackAtTop(trackUID);
                    model.notifyTreeModelHasChanged();
                    TreePath thePath = new TreePath(targetPath);
                    TreePath parentPath = thePath.getParentPath();
                    model.notifyTreeModelHasChanged();
                    playlistsTree.expandPath(parentPath);
                    playlistsTree.scrollPathToVisible(parentPath);
                }
            }
        }
        if (target.st == SourceTarget.PlaylistsTreePlaylistTandaTrack)
        {
            DropLocation location = playlistsTree.getDropLocation();
            Point dropPoint = location.getDropPoint();
            TreePath path = playlistsTree.getClosestPathForLocation(dropPoint.x, dropPoint.y);
            Object[] objs = path.getPath();
            if (objs.length == 3)
            {
                Tanda tanda = (Tanda) objs[2];
                // long UID = Long.parseLong(kv2[1]);
                // Track newTrack = model.mTrackTableModel.getTrackbyUniqueId(source.trackId);
                if (!tanda.insertTrackAtTop(source.trackId))
                {
                    JOptionPane.showMessageDialog(null, "Cannot insert duplicate track");
                    return false;
                }
                model.notifyTreeModelHasChanged();
                playlistsTree.expandPath(path);
                playlistsTree.scrollPathToVisible(path);
            }
            if (objs.length == 4)
            {
                Tanda targetTanda = (Tanda) objs[2];
                Track targetTrack = (Track) objs[3];
                Playlist targetPlaylist = (Playlist) objs[1];
                // Are we moving the track within the tanda?
                if (targetTanda.uniqueId == source.tandaId)
                {
                    boolean rc =targetTanda.moveTrack(source.trackId, targetTrack.uniqueId);
                    if (rc)
                    {
                        model.notifyTreeModelHasChanged();
                        model.mTandaTreeModel.notifyTreeModelHasChanged();
                        playlistsTree.expandPath(path);
                        playlistsTree.scrollPathToVisible(path); 
                        return true;
                    }
                    else
                        Utilities.msg("Cannot move track");
                    return false;
                }
                // long UID = Long.parseLong(kv2[1]);
                // Track newTrack = model.mTrackTableModel.getTrackbyUniqueId(source.trackId);
                if (!targetTanda.insertTrack(source.trackId, targetTrack.uniqueId))
                {
                    JOptionPane.showMessageDialog(null, "Cannot insert duplicate track");
                    return false;
                }
                model.checkForDuplicateTracks(targetPlaylist.uniqueId);
                model.notifyTreeModelHasChanged();
                playlistsTree.expandPath(path);
                playlistsTree.scrollPathToVisible(path);
                return true;
            }
        }
        return false;
    }

    Object[] getParentPath(Object[] in)
    {
        Object[] out = new Object[in.length - 1];
        for (int i = 0; i < out.length; i++)
            out[i] = in[i];
        return out;
    }

    TreePath getParentPath(TreePath tpIn)
    {
        Object[] in = tpIn.getPath();
        Object[] out = getParentPath(in);
        TreePath pathOut = new TreePath(out);
        return pathOut;
    }

    // We can import tandas to the playlists tree
    private boolean importToPlaylistsTree(TransferSupport support)
    {
        String str = getTransferableString(support);
        if (str.startsWith("playlist="))
            return moveTandaWithinPlaylist(support, str);
        JTree playlistsTree = (JTree) support.getComponent();
        long tandaUID = Long.parseLong(str.substring(6));
        PlaylistTreeModel playlistTreeModel = (PlaylistTreeModel) playlistsTree.getModel();
        Tanda tandaSource = playlistTreeModel.mTandaTreeModel.getTandaByUniqueId(tandaUID);
        out("Import:" + tandaSource.orchestra);
        // Build the new object to be inserted
        PlaylistTreeModel playlistModel = (PlaylistTreeModel) playlistsTree.getModel();
        DropLocation location = playlistsTree.getDropLocation();
        Point dropPoint = location.getDropPoint();
        DropTarget dropTarget = playlistsTree.getDropTarget();
        TreePath path = playlistsTree.getClosestPathForLocation(dropPoint.x, dropPoint.y);
        Object[] po = path.getPath();
        Playlist playlist = (Playlist) po[1];
        // Insert below existing tanda
        if (po.length == 3)
        {
            Tanda tandaTarget = (Tanda) po[2];
            out("drop at " + path);
            out("Insert " + tandaSource.orchestra + " under " + tandaTarget.orchestra);
            // playlist.insertTanda(tandaTarget.uniqueId, tandaSource);
            playlist.insertTanda(tandaTarget.uniqueId, tandaSource.uniqueId);
        }
        // insert at top of tanda
        else if (po.length == 2)
        {
            // playlist.insertTanda(-1, tandaSource);
            playlist.insertTandaAtTop(tandaSource.uniqueId);
        }
        playlistModel.notifyTreeModelHasChanged();
        Object[] ooo = new Object[2];
        ooo[0] = po[0];
        ooo[1] = po[1];
        TreePath newPath = new TreePath(ooo);
        playlistsTree.expandPath(newPath);
        return true;
    }

    private boolean moveTandaWithinPlaylist(TransferSupport support, String str)
    {
        long[] pat = extractPandT(str);
        long playlistUID = pat[0];
        long tandaUID = pat[1];
        JTree playlistsTree = (JTree) support.getComponent();
        PlaylistTreeModel playlistTreeModel = (PlaylistTreeModel) playlistsTree.getModel();
        Playlist playlist = playlistTreeModel.getPlaylist(playlistUID);
        // Tanda tandaToMove = playlist.getTanda(tandaUID);
        playlist.deleteTanda(tandaUID);
        DropLocation location = playlistsTree.getDropLocation();
        Point dropPoint = location.getDropPoint();
        TreePath path = playlistsTree.getClosestPathForLocation(dropPoint.x, dropPoint.y);
        Object[] po = path.getPath();
        Tanda locationTanda = (Tanda) po[2];
        playlist.insertTanda(locationTanda.uniqueId, tandaUID);
        playlistTreeModel.notifyTreeModelHasChanged();
        Object[] oo = new Object[2];
        oo[0] = po[0];
        oo[1] = po[1];
        TreePath newPath = new TreePath(oo);
        playlistsTree.expandPath(newPath);
        return true;
    }

    private long[] extractPandT(String str)
    {
        long[] ret = new long[2];
        int i1 = str.indexOf("playlist=");
        int i2 = str.indexOf(",");
        String s1 = str.substring(i1 + 9, i2);
        ret[0] = Long.parseLong(s1);
        i1 = str.indexOf("tanda=");
        String s2 = str.substring(i1 + 6);
        ret[1] = Long.parseLong(s2);
        return ret;
    }

    private String getTransferableString(TransferSupport support)
    {
        var transferable = support.getTransferable();
        return getTransferableString(transferable);
    }

    private String getTransferableString(Transferable t)
    {
        Object obj = null;
        try
        {
            obj = t.getTransferData(DataFlavor.stringFlavor);
        }
        catch (UnsupportedFlavorException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        String str = (String) obj;
        return str;
    }

    private class Source
    {
        SourceTarget st;
        long trackId;
        long playlistId;;
        long tandaId;
    }

    private class Target
    {
        SourceTarget st;
        long trackId;
        long playlistId;;
        long tandaId;
    }

    private static void out(String in)
    {
        System.out.println(in);
    }
}
