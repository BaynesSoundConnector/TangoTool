package pkgForTTNew;

import java.awt.GridLayout;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import javax.sound.sampled.Clip;
import javax.sound.sampled.LineListener;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreePath;

public class MouseEvents
{
    TrackTableModel mTrackTableModel;
    JTable mTrackTable;
    JTree mTandaTree;
    TandaTreeModel mTandaTreeModel;
    JTree mPlaylistTree;
    PlaylistTreeModel mPlaylistTreeModel;
    String mMusicBasePath;
    int mFramePosition = 0;
    HashMap<String, String> mCurrentState;

    public MouseEvents(JTable trackTable, JTree tandaTree, JTree playlistTree, String musicBasePath,
            HashMap<String, String> currentState)
    {
        mTrackTable = trackTable;
        mTrackTableModel = (TrackTableModel) mTrackTable.getModel();
        mTandaTree = tandaTree;
        mTandaTreeModel = (TandaTreeModel) mTandaTree.getModel();
        mPlaylistTree = playlistTree;
        mPlaylistTreeModel = (PlaylistTreeModel) mPlaylistTree.getModel();
        mMusicBasePath = musicBasePath;
        mCurrentState = currentState;
    }

    public void setFramePosition(int pos)
    {
        mFramePosition = pos;
    }

