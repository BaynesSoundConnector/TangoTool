package pkgForTTNew;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;

/**
 * Shows measured LUFS for every track in a playlist (Title/Orchestra/LUFS,
 * scrollable), with an optional "Normalize to" target that applies
 * TrackNormalizer to every track in the list in one action.
 */
public class LufsReportDialog extends JDialog
{
    private static final long serialVersionUID = 1L;
    public boolean bChanged = false;

    private final List<Track> mTracks;
    private final String mMusicBasePath;
    private final ReportTableModel mTableModel;
    private final JCheckBox normalizeCheckBox = new JCheckBox("Normalize to:");
    private final JTextField targetLufsField = new JTextField(String.valueOf(AddFolderDialog.DEFAULT_TARGET_LUFS));
    private final JProgressBar progressBar = new JProgressBar();
    private JButton okButton;
    private JButton closeButton;

    public LufsReportDialog(String playlistName, List<Track> tracks, String musicBasePath)
    {
        super((JDialog) null, "LUFS Report: " + playlistName, true);
        mTracks = tracks;
        mMusicBasePath = musicBasePath;
        mTableModel = new ReportTableModel(mTracks);

        JTable table = new JTable(mTableModel);
        table.getColumnModel().getColumn(0).setPreferredWidth(220);
        table.getColumnModel().getColumn(1).setPreferredWidth(160);
        table.getColumnModel().getColumn(2).setPreferredWidth(60);

        setLayout(new BorderLayout(8, 8));
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(500, 400));
        add(scrollPane, BorderLayout.CENTER);
        add(buildBottomPanel(), BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(null);

        measureMissingInBackground();
    }

    private JPanel buildBottomPanel()
    {
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(progressBar, BorderLayout.NORTH);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.add(normalizeCheckBox);
        targetLufsField.setColumns(5);
        controls.add(targetLufsField);
        controls.add(new JLabel("LUFS"));
        bottom.add(controls, BorderLayout.WEST);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        okButton = new JButton("OK");
        okButton.addActionListener(e -> onOk());
        closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        buttons.add(okButton);
        buttons.add(closeButton);
        bottom.add(buttons, BorderLayout.EAST);
        return bottom;
    }

    // Tracks that already have a cached measuredLufs (e.g. previously normalized)
    // aren't re-measured -- the report just reflects current state.
    private void measureMissingInBackground()
    {
        List<Track> toMeasure = new ArrayList<>();
        for (Track t : mTracks)
            if (t.measuredLufs == null)
                toMeasure.add(t);
        if (toMeasure.isEmpty())
            return;

        progressBar.setMaximum(toMeasure.size());
        progressBar.setStringPainted(true);
        progressBar.setString("Measuring 0 / " + toMeasure.size());

        SwingWorker<Void, Integer> worker = new SwingWorker<>()
        {
            @Override
            protected Void doInBackground()
            {
                int completed = 0;
                for (Track t : toMeasure)
                {
                    try
                    {
                        File f = new File(mMusicBasePath + "\\" + t.relativePath + "\\" + t.fileName);
                        t.measuredLufs = TrackNormalizer.measureLufs(f);
                        bChanged = true;
                    }
                    catch (Exception ex)
                    {
                        Utilities.out("LUFS measure failed for " + t.fileName + ": " + ex.getMessage());
                    }
                    completed++;
                    publish(completed);
                }
                return null;
            }

            @Override
            protected void process(List<Integer> chunks)
            {
                progressBar.setValue(chunks.get(chunks.size() - 1));
                progressBar.setString("Measuring " + chunks.get(chunks.size() - 1) + " / " + toMeasure.size());
                mTableModel.fireTableDataChanged();
            }

            @Override
            protected void done()
            {
                progressBar.setString("Done");
                mTableModel.fireTableDataChanged();
            }
        };
        worker.execute();
    }

