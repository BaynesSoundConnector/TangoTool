package pkgForTTNew;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class TandaDetailDialog extends JDialog implements ActionListener, DocumentListener
{
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    public boolean bChanged = false;
    Tanda mTanda;
    JTextField description;
    JTextField orchestra;
    JTextField source;
    JTextField lastChanged;
    JTextField lastPlayed;
    JButton okButton;
    JButton cancelButton;

    public TandaDetailDialog(Tanda tanda)
    {
        super((JDialog) null, true);
        this.setTitle("Tanda Information");
        mTanda = tanda;
        setBounds(100, 100, 500, 250);
        int size = 10;
        JPanel mainPanel = new JPanel(new BorderLayout());
        this.getContentPane().add(mainPanel);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(size, size, size, size));
        JPanel grid = new JPanel(new GridLayout(6, 2));
        grid.add(new JLabel("Description"));
        description = new JTextField(20);
        description.getDocument().addDocumentListener(this);
        description.setText(tanda.description);
        grid.add(description);
        grid.add(new JLabel("Orchestra"));
        orchestra = new JTextField(20);
        orchestra.getDocument().addDocumentListener(this);
        orchestra.setText(tanda.orchestra);
        grid.add(orchestra);
        grid.add(new JLabel("Source"));
        source = new JTextField(20);
        source.getDocument().addDocumentListener(this);
        source.setText(tanda.source);
        grid.add(source);
        grid.add(new JLabel("Time"));
        grid.add(new JLabel("xxx.xxx"));
        grid.add(new JLabel("Last changed"));
        grid.add(new JLabel("mm/dd/yy hh:mm"));
        grid.add(new JLabel("Last played"));
        grid.add(new JLabel("mm/dd/yy hh:mm"));
        mainPanel.add(grid, BorderLayout.NORTH);
        JPanel bottomLine = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(this);
        bottomLine.add(cancelButton);
        okButton = new JButton("OK");
        okButton.addActionListener(this);
        bottomLine.add(okButton);
        mainPanel.add(bottomLine, BorderLayout.SOUTH);
        bChanged = false;
    }


    @Override
    public void actionPerformed(ActionEvent e)
    {
        if (e.getSource() == okButton)
        {
            this.dispose();
        }
        else if (e.getSource() == cancelButton)
        {
            bChanged = false;
            this.dispose();
        }
        
    }

    @Override
    public void insertUpdate(DocumentEvent e)
    {
        bChanged = true;
        Utilities.out("insertUpdate");
    }

    @Override
    public void removeUpdate(DocumentEvent e)
    {
        bChanged = true;
        Utilities.out("removeUpdate");
    }

    @Override
    public void changedUpdate(DocumentEvent e)
    {
        bChanged = true;
        Utilities.out("changedUpdate");
    }
}
