package pkgForTTNew;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.SwingWorker;

public class Player implements PropertyChangeListener
{
	Track trackBeingPlayed;
	Clip clip;
	String musicBasePath;

	public Player(String musicBasePath)
	{
		this.musicBasePath = musicBasePath;
	}

	public void playATrack(Track track)
	{
		trackBeingPlayed = track;
		String waveFileName = musicBasePath + "tango\\music\\" + track.relativePath + "\\" + track.fileName;
		PlayThread thread = new PlayThread();
		thread.addPropertyChangeListener(this);
		thread.execute();
		// playFile(waveFileName);
	}

	class PlayThread extends SwingWorker<String, Object>
	{
		@Override
		public String doInBackground()
		{
			// Thread.currentThread().sleep(4000);
			String waveFileName = musicBasePath + "tango\\music\\" + trackBeingPlayed.relativePath + "\\"
					+ trackBeingPlayed.fileName;
			
			playFile(waveFileName);
			setProgress(100);
			out("Egg salad");
			return "egg salas";
		}

		@Override
		protected void done()
		{
			try
			{
				out("done");
			}
			catch (Exception ignore)
			{
			}
		}
	}

	private void playFile(String fileName)
	{
		AudioInputStream audioInputStream;
		if (clip != null) clip.stop();
		try
		{
			audioInputStream = AudioSystem.getAudioInputStream(new File(fileName).getAbsoluteFile());
			// create clip reference
			clip = AudioSystem.getClip();
			// open audioInputStream to the clip
			clip.open(audioInputStream);
			clip.loop(0);
		}
		catch (UnsupportedAudioFileException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (LineUnavailableException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void out(String in)
	{
		System.out.println();
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt)
	{
		// TODO Auto-generated method stub
	}
}
