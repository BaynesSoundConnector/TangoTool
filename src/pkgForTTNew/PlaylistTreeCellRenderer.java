package pkgForTTNew;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

public class PlaylistTreeCellRenderer extends DefaultTreeCellRenderer
{
    public PlaylistTreeCellRenderer()
    {
    }

    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf,
            int row, boolean hasFocus)
    {
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        //this.setBackgroundSelectionColor(Color.CYAN);
        //setBackgroundNonSelectionColor(new Color(255,255,204));
        if (value instanceof DefaultMutableTreeNode)
        {
            setForeground(Color.BLUE);
        }
        else if (value instanceof Playlist)
        {
            setForeground(Color.black); 
            //setBackgroundNonSelectionColor(new Color(255,255,204));
        }
        else if (value instanceof Track)
        {
            Track track = (Track)value;
            if (track.style == Track.Style.Cortina)
                setForeground(Color.MAGENTA);
            else
                setForeground(Color.black);

            //setBackgroundNonSelectionColor(Color.yellow);
        }
        
        else if (value instanceof Tanda)
        {
            Tanda tanda = (Tanda) value;
            //setBackgroundNonSelectionColor(Color.white);
            if (tanda.style == Track.Style.Tango)
                setForeground(Color.red);
            else if (tanda.style == Track.Style.Valse)
                setForeground(Color.BLUE);
            else if (tanda.style == Track.Style.Milonga)
                setForeground(new Color(0,153,0));
            else
                setForeground(Color.ORANGE);

        }
        // setForeground(Color.red);
        // setBackground(Color.green);
        // this.setBackgroundNonSelectionColor(Color.pink);
        // this.setBackgroundSelectionColor(Color.CYAN);
        return this;
    }
}
