package pkgForTTNew;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import pkgForTTNew.Track.Style;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
public class AddFolderDialog extends JDialog implements ActionListener
{
    private static final long serialVersionUID = 1L;
    public static final double DEFAULT_TARGET_LUFS = -15.0;
    public boolean bChanged = false;
    File folder;
    TrackTableModel mTrackTableModel;
    ArrayList<File> filesToAdd;
    JTextField orchestra = new JTextField();
    JTextField album = new JTextField();
    JTextField comment = new JTextField();
    JCheckBox normalizeCheckBox = new JCheckBox("Normalize to LUFS:");
    JTextField targetLufsField = new JTextField(String.valueOf(DEFAULT_TARGET_LUFS));
    JProgressBar progressBar = new JProgressBar();
    JButton okButton;
    JButton cancelButton;

    // Update an existing track
    public AddFolderDialog(String path, TrackTableModel trackTableModel)
    {
        super((JDialog) null, true);
        folder = new File(path);
        mTrackTableModel = trackTableModel;
        JPanel mainPanel = setup();
        createPanel(mainPanel);
    }

    private JPanel setup()
    {
        setBounds(100, 100, 800, 340);
        JPanel mainPanel = new JPanel(new GridLayout(7, 2));
        mainPanel.setBorder(BorderFactory.createEtchedBorder());
        this.getContentPane().add(mainPanel, BorderLayout.CENTER);
        this.getContentPane().add(progressBar, BorderLayout.SOUTH);
        return mainPanel;
    }

    private void createPanel(JPanel mainPanel)
    {
        mainPanel.add(createLabel("Folder:"));
        mainPanel.add(createLabel(folder.getPath()));
        mainPanel.add(createLabel("Orchestra:"));
        mainPanel.add(createField(folder.getName(), orchestra));
        mainPanel.add(createLabel("Album:"));
        mainPanel.add(createField(folder.getName(), album));
        mainPanel.add(createLabel("Comment:"));
        mainPanel.add(createField("", comment));
        mainPanel.add(createNormalizeRow());
        mainPanel.add(new JPanel());
        File[] files = folder.listFiles();
        onlyAudioFiles(files);
        String msg = "Add " + filesToAdd.size() + " tracks?";
        mainPanel.add(createLabel(msg));
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

    private JPanel createField(String textContent, JTextField textField)
    {
        JPanel jp = new JPanel();
        textField.setColumns(30);
        jp.setLayout(new FlowLayout(FlowLayout.LEFT));
        textField.setText(textContent);
        jp.add((textField));
        return jp;
    }

    private void onlyAudioFiles(File[] in)
    {
        filesToAdd = new ArrayList<File>();
        for (File file : in)
        {
            String ext = getFileExtension(file.getName());
            if (ext.equalsIgnoreCase("wav") || ext.equalsIgnoreCase("flac") || ext.equalsIgnoreCase("mp3"))
                filesToAdd.add(file);
        }
    }

    public static String getFileExtension(String filePath)
    {
        int lastIndexOfDot = filePath.lastIndexOf('.');
        if (lastIndexOfDot == -1)
        {
            return "No extension";
        }
        return filePath.substring(lastIndexOfDot + 1);
    }

    public static String stripFileExtension(String filePath)
    {
        int lastIndexOfDot = filePath.lastIndexOf('.');
        if (lastIndexOfDot == -1)
            return "No extension";
        else
        {
            return filePath.substring(0, lastIndexOfDot);
        }
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
        okButton = new JButton("OK");
        okButton.addActionListener(this);
        panel.add(okButton);
        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(this);
        panel.add(cancelButton);
        return panel;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        if (e.getActionCommand().equals("OK"))
        {
            boolean normalize = normalizeCheckBox.isSelected();
            double targetLufs = DEFAULT_TARGET_LUFS;
            if (normalize)
            {
                try
                {
                    targetLufs = Double.parseDouble(targetLufsField.getText().trim());
                }
                catch (NumberFormatException ex)
                {
                    JOptionPane.showMessageDialog(this, "Target LUFS must be a number.");
                    return;
                }
            }

            if (!normalize)
            {
                addTracksToModel(filesToAdd, orchestra.getText(), album.getText());
                this.dispose();
                return;
            }

            runNormalizeAndAdd(targetLufs);
        }
        else if (e.getActionCommand().equals("Cancel"))
        {
            this.dispose();
        }
    }

    private void runNormalizeAndAdd(double targetLufs)
    {
        okButton.setEnabled(false);
        cancelButton.setEnabled(false);
        normalizeCheckBox.setEnabled(false);
        targetLufsField.setEnabled(false);
        progressBar.setMaximum(filesToAdd.size());
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        progressBar.setString("Normalizing 0 / " + filesToAdd.size());

        final String orchestraText = orchestra.getText();
        final String albumText = album.getText();

        SwingWorker<List<File>, Integer> worker = new SwingWorker<>()
        {
            List<File> finalFiles = new ArrayList<>();
            List<String> clippedFiles = new ArrayList<>();
            List<String> failedFiles = new ArrayList<>();

            @Override
            protected List<File> doInBackground()
            {
                int completed = 0;
                for (File file : filesToAdd)
                {
                    try
                    {
                        TrackNormalizer.Result result = TrackNormalizer.normalize(file, targetLufs);
                        finalFiles.add(result.finalFile());
                        if (result.clipped())
                            clippedFiles.add(result.finalFile().getName());
                    }
                    catch (Exception ex)
                    {
                        Utilities.out("Normalize failed for " + file.getPath() + ": " + ex.getMessage());
                        failedFiles.add(file.getName());
                        finalFiles.add(file);
                    }
                    completed++;
                    publish(completed);
                }
                return finalFiles;
            }

            @Override
            protected void process(List<Integer> chunks)
            {
                int completed = chunks.get(chunks.size() - 1);
                progressBar.setValue(completed);
                progressBar.setString("Normalizing " + completed + " / " + filesToAdd.size());
            }

            @Override
            protected void done()
            {
                try
                {
                    List<File> results = get();
                    addTracksToModel(results, orchestraText, albumText);
                    reportSummary(clippedFiles, failedFiles);
                }
                catch (Exception ex)
                {
                    JOptionPane.showMessageDialog(AddFolderDialog.this,
                            "Error during normalization: " + ex.getMessage());
                }
                AddFolderDialog.this.dispose();
            }
        };
        worker.execute();
    }

    private void reportSummary(List<String> clippedFiles, List<String> failedFiles)
    {
        if (clippedFiles.isEmpty() && failedFiles.isEmpty())
            return;
        StringBuilder sb = new StringBuilder();
        if (!clippedFiles.isEmpty())
        {
            sb.append("The following files clipped and were hard-limited to 0 dBFS:\n");
            for (String s : clippedFiles) sb.append(s).append("\n");
        }
        if (!failedFiles.isEmpty())
        {
            sb.append("\nThe following files could not be normalized and were added unmodified:\n");
            for (String s : failedFiles) sb.append(s).append("\n");
        }
        showScrollableMessage("Normalization Summary", sb.toString());
    }

    private static void showScrollableMessage(String title, String message)
    {
        JTextArea textArea = new JTextArea(message);
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new java.awt.Dimension(500, 400));
        JOptionPane.showMessageDialog(null, scrollPane, title, JOptionPane.INFORMATION_MESSAGE);
    }

