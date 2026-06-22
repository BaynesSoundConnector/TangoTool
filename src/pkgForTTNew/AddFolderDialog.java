package pkgForTTNew;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import pkgForTTNew.Track.Style;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
public class AddFolderDialog extends JDialog implements ActionListener
{
    private static final long serialVersionUID = 1L;
    public boolean bChanged = false;
    File folder;
    TrackTableModel mTrackTableModel;
    ArrayList<File> filesToAdd;
    JTextField orchestra = new JTextField();
    JTextField album = new JTextField();
    JTextField comment = new JTextField();

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
        setBounds(100, 100, 800, 300);
        JPanel mainPanel = new JPanel(new GridLayout(6, 2));
        mainPanel.setBorder(BorderFactory.createEtchedBorder());
        this.getContentPane().add(mainPanel);
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
        File[] files = folder.listFiles();
        onlyAudioFiles(files);
        String msg = "Add " + filesToAdd.size() + " tracks?";
        mainPanel.add(createLabel(msg));
        mainPanel.add(createokcancel());
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
            if (ext.equalsIgnoreCase("wav") || ext.equalsIgnoreCase("flac"))
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
        JButton ok = new JButton("OK");
        ok.addActionListener(this);
        panel.add(ok);
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(this);
        panel.add(cancel);
        return panel;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        StringBuffer added = new StringBuffer();
        if (e.getActionCommand().equals("OK"))
        {
            for (File file : filesToAdd)
            {
                Track track = new Track();
                track.album = album.getText();
                track.orchestra = orchestra.getText();
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
                track.fileType = ext.equals("flac") ? Track.Type.FLAC : Track.Type.WAV;
                track.style = Track.Style.Unknown;
                track.normalizedOrchestra = SearchTermBuilder.stripAccents(track.orchestra);
                track.searchTerm = SearchTermBuilder.buildSearchTerm(track.title, track.orchestra);
                // track.status = Track.Status.Valid;
                added.append(track.title + "\n");
                mTrackTableModel.addTrack(track);
            }
            JOptionPane.showMessageDialog(null, added);
            this.dispose();
        }
        else if (e.getActionCommand().equals("Cancel"))
        {
            this.dispose();
        }
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