    private void onOk()
    {
        if (!normalizeCheckBox.isSelected())
        {
            dispose();
            return;
        }
        double targetLufs;
        try
        {
            targetLufs = Double.parseDouble(targetLufsField.getText().trim());
        }
        catch (NumberFormatException ex)
        {
            JOptionPane.showMessageDialog(this, "Target LUFS must be a number.");
            return;
        }
        runNormalizeAll(targetLufs);
    }

    private void runNormalizeAll(double targetLufs)
    {
        okButton.setEnabled(false);
        closeButton.setEnabled(false);
        normalizeCheckBox.setEnabled(false);
        targetLufsField.setEnabled(false);
        progressBar.setValue(0);
        progressBar.setMaximum(mTracks.size());
        progressBar.setStringPainted(true);
        progressBar.setString("Normalizing 0 / " + mTracks.size());

        SwingWorker<Void, Integer> worker = new SwingWorker<>()
        {
            List<String> clippedTitles = new ArrayList<>();
            List<String> failedTitles = new ArrayList<>();

            @Override
            protected Void doInBackground()
            {
                int completed = 0;
                for (Track t : mTracks)
                {
                    try
                    {
                        File f = new File(mMusicBasePath + "\\" + t.relativePath + "\\" + t.fileName);
                        TrackNormalizer.Result result = TrackNormalizer.normalize(f, targetLufs);
                        t.fileName = result.finalFile().getName();
                        String ext = AddFolderDialog.getFileExtension(t.fileName).toLowerCase();
                        t.fileType = ext.equals("flac") ? Track.Type.FLAC
                                : ext.equals("mp3") ? Track.Type.MP3 : Track.Type.WAV;
                        t.calculatedTime = SoundUtils.getLength(result.finalFile().getPath());
                        t.songTime = SoundUtils.formatIntoMMSS(Math.round(t.calculatedTime));
                        t.measuredLufs = result.finalLufs();
                        if (result.clipped())
                            clippedTitles.add(t.title);
                    }
                    catch (Exception ex)
                    {
                        Utilities.out("Normalize failed for " + t.fileName + ": " + ex.getMessage());
                        failedTitles.add(t.title);
                    }
                    completed++;
                    publish(completed);
                }
                return null;
            }

            @Override
            protected void process(List<Integer> chunks)
            {
                progressBar.setValue(chunks.get(chunks.size() - 1));
                progressBar.setString("Normalizing " + chunks.get(chunks.size() - 1) + " / " + mTracks.size());
                mTableModel.fireTableDataChanged();
            }

            @Override
            protected void done()
            {
                bChanged = true;
                mTableModel.fireTableDataChanged();
                StringBuilder sb = new StringBuilder();
                if (!clippedTitles.isEmpty())
                {
                    sb.append("Clipped and hard-limited to 0 dBFS:\n");
                    for (String s : clippedTitles)
                        sb.append(s).append("\n");
                }
                if (!failedTitles.isEmpty())
                {
                    sb.append("\nFailed to normalize:\n");
                    for (String s : failedTitles)
                        sb.append(s).append("\n");
                }
                if (sb.length() > 0)
                    JOptionPane.showMessageDialog(LufsReportDialog.this, sb.toString());
                dispose();
            }
        };
        worker.execute();
    }

    private static class ReportTableModel extends AbstractTableModel
    {
        private static final long serialVersionUID = 1L;
        private final List<Track> tracks;
        private final String[] cols = { "Title", "Orchestra", "LUFS" };

        ReportTableModel(List<Track> tracks)
        {
            this.tracks = tracks;
        }

        @Override
        public int getRowCount()
        {
            return tracks.size();
        }

        @Override
        public int getColumnCount()
        {
            return cols.length;
        }

        @Override
        public String getColumnName(int col)
        {
            return cols[col];
        }

        @Override
        public Object getValueAt(int row, int col)
        {
            Track t = tracks.get(row);
            switch (col)
            {
                case 0:
                    return t.title;
                case 1:
                    return t.orchestra;
                case 2:
                    return t.measuredLufs == null ? "…" : String.format("%.1f", t.measuredLufs);
                default:
                    return "";
            }
        }
    }
}
