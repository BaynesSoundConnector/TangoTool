package pkgForTTNew;

import java.util.ArrayList;

public class Track
{
	public long uniqueId;
	int sequenceInAlbum;
	public String title;
	public String orchestra;
	String normalizedOrchestra;
	String searchTerm;
	int rating;
	int tempo;
	public Track.Style style;
	Track.Mood mood;
	Track.Danceability danceability;
	String songTime;
	String year;
	public String album;
	String relativePath;
	public String fileName;
	String suffix;
	String comment;
	String composer;
	String pianist;
	String bandoneonist;
	String vocalist;
	Type fileType;
	float calculatedTime;
	Double measuredLufs;
	Track.Status status;
	String Version = "1.0";
	int spoof;
	ArrayList<Long> inTandas;

	public enum Status
	{
		Valid, Invalid
	}

	public enum Type
	{
		WAV, MP3, MP4, FLAC
	}

	public enum Style
	{
		Tango, Valse, Milonga, Cortina, AltTango, WCS, Salsa, Swing, Candombe, Other, Unknown
	}

	public enum Mood
	{
		Bright, Playful, Romantic, Passionate
	}

	public enum Danceability
	{
		Excellent, Good, Fair, Poor
	}

	public enum RecordingQuality
	{
		Excellent, Good, Fair, Poor
	}


	public Track()
	{
		// TODO Auto-generated constructor stub
	}
	public String toString()
	{
	    String str = title+" ("+uniqueId+") "+songTime+" "+style;
	    return str;
		//if (this.style == Track.Style.Cortina)
			//return title+" (cortina)";
		//return title+" ("+this.uniqueId+")";
	}
	
	
}
