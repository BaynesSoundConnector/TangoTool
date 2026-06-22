package pkgForTTNew;

public class Singleton
{
    // The single instance of the class
    private static Singleton instance;
    // Example data variable
    private TrackTableModel trackTableModel;
    private TandaTreeModel tandaTreeModel;
    private PlaylistTreeModel playlistTreeModel;

    // Private constructor prevents instantiation from other classes
    private Singleton()
    {
    }

    // Public method to provide access to the instance
    public static Singleton getInstance()
    {
        if (instance == null)
        {
            instance = new Singleton();
        }
        return instance;
    }

    // Method to set data
    public void setTrackTableModel(TrackTableModel ttm)
    {
        this.trackTableModel = ttm;
    }
    public void setTandaTreeModel(TandaTreeModel ttm)
    {
        this.tandaTreeModel = ttm;
    }
    public void setPlaylistTreeModel(PlaylistTreeModel ptm)
    {
        this.playlistTreeModel = ptm;
    }

    // Method to get data
    public TrackTableModel getTrackTableModel()
    {
        return trackTableModel;
    }
    public TandaTreeModel getTandaTreeModel()
    {
        return tandaTreeModel;
    }
    public PlaylistTreeModel getPlaylistTreeModel()
    {
        return playlistTreeModel;
    }
}