    private void addTracksToModel(List<File> files, String orchestraText, String albumText)
    {
        StringBuffer added = new StringBuffer();
        for (File file : files)
        {
            Track track = new Track();
            track.album = albumText;
            track.orchestra = orchestraText;
            track.calculatedTime = SoundUtils.getLength(file.getPath());
            if (track.calculatedTime == 0L)
            {
                track.status = Track.Status.Invalid;
                Utilities.out("Invalid music file:" + file.getPath());
                track.songTime = "";
            }
            else
            {
                track.status = Track.Status.Valid;
                track.songTime = SoundUtils.formatIntoMMSS(Math.round(track.calculatedTime));
            }
            track.title = stripFileExtension(file.getName());
            track.fileName = file.getName();
            track.relativePath = subtractBasePath(mTrackTableModel.musicBasePath, file.getParent());
            String ext = getFileExtension(file.getName()).toLowerCase();
            track.fileType = ext.equals("flac") ? Track.Type.FLAC : ext.equals("mp3") ? Track.Type.MP3 : Track.Type.WAV;
            track.style = Track.Style.Unknown;
            track.normalizedOrchestra = SearchTermBuilder.stripAccents(track.orchestra);
            track.searchTerm = SearchTermBuilder.buildSearchTerm(track.title, track.orchestra);
            added.append(track.title + "\n");
            mTrackTableModel.addTrack(track);
        }
        showScrollableMessage("Tracks Added", added.toString());
    }

    private static String subtractBasePath(String basePath, String pathIn)
    {
        int index = pathIn.toLowerCase().indexOf(basePath.toLowerCase());
        if (index == -1 || index != 0)
            return null;
        String out = pathIn.substring(basePath.length() + 1);
        return out;
    }
}
