package pkgForTTNew;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

/**
 * Mass background LUFS measurement across the whole library, filling in
 * Track.measuredLufs for every track that doesn't already have one. Read-only
 * -- never touches audio files, just populates the database field so the
 * LUFS column (and per-playlist LUFS Report) don't need to re-measure later.
 * Non-modal so the rest of the app stays usable for the many minutes a full
 * library pass can take.
 */
public class CalculateLufsDialog extends JDialog
{
    private static final long serialVersionUID = 1L;
    private final TrackTableModel mTrackTableModel;
    private final String mMusicBasePath;
    private final JProgressBar progressBar = new JProgressBar();
    private final JLabel statusLabel = new JLabel(" ");
    private final JButton actionButton = new JButton("Cancel");
    private SwingWorker<Void, Integer> worker;

    public CalculateLufsDialog(TrackTableModel trackTableModel, String musicBasePath)
    {
        super((JFrame) null, "Calculate LUFS", false);
        mTrackTableModel = trackTableModel;
        mMusicBasePath = musicBasePath;

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(statusLabel, BorderLayout.NORTH);
        panel.add(progressBar, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionButton.addActionListener(e ->
        {
            if (worker != null && !worker.isDone())
                worker.cancel(false);
            else
                dispose();
        });
        buttons.add(actionButton);
        panel.add(buttons, BorderLayout.SOUTH);
        getContentPane().add(panel);
        setSize(420, 130);
        setLocationRelativeTo(null);

        start();
    }

    private void start()
    {
        List<Track> toMeasure = new ArrayList<>();
        for (Track t : mTrackTableModel.mTracks)
            if (t.measuredLufs == null)
                toMeasure.add(t);

        if (toMeasure.isEmpty())
        {
            statusLabel.setText("All tracks already have a measured LUFS value.");
            actionButton.setText("Close");
            return;
        }

        progressBar.setMaximum(toMeasure.size());
        progressBar.setStringPainted(true);
        statusLabel.setText("Calculating LUFS: 0 / " + toMeasure.size());

        worker = new SwingWorker<Void, Integer>()
        {
            int measured = 0;
            int failed = 0;

            @Override
            protected Void doInBackground()
            {
                int completed = 0;
                for (Track t : toMeasure)
                {
                    if (isCancelled())
                        break;
                    try
                    {
                        File f = new File(mMusicBasePath + "\\" + t.relativePath + "\\" + t.fileName);
                        t.measuredLufs = TrackNormalizer.measureLufs(f);
                        measured++;
                    }
                    catch (Exception ex)
                    {
                        Utilities.out("LUFS measure failed for " + t.fileName + ": " + ex.getMessage());
                        failed++;
                    }
                    completed++;
                    publish(completed);
                }
                return null;
            }

            @Override
            protected void process(List<Integer> chunks)
            {
                int completed = chunks.get(chunks.size() - 1);
                progressBar.setValue(completed);
                statusLabel.setText("Calculating LUFS: " + completed + " / " + toMeasure.size());
                mTrackTableModel.fireTableDataChanged();
            }

            @Override
            protected void done()
            {
                mTrackTableModel.setChanged(true);
                mTrackTableModel.fireTableDataChanged();
                statusLabel.setText("Done: " + measured + " measured"
                        + (failed > 0 ? ", " + failed + " failed" : "")
                        + (isCancelled() ? " (cancelled)" : "") + ". Remember to Save.");
                actionButton.setText("Close");
            }
        };
        worker.execute();
    }
}