    // Track Table
    // Process mouse events from the track table. Return true means that one or more
    // models have changed and messages should be sent to that effect:
    // notifyTreeModelHasChanged() to all models
    //
    public boolean trackTableMouseEvents(MouseEvent me, Clip[] aClip, JButton playButton, LineListener ll)
    {
        JTable target = (JTable) me.getSource();
        if (SwingUtilities.isRightMouseButton(me))
        {
            int row = target.rowAtPoint(me.getPoint());
            if (row < 0)
                return false;
            if (!target.isRowSelected(row))
                target.setRowSelectionInterval(row, row);
            ArrayList<Track> tracks = new ArrayList<Track>();
            for (int viewRow : target.getSelectedRows())
            {
                int modelRow = target.convertRowIndexToModel(viewRow);
                Object obj = target.getModel().getValueAt(modelRow, 6);
                long uid = (obj instanceof Long) ? (Long) obj : Long.parseLong((String) obj);
                Track t = mTrackTableModel.getTrackbyUniqueId(uid);
                if (t != null)
                    tracks.add(t);
            }
            if (tracks.isEmpty())
                return false;
            boolean multi = tracks.size() > 1;
            Track track = tracks.get(0);
            long UID = track.uniqueId;
            String[] optionLabels = { "Copy", "Edit", "Tandas", "Export" };
            JRadioButton[] radioButtons = new JRadioButton[optionLabels.length];
            ButtonGroup group = new ButtonGroup();
            JPanel radioPanel = new JPanel(new GridLayout(optionLabels.length, 1));
            for (int i = 0; i < optionLabels.length; i++)
            {
                radioButtons[i] = new JRadioButton(optionLabels[i]);
                group.add(radioButtons[i]);
                radioPanel.add(radioButtons[i]);
            }
            // Copy and Tandas only make sense for a single track
            radioButtons[0].setEnabled(!multi);
            radioButtons[2].setEnabled(!multi);
            radioButtons[multi ? 1 : 0].setSelected(true);
            String dialogTitle = multi ? (tracks.size() + " tracks selected") : track.title;
            int rc = JOptionPane.showConfirmDialog(null, radioPanel, dialogTitle,
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (rc != JOptionPane.OK_OPTION)
                return false;
            String selectedValue = null;
            for (JRadioButton rb : radioButtons)
                if (rb.isSelected()) { selectedValue = rb.getText(); break; }
            if (selectedValue == null)
                return false;
            else if (selectedValue.equals("Copy"))
            {
                // mTrackCopyBuffer = track;
                // JOptionPane.showMessageDialog(null, "Track " + track.title + " " +
                // track.orchestra + " copied");
            }
            else if (selectedValue.equals("Edit"))
            {
                TrackDetailDialog tdd = multi ? new TrackDetailDialog(tracks) : new TrackDetailDialog(track);
                tdd.setVisible(true);
                if (tdd.bChanged)
                {
                    if (!multi)
                    {
                        track.normalizedOrchestra = SearchTermBuilder.stripAccents(track.orchestra);
                        track.searchTerm = SearchTermBuilder.buildSearchTerm(track.title, track.orchestra);
                    }
                    mTrackTableModel.setChanged(true);
                    mTrackTableModel.fireTableDataChanged();
                }
            }
            else if (selectedValue.equals("Tandas"))
            {
                int count = mTandaTreeModel.selectTandas(UID);
                // JOptionPane.showMessageDialog(null, count+" tandas selected");
                // setUpTandaTree();
                mTandaTreeModel.notifyTreeModelHasChanged();
            }
            else if (selectedValue.equals("Export"))
            {
                String lastExportDir = mCurrentState.get("ExportDirectory");
                JFileChooser jfc = new JFileChooser();
                jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                if (lastExportDir != null && !lastExportDir.isEmpty())
                    jfc.setCurrentDirectory(new File(lastExportDir));
                int exportRc = jfc.showDialog(null, "Export Here");
                if (exportRc == JFileChooser.APPROVE_OPTION)
                {
                    String exportDir = jfc.getSelectedFile().getAbsolutePath();
                    mCurrentState.put("ExportDirectory", exportDir);
                    int successCount = 0;
                    StringBuilder failures = new StringBuilder();
                    for (Track t : tracks)
                    {
                        if (ExportTrack.exportTrack(t, mMusicBasePath, exportDir))
                            successCount++;
                        else
                            failures.append(t.title).append(" - ").append(t.orchestra).append("\n");
                    }
                    StringBuilder summary = new StringBuilder();
                    summary.append("Exported ").append(successCount).append(" of ").append(tracks.size())
                            .append(tracks.size() == 1 ? " track" : " tracks").append("\nTo: ").append(exportDir);
                    if (failures.length() > 0)
                        summary.append("\n\nFailed:\n").append(failures);
                    Utilities.msg(summary.toString());
                }
            }
            return true;
        }
        if (me.getClickCount() == 2)
        {
            // A playlist is playing; the play button is disabled to block this. Don't
            // let a track-table double-click play a track over a running playlist.
            if (!playButton.isEnabled())
                return false;
            int row = target.getSelectedRow(); // select a row
            // int index = mTrackTable.convertRowIndexToModel(row);
            // int column = target.getSelectedColumn(); // select a column
            Object obj = mTrackTable.getValueAt(row, 6);
            long UID;
            if (obj instanceof Long)
            {
                UID = ((Long) obj).longValue();
            }
            else
                UID = Long.parseLong((String) obj);
            Utilities.out("r=" + row + " UID=" + UID);
            aClip[0] = Utilities.play(UID, mMusicBasePath, mTrackTableModel, aClip[0], ll, playButton, mFramePosition);
            return false;
        }
        return false;
    }
    //
    //   Tree
    //

    public boolean playlistTreeMouseEvents(MouseEvent me, Clip[] aClip, JButton playButton, LineListener ll,
            Tanda tandaCopyBuffer, JLabel playlistBottomLine)
    {
        //
        // Playlist tree Right Click
        //
        if (SwingUtilities.isRightMouseButton(me))
        {
            TreePath tp = mPlaylistTree.getPathForLocation(me.getX(), me.getY());
            Object[] path = tp.getPath();
            if (tp.getLastPathComponent() instanceof Playlist)
            {
                Playlist playlist = (Playlist) tp.getLastPathComponent();
                Object[] possibleValues =
                { "Export to CD", "Export to XSPF Playlist", "Export to Text", "Delete" };
                Object selectedValue = JOptionPane.showInputDialog(null, null, playlist.name,
                        JOptionPane.INFORMATION_MESSAGE, null, possibleValues, possibleValues[0]);
                if (selectedValue == null)
                    return true;
                if (selectedValue.equals("Delete"))
                {
                    int rc = JOptionPane.showConfirmDialog(null, "Delete playlist: " + playlist.name + "?", playlist.name,
                            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                    if (rc == JOptionPane.YES_OPTION)
                    {
                        mPlaylistTreeModel.delete(playlist);
                        mPlaylistTreeModel.notifyTreeModelHasChanged();
                    }
                }
                else if (selectedValue.equals("Export to CD"))
                {
                    ArrayList<Track> tracks = buildTrackList(playlist);
                    if (ExportCD.exportPlaylist(mMusicBasePath, tracks, playlist.name))
                    {
                        Utilities.msg("Exported CD images to: "+mMusicBasePath+"\\playlists\\CDImages\\"+playlist.name);
                    }
                }
                else if (selectedValue.equals("Export to Text"))
                {
                    Utilities.msg("Not implemented");
                }
                else if (selectedValue.equals("Export to XSPF Playlist"))
                {
                    String outputFileName = "\\playlists\\"+playlist.name+".xspf";
                    ArrayList<Track> tracks = buildTrackList(playlist);
                    if (ExportXSPF.convertToXSPF(tracks, outputFileName, mMusicBasePath))
                        Utilities.msg("Created playlist: "+mMusicBasePath+outputFileName);
                }
                return true;
            }
            else if (tp.getLastPathComponent() instanceof Tanda)
            {
                Tanda tanda = (Tanda) tp.getLastPathComponent();
                Object[] possibleValues =
                { "Copy", "Delete" };
                Object selectedValue = JOptionPane.showInputDialog(null, null,
                        "Copy or delete Tandas:" + tanda.orchestra, JOptionPane.QUESTION_MESSAGE, null, possibleValues,
                        possibleValues[0]);
                if (selectedValue == null)
                    JOptionPane.showMessageDialog(null, "SelectedValue is null!");
                else if (selectedValue.equals("Copy"))
                {
                    tandaCopyBuffer = tanda;
                    JOptionPane.showMessageDialog(null, "Copied " + tanda.orchestra);
                    return true;
                }
                else if (selectedValue.equals("Delete"))
                {
                    if (path.length == 3 && path[1] instanceof Playlist && path[2] instanceof Tanda)
                    {
                        mPlaylistTreeModel.deleteTanda((Playlist) path[1], (Tanda) path[2]);
                        Object[] newPath = new Object[2];
                        newPath[0] = path[0];
                        newPath[1] = path[1];
                        mPlaylistTreeModel.notifyTreeModelHasChanged();
                        TreePath newTreePath = new TreePath(newPath);
                        mPlaylistTree.scrollPathToVisible(newTreePath);
                        mPlaylistTree.expandPath(newTreePath);
                        JOptionPane.showMessageDialog(null, "Deleted " + tanda.orchestra);
                    }
                }
            }
            // Delete a track from a tanda in a playlist
            else if (tp.getLastPathComponent() instanceof Track)
            {
                Track track = (Track)tp.getLastPathComponent();
                Tanda tanda = (Tanda)path[2];
                int rc = JOptionPane.showConfirmDialog(null, "Delete track: " + track + "?", "Query:",
                        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (rc == JOptionPane.YES_OPTION)
                {
                    tanda.deleteTrack(track.uniqueId);
                    mTandaTreeModel.notifyTreeModelHasChanged();
                    mPlaylistTreeModel.notifyTreeModelHasChanged();
                    Object[] newPath = new Object[3];
                    newPath[0] = path[0];
                    newPath[1] = path[1];
                    newPath[2] = path[2];
                    TreePath newTreePath = new TreePath(newPath);
                    mPlaylistTree.scrollPathToVisible(newTreePath);
                    mPlaylistTree.expandPath(newTreePath);  
                }
                
            }
        }
        //
        // Double Click playlist table
        //
        else if (me.getClickCount() == 2)
        {
            TreePath tp = mPlaylistTree.getPathForLocation(me.getX(), me.getY());
            if (tp != null)
            {
                if (tp.getLastPathComponent() instanceof Track)
                {
                    if (aClip[0] != null)
                    {
                        aClip[0].stop();
                    }
                    Track track = (Track) tp.getLastPathComponent();
                    aClip[0] = Utilities.play(track.uniqueId, mMusicBasePath, mTrackTableModel, aClip[0], ll,
                            playButton, mFramePosition);
                }
            }
        }
        // Single left mouse click
        else
        {
            TreePath tp = mPlaylistTree.getPathForLocation(me.getX(), me.getY());
            if (tp != null)
            {
                Object[] objs = tp.getPath();
                displayTimeRemainingInPlaylist(objs, playlistBottomLine);
                Object obj = tp.getLastPathComponent();
                Playlist playlist;
                // If a playlist is currently playing, don't do anything when 
                // a playlist tree item is selected
                if (objs[0] instanceof Playlist)
                {
                    playlist = (Playlist) objs[0];
                    if (playlist.currentlyPlayingTanda != 0L)
                        return true;
                }
                else return true;
                Tanda tanda = null;
                Track track = null;
                if (tp.getLastPathComponent() instanceof Playlist)
                {
                    playlist = (Playlist) obj;
                    String time = Utilities.formatSecTohhmmss((int) playlist.calculatedTime);
                    playlistBottomLine.setText(time);
                }
                else if (tp.getLastPathComponent() instanceof Tanda)
                {
                    playlist = (Playlist) objs[1];
                    tanda = (Tanda) obj;
                    playlist.currentlyPlayingTanda = tanda.uniqueId;
                    float ftime = Utilities.calculateRemainingTimeInPlaylist(playlist, mTrackTableModel,
                            mTandaTreeModel);
                    String out = Utilities.formatSecTohhmmss((int) ftime);
                    playlistBottomLine.setText(out);
                }
                else if (tp.getLastPathComponent() instanceof Track)
                {
                    playlist = (Playlist) objs[1];
                    tanda = (Tanda) objs[2];
                    track = (Track) obj;
                    playlist.currentlyPlayingTanda = tanda.uniqueId;
                    playlist.currentlyPlayingTrack = track.uniqueId;
                    float ftime = Utilities.calculateRemainingTimeInPlaylist(playlist, mTrackTableModel,
                            mTandaTreeModel);
                    String out = Utilities.formatSecTohhmmss((int) ftime);
                    playlistBottomLine.setText(out);
                }
            }
        }
        return true;
    }
    private void displayTimeRemainingInPlaylist(Object[] objs, JLabel playlistBottomLine)
    {
        float ftime = Utilities.calculateRemainingTimeInPlaylist(objs, mTrackTableModel,
                mTandaTreeModel);
        String out = Utilities.formatSecTohhmmss((int) ftime);
        playlistBottomLine.setText(out);
    }
    private ArrayList<Track> buildTrackList(Playlist playlist)
    {
        ArrayList<Track> tracksOut = new ArrayList<Track>();
        long[] tandas = playlist.getTandaIDs();
        for (int i = 0 ; i < tandas.length ; i++)
        {
            Tanda tanda = mTandaTreeModel.getTandaByUniqueId(tandas[i]);
            long[] tracks = tanda.getTrackIDs();
            for (int j = 0 ; j < tracks.length ; j++)
            {
                Track track = mTrackTableModel.getTrackbyUniqueId2(tracks[j]);
                tracksOut.add(track);
            }
        }
        return tracksOut;
    }

    public boolean tandaTreeMouseEvents(MouseEvent me, Clip[] aClip, JButton playButton, LineListener ll)
    {
        if (SwingUtilities.isRightMouseButton(me))
        {
            TreePath tp = mTandaTree.getPathForLocation(me.getX(), me.getY());
            if (tp != null)
            {
                if (tp.getLastPathComponent() instanceof Track)
                {
                    Object[] path = tp.getPath();
                    Tanda tanda = null;
                    Track track = null;
                    if (path[1] instanceof Tanda)
                    {
                        tanda = (Tanda) path[1];
                        track = (Track) path[2];
                        int rc = JOptionPane.showConfirmDialog(null, "Delete track: " + track + "?", "Query:",
                                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                        if (rc == JOptionPane.YES_OPTION)
                        {
                            tanda.deleteTrack(track.uniqueId);
                            mTandaTreeModel.notifyTreeModelHasChanged();
                            TreePath ntp = retrogradePath(tp);
                            mTandaTree.expandPath(ntp);
                            mTandaTree.scrollPathToVisible(ntp);
                            return true;
                        }
                    }
                    else if (path[2] instanceof Tanda)
                    {
                        tanda = (Tanda) path[2];
                        track = (Track) path[3];
                        int rc = JOptionPane.showConfirmDialog(null, "Delete track: " + track + "?", "Query:",
                                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                        if (rc == JOptionPane.YES_OPTION)
                        {
                            tanda.deleteTrack(track.uniqueId);
                            mTandaTreeModel.notifyTreeModelHasChanged();
                            mTandaTree.expandPath(retrogradePath(tp));
                            mTandaTree.scrollPathToVisible(retrogradePath(tp));
                        }
                    }
                }
                else if (tp.getLastPathComponent() instanceof Tanda)
                {
                    Tanda tanda = (Tanda) tp.getLastPathComponent();
                    Object[] possibleValues =
                    { "Info", "Delete" };
                    Object selectedValue = JOptionPane.showInputDialog(null, "Edit Tanda info or delete?",
                            tanda.orchestra, JOptionPane.INFORMATION_MESSAGE, null, possibleValues, possibleValues[0]);
                    if (selectedValue == null)
                        return true;
                    if (selectedValue.equals("Delete"))
                    {
                        int rc = JOptionPane.showConfirmDialog(null, "Delete tanda:" + tanda.orchestra + " ?", "Query:",
                                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                        if (rc == JOptionPane.YES_OPTION)
                        {
                            mTandaTreeModel.deleteTanda(tanda.uniqueId);
                            Utilities.out("Delete tanda " + tanda.uniqueId);
                            mTandaTreeModel.notifyTreeModelHasChanged();
                            mTandaTree.expandPath(retrogradePath(tp));
                            mTandaTree.scrollPathToVisible(retrogradePath(tp));
                        }
                    }
                    else if (selectedValue.equals("Info"))
                    {
                        TandaDetailDialog tdd = new TandaDetailDialog(tanda);
                        tdd.setVisible(true);
                        if (tdd.bChanged)
                        {
                            int rc = JOptionPane.showConfirmDialog(null, "Save changes?", "Query:",
                                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                            if (rc == JOptionPane.YES_OPTION)
                            {
                                tanda.description = tdd.description.getText();
                                tanda.orchestra = tdd.orchestra.getText();
                                tanda.source = tdd.source.getText();
                            }
                        }
                    }
                }
            }
        }
        if (me.getClickCount() == 2)
        {
            TreePath tp = mTandaTree.getPathForLocation(me.getX(), me.getY());
            if (tp != null)
            {
                if (tp.getLastPathComponent() instanceof Track)
                {
                    Track track = (Track) tp.getLastPathComponent();
                    aClip[0] = Utilities.play(track.uniqueId, mMusicBasePath, mTrackTableModel, aClip[0], ll,
                            playButton, mFramePosition);
                }
            }
        }
        return true;
    }

    private TreePath retrogradePath(TreePath tpIn)
    {
        Object[] objs = tpIn.getPath();
        Object[] objsOut = new Object[objs.length - 1];
        for (int i = 0; i < objsOut.length; i++)
            objsOut[i] = objs[i];
        return new TreePath(objsOut);
    }
}
