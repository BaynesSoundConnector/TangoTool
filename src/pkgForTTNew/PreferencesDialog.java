package pkgForTTNew;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
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

public class PreferencesDialog extends JDialog implements ActionListener, DocumentListener
{
    private static final long serialVersionUID = 1L;
    public boolean bChanged = false;
    JTextField tTitle = new JTextField();
    JTextField tOrchestra = new JTextField();
    JLabel tTime = new JLabel();
    JComboBox<Style> cbStyle = new JComboBox<Style>(Track.Style.values());
    JTextField tAlbum = new JTextField();
    private boolean bFieldChanged = false;
    private boolean bCreate = false;
    Track mTrack;

    // Update an existing track
    public PreferencesDialog()
    {
        super((JDialog) null, true);
        bCreate = false;
        JPanel mainPanel = setup();
        //mTrack = track;
        createPanel(mainPanel);
    }

    // Create a new track
    public PreferencesDialog(File file, String musicBasePath)
    {
        super((JDialog) null, true);
        bCreate = true;
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
        setBounds(100, 100, 500, 300);
        JPanel mainPanel = new JPanel(new GridLayout(8, 2));
        mainPanel.setBorder(BorderFactory.createEtchedBorder());
        this.getContentPane().add(mainPanel);
        return mainPanel;
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
        mainPanel.add(createLabel("Time"));
        mainPanel.add(new JLabel(mTrack.songTime));
        mainPanel.add(createLabel("File"));
        mainPanel.add(createLabel(mTrack.fileName));
        mainPanel.add(createLabel("Directory"));
        mainPanel.add(createLabel(mTrack.relativePath));
        mainPanel.add(createLabel(""));
        mainPanel.add(createokcancel());
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
        jp.add(cbStyle);
        return jp;
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
            if (bCreate)
            {
                mTrack.title = tTitle.getText().strip();
                mTrack.orchestra = tOrchestra.getText().strip();
                mTrack.album = tAlbum.getText().strip();
                mTrack.style = (Style) cbStyle.getSelectedItem();
                mTrack.normalizedOrchestra = SearchTermBuilder.stripAccents(mTrack.orchestra);
                mTrack.searchTerm = SearchTermBuilder.buildSearchTerm(mTrack.title, mTrack.orchestra);
                bChanged = true;
                this.dispose();
            }
            if (bFieldChanged)
            {
                mTrack.title = tTitle.getText().strip();
                mTrack.orchestra = tOrchestra.getText().strip();
                mTrack.album = tAlbum.getText().strip();
                if (mTrack.style != (Style) cbStyle.getSelectedItem())
                {
                    mTrack.style = (Style) cbStyle.getSelectedItem();
                }
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
