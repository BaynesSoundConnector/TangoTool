package pkgForTTNew;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.List;
import pkgForTTNew.Track.Style;

import javax.accessibility.Accessible;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.plaf.basic.BasicComboPopup;

public class TrackDetailDialog extends JDialog implements ActionListener, DocumentListener
{
    private static final long serialVersionUID = 1L;
    public boolean bChanged = false;
    JTextField tTitle = new JTextField();
    JTextField tOrchestra = new JTextField();
    JTextField tYear = new JTextField();
    JLabel tTime = new JLabel();
    JComboBox<Style> cbStyle = new JComboBox<Style>(Track.Style.values());
    JTextField tAlbum = new JTextField();
    JCheckBox normalizeCheckBox = new JCheckBox("Normalize to LUFS:");
    JTextField targetLufsField = new JTextField(String.valueOf(AddFolderDialog.DEFAULT_TARGET_LUFS));
    private boolean bFieldChanged = false;
    private boolean bCreate = false;
    private boolean bBulk = false;
    Track mTrack;
    File mSourceFile;
    List<Track> mTracks;
    private static final String BULK_NO_STYLE_CHANGE = "(No change)";
    private JComboBox<Object> cbBulkStyle;
    private boolean bTitleDirty = false;
    private boolean bOrchestraDirty = false;
    private boolean bAlbumDirty = false;
    private boolean bYearDirty = false;
    private boolean bStyleDirty = false;

    // Update an existing track
    public TrackDetailDialog(Track track, String musicBasePath)
    {
        super((JDialog) null, true);
        bCreate = false;
        JPanel mainPanel = setup();
        mTrack = track;
        mSourceFile = new File(musicBasePath + "\\" + track.relativePath + "\\" + track.fileName);
        createPanel(mainPanel);
        finalizeSize();
    }

