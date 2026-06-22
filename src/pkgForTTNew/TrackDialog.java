package pkgForTTNew;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;

public class TrackDialog extends JDialog
{
	public TrackDialog(JFrame parent)
	{
		super(parent, "hubba hybba", true);
		JFrame frame = new JFrame();
		frame.setLayout(new BorderLayout());
		frame.add(new JButton("hello"), BorderLayout.CENTER);
		frame.setPreferredSize(new Dimension(400,400));
		this.getContentPane().add(frame);
		//frame.pack();
		//frame.setVisible(true);
		//this.setModal(true);
	}
}
