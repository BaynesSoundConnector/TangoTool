package pkgForTTNew;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
//import TTDND.TreeTransferHandler;


public class MainForTTNew extends JFrame implements ActionListener, MouseListener, LineListener, ListSelectionListener,
        TableModelListener, ItemListener, TreeModelListener
{
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    JMenuBar menuBar;
    JMenu menu;
    JPanel mainPanel;
    JSplitPane leftSplitPane;
    JSplitPane centerSplitPane;
    JSplitPane rightSplitPane;
    Dimension screenSize;
    JPanel trackPanel;
    JPanel tandaPanel;
    JPanel playlistPanel;
    JTable mTrackTable;
    JTree mTandaTree;
    JTree mPlaylistTree;
    Tanda mTandaCopyBuffer;
    Track mTrackCopyBuffer;
    MouseEvents mouseEvents;
    String mMusicBasePath;
    JTextField playlistSearchTerm;
    JTextField tandaSearchTerm;
    TrackTableModel mTrackTableModel;
    TandaTreeModel mTandaTreeModel;
    PlaylistTreeModel mPlaylistTreeModel;
    Clip mClip;
    JButton resetButton;
    JButton searchButton;
    JButton playButton;
    JButton startPlaylistButton;
    JButton stopPlaylistButton;
    JButton nextSongButton;
    JButton resetTandaTree;
    JButton tandaSearchButton;
    JCheckBox showTrackCheckBox;
    JCheckBox last10SecondsCheckBox;
    JCheckBox onePlaylistCheckBox;
    JLabel bottomLine = new JLabel(" ");
    JLabel playlistBottomLine = new JLabel();
    // boolean bPlayingOneTrack;
    boolean bStoppingPlaylist;
    boolean bContinuousPlaying;
    boolean bPaused;
    TreePath mCurrentlyPlayingPath;
    Playlist mCurrentlyPlayingPlaylist;
    int mFramePosition = 0;
    HashMap<String, String> currentState = new HashMap<>();
    public static boolean NEWTRACKDATABASE = true;
    long mLastPreviousKeyTime = 0L;

    public MainForTTNew()
    {
        super("Tango Playlist Tool");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        out(getConfigFileName());
        loadState();
        // setUIFont (new javax.swing.plaf.FontUIResource("Times New
        // Roman",Font.PLAIN,14));
        setUIFont(new javax.swing.plaf.FontUIResource("Lucida Sans Unicode", Font.PLAIN, 14));
        mMusicBasePath = normalizePath(currentState.get("MusicBasePath"));
        mTrackTableModel = new TrackTableModel(mMusicBasePath);
        // Utilities.out("Tracks:"+mTrackTableModel.getTrackCount());
        mTandaTreeModel = new TandaTreeModel(mMusicBasePath);
        mTandaTreeModel.addTreeModelListener(this);
        // Utilities.out("Tracks:"+mTrackTableModel.getTrackCount());
        mTandaTreeModel.setTrackTableModel(mTrackTableModel);
        mPlaylistTreeModel = new PlaylistTreeModel(mTrackTableModel, mTandaTreeModel);
        mPlaylistTreeModel.addTreeModelListener(this);
        DataHandler.loadDataNew(mMusicBasePath, mTrackTableModel, mTandaTreeModel, mPlaylistTreeModel);
        // AssembleOldData aod = new AssembleOldData(mMusicBasePath, mTrackTableModel,
        // mTandaTreeModel, mPlaylistTreeModel);
        calculateTime();
        // Utilities.out("Tracks:"+mTrackTableModel.getTrackCount());
        makeFrameFullSize(this);
        this.setLayout(new BorderLayout());
        this.setJMenuBar(setUpMenuBar());
        mainPanel = new JPanel(new GridLayout(2, 1));
        this.add(mainPanel, BorderLayout.CENTER);
        this.add(createBottomLine(), BorderLayout.SOUTH);
        this.add(createTopLine(), BorderLayout.NORTH);
        this.add(new JSplitPane(JSplitPane.HORIZONTAL_SPLIT));
        this.add(createCenter(), BorderLayout.CENTER);
        // Utilities.out("Tracks:"+mTrackTableModel.getTrackCount());
        calculateStats();
        mouseEvents = new MouseEvents(mTrackTable, mTandaTree, mPlaylistTree, mMusicBasePath, currentState);
        setupRemoteKeyBindings();
    }

    public static void setUIFont(javax.swing.plaf.FontUIResource f)
    {
        java.util.Enumeration keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements())
        {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof javax.swing.plaf.FontUIResource)
                UIManager.put(key, f);
        }
    }

    private void redoTrackInTandasColumn()
    {
        for (Track track : mTrackTableModel.mTracks)
        {
            track.inTandas = mTandaTreeModel.trackInTandas(track.uniqueId);
        }
    }

    private Statistics calculateStats()
    {
        Statistics stats = new Statistics();
        Iterator<Track> it = mTrackTableModel.mTracks.iterator();
        while (it.hasNext())
        {
            Track track = it.next();
            stats.total++;
            stats.totalTrackTime += track.calculatedTime;
            track.inTandas = mTandaTreeModel.trackInTandas(track.uniqueId);
            // out("stats");
        }
        return stats;
    }

    private JSplitPane createCenter()
    {
        trackPanel = setUpTrackPanel();
        tandaPanel = setUpTandaPanel();
        rightSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tandaPanel, setUpPlaylistPanel());
        leftSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, trackPanel, rightSplitPane);
        String t1 = currentState.get("leftDivider");
        String t2 = currentState.get("rightDivider");
        leftSplitPane.setDividerLocation(Integer.valueOf(t1));
        rightSplitPane.setDividerLocation(Integer.valueOf(t2));
        return leftSplitPane;
    }

    private JPanel setUpTandaPanel()
    {
        tandaPanel = new JPanel(new BorderLayout());
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        tandaSearchTerm = new JTextField(20);
        panel.add(tandaSearchTerm);
        tandaSearchButton = new JButton("Search");
        tandaSearchButton.addActionListener(this);
        panel.add(tandaSearchButton);
        resetTandaTree = new JButton("Reset");
        resetTandaTree.addActionListener(this);
        resetTandaTree.setActionCommand("resettandatree");
        panel.add(resetTandaTree);
        // mTandaTreeModel = new TandaTreeModel(Path, mTrackTableModel);
        setUpTandaTree();
        JScrollPane scrollpane = new JScrollPane(mTandaTree);
        tandaPanel.add(scrollpane, BorderLayout.CENTER);
        tandaPanel.add(panel, BorderLayout.NORTH);
        return tandaPanel;
    }

    private void setUpTandaTree()
    {
        mTandaTree = new JTree(mTandaTreeModel);
        mTandaTree.setRootVisible(true);
        mTandaTree.setName("Tandas");
        mTandaTree.setShowsRootHandles(true);
        mTandaTree.addMouseListener(this);
        mTandaTree.setTransferHandler(new TangoTransferHandler());
        mTandaTree.setDragEnabled(true);
        mTandaTree.setDropMode(DropMode.ON);
    }

    private JPanel setUpTrackPanel()
    {
        // mTrackTableModel = new TrackTableModel(Path);
        mTrackTable = new JTable(mTrackTableModel);
        mTrackTableModel.addTableModelListener(this);
        mTrackTable.getSelectionModel().addListSelectionListener(this);
        mTrackTable.setAutoCreateRowSorter(true);
        mTrackTable.setDragEnabled(true);
        mTrackTable.setTransferHandler(new TangoTransferHandler());
        mTrackTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        mTrackTable.addMouseListener(this);
        TableColumnModel columnModel = mTrackTable.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(200);
        columnModel.getColumn(1).setPreferredWidth(100);
        columnModel.getColumn(2).setPreferredWidth(30);
        columnModel.getColumn(3).setPreferredWidth(5);
        columnModel.getColumn(4).setPreferredWidth(10);
        columnModel.getColumn(5).setPreferredWidth(30);
        columnModel.getColumn(6).setPreferredWidth(7);
        JScrollPane scrollPane = new JScrollPane(mTrackTable);
        mTrackTable.setFillsViewportHeight(true);
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(setupTrackBar(), BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel setUpPlaylistPanel()
    {
        playlistPanel = new JPanel(new BorderLayout());
        JPanel grid = new JPanel(new GridLayout(2, 1));
        JPanel flow = new JPanel(new FlowLayout(FlowLayout.CENTER));
        flow.add(new JLabel("Playlists"));
        grid.add(flow);
        JPanel flow2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        startPlaylistButton = new JButton("Start Playlist");
        startPlaylistButton.addActionListener(this);
        startPlaylistButton.setPreferredSize(new Dimension(120, 20));
        startPlaylistButton.setEnabled(false);
        flow2.add(startPlaylistButton);
        stopPlaylistButton = new JButton("Stop Playlist");
        stopPlaylistButton.addActionListener(this);
        stopPlaylistButton.setPreferredSize(new Dimension(110, 20));
        stopPlaylistButton.setEnabled(false);
        flow2.add(stopPlaylistButton);
        nextSongButton = new JButton("Next");
        nextSongButton.addActionListener(this);
        nextSongButton.setPreferredSize(new Dimension(75, 20));
        nextSongButton.setEnabled(false);
        flow2.add(nextSongButton);
        onePlaylistCheckBox = new JCheckBox("One Playlist");
        onePlaylistCheckBox.addActionListener(this);
        // onePlaylistCheckBox.setEnabled(false);
        flow2.add(onePlaylistCheckBox);
        grid.add(flow2);
        playlistPanel.add(grid, BorderLayout.NORTH);
        // mTandaTree.addMouseListener(this);
        setUpPlaylistTree();
        JScrollPane scrollpane = new JScrollPane(mPlaylistTree);
        playlistPanel.add(scrollpane, BorderLayout.CENTER);
        playlistPanel.add(createPlaylistBottomLine(), BorderLayout.SOUTH);
        int count = mPlaylistTree.getRowCount();
        return playlistPanel;
    }

    private JPanel createPlaylistBottomLine()
    {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        playlistBottomLine.setText("" + mPlaylistTreeModel.getPlaylistCount() + " Playlists");
        panel.add(playlistBottomLine);
        return panel;
    }

    private void setUpPlaylistTree()
    {
        // mPlaylistTreeModel = new PlaylistTreeModel(mTrackTableModel,
        // mTandaTreeModel);
        mPlaylistTree = new JTree(mPlaylistTreeModel);
        mPlaylistTree.setCellRenderer(new PlaylistTreeCellRenderer());
        mPlaylistTree.setRootVisible(true);
        mPlaylistTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        mPlaylistTree.setShowsRootHandles(true);
        mPlaylistTree.setName("Playlists");
        mPlaylistTree.setDragEnabled(true);
        mPlaylistTree.addMouseListener(this);
        mPlaylistTree.setEditable(true);
        mPlaylistTree.setDropMode(DropMode.ON);
        mPlaylistTree.setTransferHandler(new TangoTransferHandler());
    }

    JPanel setupTrackBar()
    {
        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        playlistSearchTerm = new JTextField(20);
        searchBar.add(playlistSearchTerm);
        searchButton = new JButton("Search");
        searchButton.addActionListener(this);
        searchBar.add(searchButton);
        resetButton = new JButton("Reset");
        resetButton.addActionListener(this);
        searchBar.add(resetButton);
        playButton = new JButton("Play");
        playButton.addActionListener(this);
        searchBar.add(playButton);
        showTrackCheckBox = new JCheckBox("Show");
        showTrackCheckBox.setSelected(false);
        searchBar.add(showTrackCheckBox);
        showTrackCheckBox.addItemListener(this);
        showTrackCheckBox.setName("showtrack");
        last10SecondsCheckBox = new JCheckBox("Last 10 seconds");
        last10SecondsCheckBox.setSelected(false);
        searchBar.add(last10SecondsCheckBox);
        last10SecondsCheckBox.addItemListener(this);
        last10SecondsCheckBox.setName("last10");
        return searchBar;
    }

    private JMenuBar setUpMenuBar()
    {
        menuBar = new JMenuBar();
        menu = new JMenu("File");
        menuBar.add(menu);
        JMenuItem addMenuItem = new JMenuItem("Add Track");
        menu.add(addMenuItem);
        addMenuItem.addActionListener(this);
        JMenuItem addFolderMenuItem = new JMenuItem("Add Folder");
        menu.add(addFolderMenuItem);
        addFolderMenuItem.addActionListener(this);
        JMenuItem openMenuItem = new JMenuItem("Open");
        menu.add(openMenuItem);
        openMenuItem.addActionListener(this);
        JMenuItem saveMenuItem = new JMenuItem("Save");
        menu.add(saveMenuItem);
        saveMenuItem.addActionListener(this);
        JMenuItem exitMenuItem = new JMenuItem("Exit");
        menu.add(exitMenuItem);
        exitMenuItem.addActionListener(this);
        JMenuItem preferencesMenuItem = new JMenuItem("Preferences");
        preferencesMenuItem.addActionListener(this);
        menu.add(preferencesMenuItem);
        JMenuItem validateMenuItem = new JMenuItem("Validate");
        validateMenuItem.addActionListener(this);
        menu.add(validateMenuItem);
        return menuBar;
    }

    public JPanel createBottomLine()
    {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel.add(bottomLine);
        return panel;
    }

    public JPanel createTopLine()
    {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel.add(new JLabel("Top line"));
        return panel;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        if (e.getSource() == tandaSearchButton)
        {
            String term = tandaSearchTerm.getText();
            if (mTandaTreeModel.selectTandas(term) > 0)
                mTandaTreeModel.notifyTreeModelHasChanged();
        }
        if (e.getSource() == onePlaylistCheckBox)
        {
            JCheckBox box = (JCheckBox) e.getSource();
            Boolean boo = box.isEnabled();
            Boolean boo2 = box.isSelected();
            if (!onePlaylistCheckBox.isSelected())
            {
                mPlaylistTreeModel.showOnlyOnePlaylist(null);
                return;
            }
            TreePath path = mPlaylistTree.getSelectionPath();
            if (path == null)
            {
                onePlaylistCheckBox.setSelected(false);
                return;
            }
            Object[] oPath = path.getPath();
            Playlist playlist = (Playlist) oPath[1];
            mPlaylistTreeModel.showOnlyOnePlaylist(playlist);
        }
        if (e.getSource() == stopPlaylistButton)
        {
            if (mClip != null)
            {
                mClip.stop();
                mClip.close();
                // mClip.flush();
                mClip = null;
                bottomLine.setText(" ");
            }
            bContinuousPlaying = false;
            startPlaylistButton.setText("Start Playlist");
            stopPlaylistButton.setEnabled(false);
            mCurrentlyPlayingPlaylist.currentlyPlayingTanda = 0l;
            mCurrentlyPlayingPlaylist.currentlyPlayingTrack = 0l;
            mCurrentlyPlayingPlaylist = null;
            bPaused = false;
        }
        if (e.getSource() == nextSongButton)
        {
            if (mClip != null)
            {
                mClip.stop();
                mClip.close();
            }   
            // mClip.flush();
        }
        if (e.getSource() == startPlaylistButton)
        {
            // PLaylist in progress is being paused
            if (bContinuousPlaying && startPlaylistButton.getText().equalsIgnoreCase("pause playlist"))
            {
                bPaused = true;
                mClip.stop();
                boolean b1 = mClip.isActive();
                boolean b2 = mClip.isRunning();
                boolean b3 = mClip.isOpen();
                startPlaylistButton.setText("Resume Playlist");
                stopPlaylistButton.setEnabled(true);
                return;
            }
            if (bPaused && startPlaylistButton.getText().equalsIgnoreCase("resume playlist"))
            {
                mClip.start();
                startPlaylistButton.setText("Pause Playlist");
                bPaused = false;
                return;
            }
            TreePath path = mPlaylistTree.getSelectionPath();
            if (path == null)
                return;
            Object[] objs = path.getPath();
            // Playlist root only. Ignore
            if (objs.length == 1)
                return;
            if (mClip != null)
            {
                mClip.stop();
                mClip.close();
                // mClip.flush();
                mClip = null;
            }
            bContinuousPlaying = true;
            mCurrentlyPlayingPlaylist = null;
            Tanda tanda = null;
            Track track = null;
            // Playlist
            if (objs.length == 2)
            {
                mCurrentlyPlayingPlaylist = (Playlist) objs[1];
                long lll = mCurrentlyPlayingPlaylist.getTandaIDs()[0];
                tanda = mTandaTreeModel.getTandaByUniqueId(lll);
                track = mTrackTableModel.getTrackbyUniqueId(tanda.getTrackIDs()[0]);
            }
            // Tanda
            else if (objs.length == 3)
            {
                mCurrentlyPlayingPlaylist = (Playlist) objs[1];
                tanda = (Tanda) objs[2];
                track = mTrackTableModel.getTrackbyUniqueId(tanda.getTrackIDs()[0]);
            }
            // Track
            else if (objs.length == 4)
            {
                mCurrentlyPlayingPlaylist = (Playlist) objs[1];
                tanda = (Tanda) objs[2];
                track = (Track) objs[3];
            }
            mCurrentlyPlayingPlaylist.currentlyPlayingTanda = tanda.uniqueId;
            mCurrentlyPlayingPlaylist.currentlyPlayingTrack = track.uniqueId;
            Object[] thePath = new Object[4];
            thePath[0] = objs[0];
            thePath[1] = mCurrentlyPlayingPlaylist;
            thePath[2] = tanda;
            thePath[3] = track;
            mCurrentlyPlayingPath = new TreePath(thePath);
            mPlaylistTree.setSelectionPath(mCurrentlyPlayingPath);
            mPlaylistTree.expandPath(path);
            startPlaylistButton.setText("Pause Playlist");
            stopPlaylistButton.setEnabled(true);
            play(track.uniqueId);
            nextSongButton.setEnabled(true);
            return;
        }
        else if (e.getActionCommand().equalsIgnoreCase("save"))
        {
            // out("save");
            saveDatabases();
            saveState();
        }
        else if (e.getActionCommand().equalsIgnoreCase("exit"))
        {
            out("exit");
            saveDatabases();
            saveState();
            System.exit(0);
        }
        else if (e.getActionCommand().equalsIgnoreCase("open"))
        {
            out("open");
            File file = new File(getWorkingDirectory());
            JFileChooser jfc = new JFileChooser();
            jfc.setCurrentDirectory(file);
            int returnValue = jfc.showOpenDialog(null);
        }
        else if (e.getActionCommand().equalsIgnoreCase("Add Folder"))
        {
            JFileChooser jfc = new JFileChooser();
            jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            File f = new File(mMusicBasePath);
            jfc.setCurrentDirectory(f);
            int returnValue = jfc.showOpenDialog(null);
            if (returnValue == JFileChooser.CANCEL_OPTION)
                return;
            File sel = jfc.getSelectedFile();
            String name = sel.getName();
            String path = sel.getPath();
            String abspath = sel.getAbsolutePath();
            AddFolderDialog tdd = new AddFolderDialog(path, mTrackTableModel);
            tdd.setVisible(true);
        }
        else if (e.getActionCommand().equalsIgnoreCase("Add Track"))
        {
            out("add track");
            // File file = new File(getWorkingDirectory());
            JFileChooser jfc = new JFileChooser();
            File f = new File(mMusicBasePath);
            jfc.setCurrentDirectory(f);
            // jfc.setCurrentDirectory(file);
            int returnValue = jfc.showOpenDialog(null);
            if (returnValue == JFileChooser.CANCEL_OPTION)
                return;
            File sel = jfc.getSelectedFile();
            String name = sel.getName();
            String path = sel.getPath();
            String abspath = sel.getAbsolutePath();
            String mbplc = mMusicBasePath.toLowerCase();
            String sflc = abspath.toLowerCase();
            String fs = File.separator;
            if (!sflc.startsWith(mbplc))
            {
                JOptionPane.showMessageDialog(null, "Selected file must be in " + mMusicBasePath);
                return;
            }
            Float time = SoundUtils.musicFileValid(sel);
            if (time == 0f)
            {
                JOptionPane.showMessageDialog(null, "Selected file not a music file. Just sayin'");
                // return;
            }
            TrackDetailDialog tdd = new TrackDetailDialog(sel, mMusicBasePath);
            tdd.setVisible(true);
            if (tdd.bChanged)
            {
                Track track = tdd.getTrack();
                mTrackTableModel.addTrack(track);
                mTrackTableModel.setChanged(true);
                mTrackTableModel.fireTableDataChanged();
                mTandaTreeModel.notifyTreeModelHasChanged();
                mPlaylistTreeModel.notifyTreeModelHasChanged();
                JOptionPane.showMessageDialog(null, "Track added:" + track.orchestra);
            }
        }
        else if (e.getActionCommand().equalsIgnoreCase("validate"))
        {
            String message = Utilities.validate(mMusicBasePath, mTrackTableModel, mTandaTreeModel, mPlaylistTreeModel,
                    false);
            int rc = JOptionPane.showConfirmDialog(null, message + "\nRepair?", "Repair?", JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            if (rc == JOptionPane.YES_OPTION)
            {
                message = Utilities.validate(mMusicBasePath, mTrackTableModel, mTandaTreeModel, mPlaylistTreeModel,
                        true);
                JOptionPane.showMessageDialog(null, message);
            }
        }
        else if (e.getActionCommand().equalsIgnoreCase("preferences"))
        {
            out("preferences");
        }
        else if (e.getSource() == resetButton)
        {
            mTrackTableModel.reset();
            playlistSearchTerm.setText("");
            if (mClip != null)
            {
                mClip.stop();
                mClip.close();
                // mClip.flush();
                mClip = null;
                playButton.setText("Play");
                // bPlayingOneTrack = false;
            }
        }
        else if (e.getSource() == resetTandaTree)
        {
            mTandaTreeModel.reset();
            // mTandaTree.invalidate();
            mTandaTreeModel.notifyTreeModelHasChanged();
        }
        else if (e.getSource() == searchButton)
        {
            String search = playlistSearchTerm.getText();
            int found = mTrackTableModel.search(search);
            if (found == 0)
                JOptionPane.showMessageDialog(null, "Not found");
        }
        // Track table play button. Play only this track
        else if (e.getSource() == playButton)
        {
            if (mClip != null)
            {
                mClip.stop();
                mClip.close();
                // mClip.flush();
                mClip = null;
                return;
                // playButton.setText("Play");
                // bPlayingOneTrack = false;
            }
            int row = mTrackTable.getSelectedRow();
            if (row == -1)
                return;
            Object obj = mTrackTable.getValueAt(row, 6);
            long UID = (long) obj;
            playButton.setText("Stop");
            // bPlayingOneTrack = true;
            play(UID);
        }
    }

    private long getTrackUIDFromRow(int row)
    {
        // String str = mTrackTableModel.getValueAt(row, 6);
        Object obj = mTrackTableModel.getValueAt(row, 6);
        // long UID = Long.parseLong(str);
        // Track track = mTrackTableModel.getTrackbyUniqueId(UID);
        // return track.uniqueId;
        return 0l;
    }

    private void resetTrackCheckBox()
    {
        showTrackCheckBox.setSelected(false);
        mTandaTreeModel.selectTandas(null);
        mTandaTreeModel.notifyTreeModelHasChanged();
        mPlaylistTreeModel.showTrack(-1);
        mPlaylistTreeModel.notifyTreeModelHasChanged();
        return;
    }

    private void expandThePlaylist(TreePath path)
    {
        Object[] obja = path.getPath();
        Object[] objb = new Object[2];
        objb[0] = obja[0];
        objb[1] = obja[1];
        TreePath tp = new TreePath(objb);
        mPlaylistTree.expandPath(tp);
    }

    public void mouseClicked(MouseEvent me)
    {
        //
        // Track Table Mouse Events
        //
        if (me.getSource() instanceof JTable)
        {
            Clip[] aClip = new Clip[1];
            aClip[0] = mClip;
            mouseEvents.trackTableMouseEvents(me, aClip, playButton, this);
            mClip = aClip[0];
            return;
        }
        //
        else if (me.getSource() instanceof JTree)
        {
            JTree tree = (JTree) me.getSource();
            //
            // Tanda Tree Mouse Events
            //
            if (tree.getName().equalsIgnoreCase("Tandas"))
            {
                // Utilities.out("Tanda MouseMessage");
                Clip[] aClip = new Clip[1];
                aClip[0] = mClip;
                mouseEvents.tandaTreeMouseEvents(me, aClip, playButton, this);
                mClip = aClip[0];
                return;
            }
            //
            // Playlist Tree Mouse Events
            //
            else if (tree.getName().equalsIgnoreCase("Playlists"))
            {
                int tsc = mPlaylistTree.getSelectionCount();
                if (tsc > 0)
                {
                    onePlaylistCheckBox.setEnabled(true);
                    startPlaylistButton.setEnabled(true);
                }
                Clip[] aClip = new Clip[1];
                aClip[0] = mClip;
                mouseEvents.playlistTreeMouseEvents(me, aClip, playButton, this, mTandaCopyBuffer, playlistBottomLine);
                mClip = aClip[0];
            }
        }
    }

    private void play(long trackUID)
    {
        if (mClip != null)
        {
            mClip.stop();
            mClip.close();
            bottomLine.setText(" ");
            // mClip.flush();
        }
        Clip[] aClip = new Clip[1];
        aClip[0] = mClip;
        mClip = Utilities.play(trackUID, mMusicBasePath, mTrackTableModel, aClip[0], this, playButton, mFramePosition);
        bottomLine.setText(mTrackTableModel.getTrackbyUniqueId(trackUID).title);
    }

    private void makeFrameFullSize(JFrame aFrame)
    {
        screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        aFrame.setSize(screenSize.width, screenSize.height - 50);
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        // TODO Auto-generated method stub
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
        // TODO Auto-generated method stub
    }

    @Override
    public void mouseEntered(MouseEvent e)
    {
        // TODO Auto-generated method stub
    }

    @Override
    public void mouseExited(MouseEvent e)
    {
        // TODO Auto-generated method stub
    }

    @Override
    public void update(LineEvent event)
    {
        LineEvent.Type type = event.getType();
        if (type == LineEvent.Type.OPEN)
        {
            out("LineEvent.Type.OPEN");
        }
        else if (type == LineEvent.Type.CLOSE)
        {
            out("LineEvent.Type.CLOSE");
        }
        else if (type == LineEvent.Type.START)
        {
            out("LineEvent.Type.START");
            if (bContinuousPlaying)
            {
                String out = formatRemainingTimeInPlaylist(mCurrentlyPlayingPlaylist);
                playlistBottomLine.setText(out);
            }
        }
        // Song ended, what should we do?
        else if (type == LineEvent.Type.STOP)
        {
            out("LineEvent.Type.STOP");
            if (bPaused)
                return;
            if (!bContinuousPlaying)
            {
                mClip = null;
                playButton.setText("Play");
                return;
            }
            // Advance one row
            // Object obj = advancePlaylistTreeOneRow();
            // We have reached the end of the playlist
            if (!advanceOneTrack())
            {
                endPlaylist();
                return;
            }
            setPlaylistSelectedItem();
            // while (!(obj instanceof Track))
            // {
            // if (obj instanceof Tanda)
            // mCurrentlyPlayingPlaylist.currentlyPlayingTanda = ((Tanda) obj).uniqueId;
            // obj = advancePlaylistTreeOneRow();
            // }
            // Track track = (Track) obj;
            // mCurrentlyPlayingPlaylist.currentlyPlayingTrack = track.uniqueId;
            playlistBottomLine.setText(formatRemainingTimeInPlaylist(mCurrentlyPlayingPlaylist));
            // Utilities.out("Time remaining in playlist=" +
            // calculateRemainingTimeInPlaylist());
            play(mCurrentlyPlayingPlaylist.currentlyPlayingTrack);
        }
    }

    private void setPlaylistSelectedItem()
    {
        // TreePath path = mPlaylistTree.getSelectionPath();
        // Object[] objs = path.getPath();
        Object[] newObjs = new Object[4];
        newObjs[0] = mPlaylistTreeModel.getRoot();
        newObjs[1] = mCurrentlyPlayingPlaylist;
        newObjs[2] = mTandaTreeModel.getTandaByUniqueId(mCurrentlyPlayingPlaylist.currentlyPlayingTanda);
        newObjs[3] = mTrackTableModel.getTrackbyUniqueId(mCurrentlyPlayingPlaylist.currentlyPlayingTrack);
        TreePath newPath = new TreePath(newObjs);
        mPlaylistTree.setSelectionPath(newPath);
    }

    private void endPlaylist()
    {
        mCurrentlyPlayingPath = null;
        startPlaylistButton.setText("Play");
        mClip = null;
        bContinuousPlaying = false;
        nextSongButton.setEnabled(false);
        JOptionPane.showMessageDialog(null, "Playlist has ended");
        startPlaylistButton.setText("Start Playlist");
        startPlaylistButton.setEnabled(false);
        stopPlaylistButton.setEnabled(false);
    }

    private boolean advanceOneTrack()
    {
        if (mCurrentlyPlayingPlaylist == null)
            return false;
        if (mCurrentlyPlayingPlaylist.currentlyPlayingTanda == 0l)
        {
            mCurrentlyPlayingPlaylist.currentlyPlayingTanda = mCurrentlyPlayingPlaylist.getFirstTandaID();
            mCurrentlyPlayingPlaylist.currentlyPlayingTrack = mTandaTreeModel
                    .getFirstTrackID(mCurrentlyPlayingPlaylist.currentlyPlayingTanda);
            // setPlaylistTreeSelection(mCurrentlyPlayingPlaylist);
        }
        mCurrentlyPlayingPlaylist.currentlyPlayingTrack = mTandaTreeModel.getNextTrackID(
                mCurrentlyPlayingPlaylist.currentlyPlayingTanda, mCurrentlyPlayingPlaylist.currentlyPlayingTrack);
        if (mCurrentlyPlayingPlaylist.currentlyPlayingTrack == -1l)
        {
            if (mCurrentlyPlayingPlaylist.nextTanda())
                mCurrentlyPlayingPlaylist.currentlyPlayingTrack = mTandaTreeModel
                        .getFirstTrackID(mCurrentlyPlayingPlaylist.currentlyPlayingTanda);
            else
                return false;
        }
        return true;
    }

    private String formatRemainingTimeInPlaylist(Playlist playlist)
    {
        float time = Utilities.calculateRemainingTimeInPlaylist(playlist, mTrackTableModel, mTandaTreeModel);
        String out = Utilities.formatSecTohhmmss((int) time);
        return out;
    }

    private float calculateRemainingTimeInSong()
    {
        long length = mClip.getMicrosecondLength();
        long position = mClip.getMicrosecondPosition();
        Utilities.out(mMusicBasePath);
        float remain = ((float) length - position) / 1000;
        Utilities.out(remain + " time remaining in song");
        return remain;
    }

    private void loadState()
    {
        try
        {
            String configFile = getConfigFileName();
            File file = new File(configFile);
            if (!file.exists())
            {
                file = saveState();
            }
            file.getParentFile().mkdirs();
            BufferedReader br = new BufferedReader(new FileReader(configFile));
            while (br.ready())
            {
                String line = br.readLine();
                if (line.length() < 2)
                    break;
                String key = line.substring(0, line.indexOf("\t"));
                String value = line.substring(line.indexOf("\t") + 1);
                currentState.put(key, value);
                // out(line);
            }
            br.close();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // currentDataFilePath.getText();
    }

    private File saveState()
    {
        out("saveState()");
        try
        {
            int leftDivider = leftSplitPane.getDividerLocation();
            int rightDivider = rightSplitPane.getDividerLocation();
            String configFile = getConfigFileName();
            File file = new File(configFile);
            file.getParentFile().mkdirs();
            BufferedWriter bw;
            bw = new BufferedWriter(new FileWriter(configFile));
            currentState.put("leftDivider", new String(leftDivider + ""));
            currentState.put("rightDivider", new String(rightDivider + ""));
            Set<String> ss = currentState.keySet();
            Iterator<String> it = ss.iterator();
            while (it.hasNext())
            {
                String key = it.next();
                String value = currentState.get(key);
                bw.write(key + "\t" + value + "\n");
            }
            bw.close();
            return file;
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    private String getWorkingDirectory()
    {
        Map<String, String> env = System.getenv();
        String appData = env.get("APPDATA");
        return appData + "\\TangoToolNew";
    }

    private void environmentVariables()
    {
        Map<String, String> env = System.getenv();
        for (String envName : env.keySet())
        {
            System.out.format("%s=%s%n", envName, env.get(envName));
        }
    }

    protected void processWindowEvent(WindowEvent e)
    {
        // out("WindowEvent:" + e.getID());
        if (e.getID() == WindowEvent.WINDOW_CLOSING)
        {
            saveState();
            saveDatabases();
        }
        super.processWindowEvent(e);
        if (e.getID() == WindowEvent.WINDOW_CLOSING)
        {
            System.exit(0);
        }
    }

    public void windowClosing(WindowEvent e)
    {
        out("windowClosing()");
    }

    private void out(String st)
    {
        System.out.println(st);
    }

    public static void main(String[] args)
    {
        new MainForTTNew().setVisible(true);
    }

    private String getConfigFileName()
    {
        Map<String, String> env = System.getenv();
        String appData = env.get("APPDATA");
        return appData + "\\TangoToolNew\\config.txt";
    }

    @Override
    public void valueChanged(ListSelectionEvent e)
    {
        // if (showTrackCheckBox.isSelected())
        // resetTrackCheckBox();
    }

    private class Statistics
    {
        int total;
        float totalTrackTime;
        int totalTandas;
        int totalPlayslists;
    }

    @Override
    public void tableChanged(TableModelEvent e)
    {
        Utilities.out("tableChanged()");
        redoTrackInTandasColumn();
    }

    private boolean dataChanged()
    {
        if (mTrackTableModel.isChanged())
            Utilities.msg("mTrackTable is changed");
        if (mTandaTreeModel.isChanged())
            Utilities.msg("mTandaTreeModel is changed");
        if (mPlaylistTreeModel.isChanged())
            Utilities.msg("mPlaylistTreeModel is changed");
        return mTrackTableModel.isChanged() || mTandaTreeModel.isChanged() || mPlaylistTreeModel.isChanged();
    }

    private void saveDatabases()
    {
        if (dataChanged())
        {
            int rc = JOptionPane.showConfirmDialog(null, "Save databases?", "Hello?", JOptionPane.YES_NO_OPTION);
            if (rc == JOptionPane.NO_OPTION)
                return;
        }
        else
            return;
        try
        {
            JOptionPane.showMessageDialog(null, "Press OK and wait for confirmation...");
            boolean rc = backupDatabaseXML();
            if (rc != true)
            {
                JOptionPane.showMessageDialog(null, "Database.xml file rename for backup failed, data not saved:");
                return;
            }
            BufferedWriter writer = new BufferedWriter(new FileWriter(mMusicBasePath + "\\" + "database.xml"));
            rc = backupTracksXML();
            if (rc != true)
            {
                JOptionPane.showMessageDialog(null, "Tracks.xml file rename for backup failed, data not saved:");
                return;
            }
            String fno = mMusicBasePath + "\\" + "tracks.xml";
            mTrackTableModel.writeNew(fno);
            writer.write("***tracks***\n");
            mTandaTreeModel.write(writer);
            mPlaylistTreeModel.write(writer);
            writer.close();
            JOptionPane.showMessageDialog(null, "Data save confirmed");
            mTrackTableModel.setChanged(false);
            mTandaTreeModel.setChanged(false);
            mPlaylistTreeModel.setChanged(false);
        }
        catch (Exception e)
        {
            out("TrackTableModel.write() IOException:" + e.getMessage());
            return;
        }
        return;
    }

    private boolean backupDatabaseXML()
    {
        File file = new File(mMusicBasePath + "/" + "database.xml");
        String ts = Utilities.getTimeString();
        String newFileName = mMusicBasePath + "/" + "database-" + ts + ".bak";
        File backup = new File(newFileName);
        boolean rc = file.renameTo(backup);
        return rc;
    }

    private boolean backupTracksXML()
    {
        File file = new File(mMusicBasePath + "/" + "tracks.xml");
        String ts = Utilities.getTimeString();
        String newFileName = mMusicBasePath + "/" + "tracks-" + ts + ".bak";
        File backup = new File(newFileName);
        boolean rc = file.renameTo(backup);
        return rc;
    }

    private String normalizePath(String pathIn)
    {
        return pathIn.replace("/", File.separator);
    }

    // Go through all of the tandas and playlists and update elapsed time
    public void calculateTime()
    {
        float playlistTime = 0f;
        float tandaTime = 0f;
        float totalTime = 0f;
        // For each playlist
        for (Playlist playlist : mPlaylistTreeModel.mPlaylists)
        {
            // For each tanda
            for (long aaa : playlist.getTandaIDs())
            {
                Tanda tanda = mTandaTreeModel.getTandaByUniqueId(aaa);
                if (tanda == null)
                {
                    Utilities.out("Playlist " + " " + playlist.uniqueId + " has a missing tanda:" + aaa);
                    continue;
                }
                // Utilities.out(null);
                // For each track
                for (Long bbb : tanda.mTracks)
                {
                    Track track = mTrackTableModel.getTrackbyUniqueId(bbb);
                    tandaTime += track.calculatedTime;
                    // Utilities.out("Track:"+track.uniqueId+" "+track.calculatedTime+" seconds");
                }
                tanda.calculatedTime = tandaTime;
                // Utilities.out("Tanda:"+tanda.uniqueId+" "+tanda.calculatedTime+" seconds");
                playlistTime += tandaTime;
                tandaTime = 0f;
            }
            playlist.calculatedTime = playlistTime;
            playlistTime = 0f;
            // Utilities.out("Playlist:"+playlist.uniqueId+" "+playlist.calculatedTime+"
            // seconds");
        }
    }

    @Override
    public void itemStateChanged(ItemEvent e)
    {
        Utilities.out("itemStateChanged");
        Object obj = e.getSource();
        JCheckBox jcb = (JCheckBox) obj;
        String name = jcb.getName();
        if (name.equalsIgnoreCase("last10"))
        {
            boolean sel = jcb.isSelected();
            if (sel)
            {
                mFramePosition = 500000;
                mouseEvents.setFramePosition(500000);
                Utilities.out("Frame position set to 500000");
            }
            else
            {
                mFramePosition = 0;
                mouseEvents.setFramePosition(0);
                Utilities.out("Frame position set to 0");
            }
            return;
        }
        else if (name.equalsIgnoreCase("showtrack"))
        {
            boolean boo = jcb.isSelected();
            Utilities.out("sel:" + boo);
            if (!showTrackCheckBox.isSelected())
            {
                mTandaTreeModel.reset();
                // showTrackCheckBox.setSelected(false);
                mTandaTreeModel.notifyTreeModelHasChanged();
                return;
            }
            int row = mTrackTable.getSelectedRow();
            if (row == -1)
            {
                jcb.setSelected(false);
                return;
            }
            Object ooo = mTrackTable.getValueAt(row, 6);
            long UID;
            if (ooo instanceof Long)
            {
                UID = ((Long) ooo).longValue();
            }
            else
                UID = Long.parseLong((String) obj);
            int count = mTandaTreeModel.selectTandas(UID);
            mTandaTreeModel.notifyTreeModelHasChanged();
            return;
        }
    }

    @Override
    public void treeNodesChanged(TreeModelEvent e)
    {
        Utilities.out("treeNodesChanged()");
    }

    @Override
    public void treeNodesInserted(TreeModelEvent e)
    {
        Utilities.out("treeNodesInserted()");
    }

    @Override
    public void treeNodesRemoved(TreeModelEvent e)
    {
        Utilities.out("treeNodesRemoved()");
    }

    @Override
    public void treeStructureChanged(TreeModelEvent e)
    {
        Utilities.out("treeStructureChanged()" + e.getSource());
        Utilities.out("Recalculating time");
        calculateTime();
        Utilities.out("Done recalculating time");
    }

    private void setupRemoteKeyBindings()
    {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e ->
        {
            if (e.getID() != KeyEvent.KEY_PRESSED) return false;
            if (KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow() != MainForTTNew.this)
                return false;
            // Only intercept when a track is selected in the playlist, or playback is active
            TreePath tp = mPlaylistTree.getSelectionPath();
            boolean trackSelected = tp != null && tp.getLastPathComponent() instanceof Track;
            if (!trackSelected && !bContinuousPlaying && !bPaused) return false;
            switch (e.getKeyCode())
            {
                case KeyEvent.VK_P:
                case KeyEvent.VK_ESCAPE: remoteTogglePlayback(); return true;
                case KeyEvent.VK_DOWN:   remoteNext();     return true;
                case KeyEvent.VK_UP:     remotePrevious(); return true;
                default: return false;
            }
        });
    }

    private void remoteTogglePlayback()
    {
        TreePath path = mPlaylistTree.getSelectionPath();
        if (path == null || !(path.getLastPathComponent() instanceof Track)) return;

        if (bContinuousPlaying && !bPaused)
        {
            // Playing → pause in place
            bPaused = true;
            mClip.stop();
            startPlaylistButton.setText("Resume Playlist");
            return;
        }

        if (bPaused)
        {
            // Paused → resume
            bPaused = false;
            mClip.start();
            startPlaylistButton.setText("Pause Playlist");
            return;
        }

        // Stopped → start playlist from selected track
        Object[] objs = path.getPath();
        Playlist playlist = (Playlist) objs[1];
        Tanda tanda       = (Tanda)    objs[2];
        Track track       = (Track)    objs[3];
        mCurrentlyPlayingPlaylist = playlist;
        mCurrentlyPlayingPlaylist.currentlyPlayingTanda = tanda.uniqueId;
        mCurrentlyPlayingPlaylist.currentlyPlayingTrack = track.uniqueId;
        bContinuousPlaying = true;
        startPlaylistButton.setText("Pause Playlist");
        stopPlaylistButton.setEnabled(true);
        nextSongButton.setEnabled(true);
        play(track.uniqueId);
    }

    private void remoteNext()
    {
        TreePath path = mPlaylistTree.getSelectionPath();
        if (path == null || !(path.getLastPathComponent() instanceof Track)) return;

        Object[] objs   = path.getPath();
        Playlist playlist = (Playlist) objs[1];
        Tanda tanda       = (Tanda)    objs[2];
        Track track       = (Track)    objs[3];

        long nextTrackID = mTandaTreeModel.getNextTrackID(tanda.uniqueId, track.uniqueId);
        Tanda nextTanda = tanda;
        if (nextTrackID == -1L)
        {
            nextTanda = getNextTandaInPlaylist(playlist, tanda);
            if (nextTanda == null) return; // already at end of playlist
            nextTrackID = mTandaTreeModel.getFirstTrackID(nextTanda.uniqueId);
        }

        stopPlayback();

        // Expand tanda if needed, then select the track
        Track nextTrack = mTrackTableModel.getTrackbyUniqueId(nextTrackID);
        Object[] newPath = { objs[0], playlist, nextTanda, nextTrack };
        TreePath newTreePath = new TreePath(newPath);
        mPlaylistTree.expandPath(new TreePath(new Object[]{ objs[0], playlist, nextTanda }));
        mPlaylistTree.setSelectionPath(newTreePath);
        mPlaylistTree.scrollPathToVisible(newTreePath);
    }

    private void remotePrevious()
    {
        TreePath path = mPlaylistTree.getSelectionPath();
        if (path == null || !(path.getLastPathComponent() instanceof Track)) return;

        Object[] objs     = path.getPath();
        Playlist playlist = (Playlist) objs[1];
        Tanda tanda       = (Tanda)    objs[2];
        Track track       = (Track)    objs[3];

        long now = System.currentTimeMillis();
        boolean quickDoubleTap = (now - mLastPreviousKeyTime) < 2000L;

        if ((bContinuousPlaying || bPaused) && !quickDoubleTap)
        {
            // Single press while playing or paused: restart current track.
            // setFramePosition(0) jumps to the start without stopping the clip,
            // so LineEvent.STOP does not fire and auto-advance is not triggered.
            mLastPreviousKeyTime = now; // arm the double-tap window for a possible follow-up press
            if (mClip != null)
            {
                mClip.setFramePosition(0);
                if (bPaused)
                {
                    mClip.start();
                    bPaused = false;
                    startPlaylistButton.setText("Pause Playlist");
                }
            }
            return;
        }

        // Double tap while playing/paused, or not playing: go to previous track.
        // Reset timer so the next back press is always treated as a single press.
        mLastPreviousKeyTime = 0L;
        long prevTrackID = mTandaTreeModel.getPreviousTrackID(tanda.uniqueId, track.uniqueId);
        Tanda prevTanda = tanda;
        if (prevTrackID == -1L)
        {
            prevTanda = getPreviousTandaInPlaylist(playlist, tanda);
            if (prevTanda == null) return; // already at start of playlist
            prevTrackID = mTandaTreeModel.getLastTrackID(prevTanda.uniqueId);
        }

        stopPlayback();

        Track prevTrack = mTrackTableModel.getTrackbyUniqueId(prevTrackID);
        Object[] newPath = { objs[0], playlist, prevTanda, prevTrack };
        TreePath newTreePath = new TreePath(newPath);
        mPlaylistTree.expandPath(new TreePath(new Object[]{ objs[0], playlist, prevTanda }));
        mPlaylistTree.setSelectionPath(newTreePath);
        mPlaylistTree.scrollPathToVisible(newTreePath);
    }

    private void stopPlayback()
    {
        bContinuousPlaying = false; // set first so LineEvent STOP handler takes the safe path
        bPaused = false;
        if (mClip != null)
        {
            mClip.stop();
            mClip.close();
            mClip = null;
        }
        if (mCurrentlyPlayingPlaylist != null)
        {
            mCurrentlyPlayingPlaylist.currentlyPlayingTanda = 0L;
            mCurrentlyPlayingPlaylist.currentlyPlayingTrack = 0L;
            mCurrentlyPlayingPlaylist = null;
        }
        startPlaylistButton.setText("Start Playlist");
        stopPlaylistButton.setEnabled(false);
        nextSongButton.setEnabled(false);
        bottomLine.setText(" ");
    }

    private Tanda getNextTandaInPlaylist(Playlist playlist, Tanda tanda)
    {
        long[] ids = playlist.getTandaIDs();
        for (int i = 0; i < ids.length - 1; i++)
            if (ids[i] == tanda.uniqueId)
                return mTandaTreeModel.getTandaByUniqueId(ids[i + 1]);
        return null;
    }

    private Tanda getPreviousTandaInPlaylist(Playlist playlist, Tanda tanda)
    {
        long[] ids = playlist.getTandaIDs();
        for (int i = 1; i < ids.length; i++)
            if (ids[i] == tanda.uniqueId)
                return mTandaTreeModel.getTandaByUniqueId(ids[i - 1]);
        return null;
    }
}