    // Create a new track
    public TrackDetailDialog(File file, String musicBasePath)
    {
        super((JDialog) null, true);
        bCreate = true;
        mSourceFile = file;
        JPanel mainPanel = setup();
        String fileNameAndPath = "";
        try
        {
            fileNameAndPath = file.getCanonicalPath();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        mTrack = new Track();
        mTrack.title = "";
        mTrack.year = "";
        mTrack.orchestra = "";
        mTrack.style = Track.Style.Unknown;
        mTrack.album = "";
        mTrack.calculatedTime = SoundUtils.getLength(fileNameAndPath);
        mTrack.songTime = SoundUtils.formatIntoMMSS(Math.round(mTrack.calculatedTime));
        mTrack.fileName = parseFilename(fileNameAndPath);
        mTrack.relativePath = parseRelativePath(fileNameAndPath, musicBasePath);
        if (mTrack.relativePath == null)
            return;
        createPanel(mainPanel);
        finalizeSize();
    }

    // Bulk-edit multiple tracks at once. Only fields the user actually
    // changes are applied, so unedited fields are left alone on every track.
    public TrackDetailDialog(List<Track> tracks)
    {
        super((JDialog) null, true);
        bBulk = true;
        mTracks = tracks;
        JPanel mainPanel = setup();
        createBulkPanel(mainPanel);
        finalizeSize();
    }

    // Size the dialog to exactly fit its content instead of guessing a
    // pixel height, which was clipping the OK/Cancel row and combo boxes.
    private void finalizeSize()
    {
        pack();
        Dimension packed = getSize();
        setSize(Math.max(packed.width, 780), packed.height);
        setLocation(100, 100);
    }

    public Track getTrack()
    {
        return mTrack;
    }

    private String parseFilename(String pathIn)
    {
        int index = pathIn.lastIndexOf(File.separator);
        return pathIn.substring(index + 1);
    }

    private String parseRelativePath(String pathIn, String musicBasePath)
    {
        String mbplc = musicBasePath.toLowerCase();
        String sflc = pathIn.toLowerCase();
        if (!sflc.startsWith(mbplc))
        {
            JOptionPane.showMessageDialog(null, "Selected file must be in " + musicBasePath);
            return null;
        }
        int index = pathIn.lastIndexOf(File.separator);
        String out = pathIn.substring(musicBasePath.length() + 1, index);
        return out;
    }

    private JPanel setup()
    {
        int rows = bBulk ? 7 : 10;
        JPanel mainPanel = new JPanel(new GridLayout(rows, 2, 6, 4));
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(),
                BorderFactory.createEmptyBorder(6, 10, 12, 10)));
        this.getContentPane().add(mainPanel);
        return mainPanel;
    }

    private void createBulkPanel(JPanel mainPanel)
    {
        mainPanel.add(createLabel(mTracks.size() + " tracks selected"));
        mainPanel.add(createLabel("Blank fields are left unchanged"));
        mainPanel.add(createLabel("Title"));
        mainPanel.add(createBulkField(tTitle, () -> bTitleDirty = true));
        mainPanel.add(createLabel("Orchestra"));
        mainPanel.add(createBulkField(tOrchestra, () -> bOrchestraDirty = true));
        mainPanel.add(createLabel("Style"));
        mainPanel.add(prepBulkStyle());
        mainPanel.add(createLabel("Album"));
        mainPanel.add(createBulkField(tAlbum, () -> bAlbumDirty = true));
        mainPanel.add(createLabel("Year Recorded"));
        mainPanel.add(createBulkField(tYear, () -> bYearDirty = true));
        mainPanel.add(createLabel(""));
        mainPanel.add(createokcancel());
    }

    private JPanel createBulkField(JTextField textField, Runnable markDirty)
    {
        JPanel jp = new JPanel();
        textField.setColumns(20);
        jp.setLayout(new FlowLayout(FlowLayout.LEFT));
        textField.getDocument().addDocumentListener(new DocumentListener()
        {
            @Override
            public void insertUpdate(DocumentEvent e) { markDirty.run(); }
            @Override
            public void removeUpdate(DocumentEvent e) { markDirty.run(); }
            @Override
            public void changedUpdate(DocumentEvent e) { markDirty.run(); }
        });
        jp.add(textField);
        return jp;
    }

    private JPanel prepBulkStyle()
    {
        JPanel jp = new JPanel(new FlowLayout(FlowLayout.LEFT));
        Style[] styles = Track.Style.values();
        Object[] items = new Object[styles.length + 1];
        items[0] = BULK_NO_STYLE_CHANGE;
        System.arraycopy(styles, 0, items, 1, styles.length);
        cbBulkStyle = new JComboBox<Object>(items);
        cbBulkStyle.setSelectedIndex(0);
        cbBulkStyle.addActionListener(e -> bStyleDirty = true);
        scrollPopupToTop(cbBulkStyle);
        jp.add(cbBulkStyle);
        return jp;
    }

    private void applyBulkEdits()
    {
        boolean styleDirty = bStyleDirty && cbBulkStyle.getSelectedItem() instanceof Style;
        if (!bTitleDirty && !bOrchestraDirty && !bAlbumDirty && !bYearDirty && !styleDirty)
            return;
        String newTitle = tTitle.getText().strip();
        String newOrchestra = tOrchestra.getText().strip();
        String newAlbum = tAlbum.getText().strip();
        String newYear = tYear.getText().strip();
        Style newStyle = styleDirty ? (Style) cbBulkStyle.getSelectedItem() : null;
        for (Track t : mTracks)
        {
            if (bTitleDirty)
                t.title = newTitle;
            if (bOrchestraDirty)
                t.orchestra = newOrchestra;
            if (bAlbumDirty)
                t.album = newAlbum;
            if (bYearDirty)
                t.year = newYear;
            if (styleDirty)
                t.style = newStyle;
            if (bTitleDirty || bOrchestraDirty)
            {
                t.normalizedOrchestra = SearchTermBuilder.stripAccents(t.orchestra);
                t.searchTerm = SearchTermBuilder.buildSearchTerm(t.title, t.orchestra);
            }
        }
        bChanged = true;
    }

    private void createPanel(JPanel mainPanel)
    {
        mainPanel.add(createLabel("Title"));
        mainPanel.add(createField(mTrack.title, tTitle));
        mainPanel.add(createLabel("Orchestra"));
        mainPanel.add(createField(mTrack.orchestra, tOrchestra));
        mainPanel.add(createLabel("Style"));
        mainPanel.add(prepStyle());
        mainPanel.add(createLabel("Album"));
        mainPanel.add(createField(mTrack.album, tAlbum));
        mainPanel.add(createLabel("Year Recorded"));
        mainPanel.add(createField(mTrack.year, tYear));
        mainPanel.add(createLabel("Time"));
        mainPanel.add(new JLabel(mTrack.songTime));
        mainPanel.add(createLabel("File"));
        mainPanel.add(createReadOnlyField(mTrack.fileName));
        mainPanel.add(createLabel("Directory"));
        mainPanel.add(createReadOnlyField(mTrack.relativePath));
        mainPanel.add(createNormalizeRow());
        mainPanel.add(new JPanel());
        mainPanel.add(createLabel(""));
        mainPanel.add(createokcancel());
    }

    private JPanel createNormalizeRow()
    {
        JPanel jp = new JPanel(new FlowLayout(FlowLayout.LEFT));
        jp.add(normalizeCheckBox);
        targetLufsField.setColumns(5);
        jp.add(targetLufsField);
        jp.add(new JLabel("LUFS"));
        return jp;
    }

    private JPanel prepStyle()
    {
        JPanel jp = new JPanel(new FlowLayout(FlowLayout.LEFT));
        // jp.setBorder(BorderFactory.createLineBorder(Color.black));
        // cbStyle.addItem(Track.Style.Tango.toString());
        // cbStyle.addItem(Track.Style.Valse.toString());
        // cbStyle.addItem(Track.Style.Milonga.toString());
        cbStyle.setSelectedItem(mTrack.style);
        cbStyle.addActionListener(this);
        scrollPopupToTop(cbStyle);
        jp.add(cbStyle);
        return jp;
    }

    // Most-used styles are listed first; always open the dropdown scrolled to
    // the top instead of wherever the currently-selected item happens to be.
    private void scrollPopupToTop(JComboBox<?> combo)
    {
        combo.addPopupMenuListener(new PopupMenuListener()
        {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e)
            {
                SwingUtilities.invokeLater(() ->
                {
                    Accessible a = combo.getAccessibleContext().getAccessibleChild(0);
                    if (a instanceof BasicComboPopup)
                        ((BasicComboPopup) a).getList().ensureIndexIsVisible(0);
                });
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {}
        });
    }

    private Component createLabel(String in)
    {
        JPanel jp = new JPanel();
        // jp.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        jp.setLayout(new FlowLayout(FlowLayout.LEFT));
        jp.add(new JLabel(in));
        return jp;
    }

    private Component createokcancel()
    {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton ok = new JButton("OK");
        ok.addActionListener(this);
        panel.add(ok);
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(this);
        panel.add(cancel);
        return panel;
    }

    private JPanel createReadOnlyField(String textContent)
    {
        JPanel jp = new JPanel();
        JTextField textField = new JTextField(textContent);
        textField.setColumns(20);
        textField.setEditable(false);
        textField.setToolTipText(textContent);
        jp.setLayout(new FlowLayout(FlowLayout.LEFT));
        jp.add(textField);
        return jp;
    }

    private JPanel createField(String textContent, JTextField textField)
    {
        JPanel jp = new JPanel();
        textField.setColumns(20);
        jp.setLayout(new FlowLayout(FlowLayout.LEFT));
        textField.setText(textContent);
        textField.getDocument().addDocumentListener(this);
        jp.add((textField));
        return jp;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        if (e.getActionCommand().equals("OK"))
        {
            if (bBulk)
            {
                applyBulkEdits();
                this.dispose();
                return;
            }
            if (bCreate)
            {
                mTrack.title = tTitle.getText().strip();
                mTrack.orchestra = tOrchestra.getText().strip();
                mTrack.album = tAlbum.getText().strip();
                mTrack.year = tYear.getText().strip();
                mTrack.style = (Style) cbStyle.getSelectedItem();
                mTrack.normalizedOrchestra = SearchTermBuilder.stripAccents(mTrack.orchestra);
                mTrack.searchTerm = SearchTermBuilder.buildSearchTerm(mTrack.title, mTrack.orchestra);
                if (normalizeCheckBox.isSelected() && !applyNormalization())
                    return;
                bChanged = true;
                this.dispose();
            }
            if (bFieldChanged)
            {
                mTrack.title = tTitle.getText().strip();
                mTrack.orchestra = tOrchestra.getText().strip();
                mTrack.album = tAlbum.getText().strip();
                mTrack.year = tYear.getText().strip();
                if (mTrack.style != (Style) cbStyle.getSelectedItem())
                {
                    mTrack.style = (Style) cbStyle.getSelectedItem();
                }
                mTrack.normalizedOrchestra = SearchTermBuilder.stripAccents(mTrack.orchestra);
                mTrack.searchTerm = SearchTermBuilder.buildSearchTerm(mTrack.title, mTrack.orchestra);
                bChanged = true;
            }
            if (!bCreate && normalizeCheckBox.isSelected())
            {
                if (!applyNormalization())
                    return;
                bChanged = true;
            }
            this.dispose();
        }
        else if (e.getActionCommand().equals("Cancel"))
        {
            this.dispose();
        }
        else if (e.getActionCommand().equals("comboBoxChanged"))
        {
            bFieldChanged = true;
        }
    }

    private boolean applyNormalization()
    {
        double targetLufs;
        try
        {
            targetLufs = Double.parseDouble(targetLufsField.getText().trim());
        }
        catch (NumberFormatException ex)
        {
            JOptionPane.showMessageDialog(this, "Target LUFS must be a number.");
            return false;
        }
        try
        {
            TrackNormalizer.Result result = TrackNormalizer.normalize(mSourceFile, targetLufs);
            mTrack.fileName = result.finalFile().getName();
            String ext = AddFolderDialog.getFileExtension(mTrack.fileName).toLowerCase();
            mTrack.fileType = ext.equals("flac") ? Track.Type.FLAC : ext.equals("mp3") ? Track.Type.MP3 : Track.Type.WAV;
            mTrack.calculatedTime = SoundUtils.getLength(result.finalFile().getPath());
            mTrack.songTime = SoundUtils.formatIntoMMSS(Math.round(mTrack.calculatedTime));
            mTrack.measuredLufs = result.finalLufs();
            if (result.clipped())
                JOptionPane.showMessageDialog(this, mTrack.fileName + " clipped and was hard-limited to 0 dBFS.");
            return true;
        }
        catch (Exception ex)
        {
            JOptionPane.showMessageDialog(this, "Could not normalize file: " + ex.getMessage());
            return false;
        }
    }

    @Override
    public void insertUpdate(DocumentEvent e)
    {
        bFieldChanged = true;
    }

    @Override
    public void removeUpdate(DocumentEvent e)
    {
        bFieldChanged = true;
    }

    @Override
    public void changedUpdate(DocumentEvent e)
    {
        bFieldChanged = true;
    }
}
