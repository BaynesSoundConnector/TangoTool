package pkgForTTNew;


import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public class SoundUtils implements LineListener
{
    public static float getLength(String path)
    {
        // FLAC (and some other formats) expose duration via file format properties
        try
        {
            AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(new File(path));
            Map<String, Object> props = fileFormat.properties();
            if (props.containsKey("duration"))
                return ((Long) props.get("duration")) / 1_000_000f;
        }
        catch (Exception ex) { /* fall through to frame-count approach */ }

        AudioInputStream stream = null;
        AudioFormat aFormat = null;
        long frameLength = 0;
        long frameSize = 0;
        try
        {
            stream = AudioSystem.getAudioInputStream(new File(path));
            AudioFormat format = stream.getFormat();
            Encoding encoding = format.getEncoding();
            Map<String, Object> properties = format.properties();
            // Java 1.6 doesn't support AudioFormat.Encoding.PCM_FLOAT, so
            // certain .wav files won't work.
            // Need to upgrade to Java 1.7
            if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED)
            {
                format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, format.getSampleRate(),
                        format.getSampleSizeInBits() * 2, format.getChannels(), format.getFrameSize() * 2,
                        format.getFrameRate(), true); // big endian
                stream = AudioSystem.getAudioInputStream(format, stream);
            }
            aFormat = stream.getFormat();
            frameLength = stream.getFrameLength();
            frameSize = format.getFrameSize();
            return frameLength / aFormat.getFrameRate();
        }
        catch (IOException ex)
        {
            Utilities.out("SoundUtils.getLength() IOException:" + ex.getMessage());
            Utilities.out(path);
            Utilities.out("format=" + aFormat.toString() + ", frame length=" + frameLength + ", frameSize=" + frameSize);
        }
        catch (UnsupportedAudioFileException ex)
        {
            Utilities.out("SoundUtils.getLength() UnsupportedAudioFileException:" + ex.getMessage());
            Utilities.out(path);
        }
        return -1;
    }

    public static String formatSecTohhmmss(int seconds)
    {
        long HH = seconds / 3600;
        long MM = (seconds % 3600) / 60;
        long SS = seconds % 60;
        String time;
        if (HH == 0)
            time = String.format("%2d:%02d", MM, SS);
        else
            time = String.format("%2d:%02d:%02d", HH, MM, SS);
        return time;
    }

    public static String formatIntoHHMMSS(int secsIn)
    {
        int hours = secsIn / 3600, remainder = secsIn % 3600, minutes = remainder / 60, seconds = remainder % 60;
        return ((hours < 10 ? "0" : "") + hours + ":" + (minutes < 10 ? "0" : "") + minutes + ":"
                + (seconds < 10 ? "0" : "") + seconds);
    }

    public static String formatIntoMMSS(int secsIn)
    {
        int hours = secsIn / 3600, remainder = secsIn % 3600, minutes = remainder / 60, seconds = remainder % 60;
        return ((minutes < 10 ? "0" : "") + minutes + ":" + (seconds < 10 ? "0" : "") + seconds);
    }

    public class RSLT
    {
        boolean bValid;
        String message;
    }

    /**
     * 
     * @param file The music file to be evaluated
     * @return The file time in seconds or 0 if the file can't be read.
     */
    public static float musicFileValid(File file)
    {
        // RSLT rslt = new RSLT();
        try
        {
            AudioInputStream stream;
            stream = AudioSystem.getAudioInputStream(file);
            AudioFormat format = stream.getFormat();
            AudioFileFormat aff = new AudioFileFormat(AudioFileFormat.Type.WAVE, format, format.getFrameSize());
            Map<String, Object> properties = aff.properties();
            if (properties.size() > 0)
                Utilities.out("SoundUtils.musicFileValid() properties are present:" + file.getPath());
            if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED)
            {
                format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, format.getSampleRate(),
                        format.getSampleSizeInBits() * 2, format.getChannels(), format.getFrameSize() * 2,
                        format.getFrameRate(), true); // big endian
                stream = AudioSystem.getAudioInputStream(format, stream);
            }
            DataLine.Info info = new DataLine.Info(Clip.class, stream.getFormat(),
                    ((int) stream.getFrameLength() * format.getFrameSize()));
            Clip clip = (Clip) AudioSystem.getLine(info);
            // clip.addLineListener();
            clip.open(stream);
            clip.start();
            // wait(100);
            clip.stop();
            clip.close();
            float time = clip.getBufferSize() / (clip.getFormat().getFrameSize() * clip.getFormat().getFrameRate());
            // rslt.bValid = true;
            return time;
        }
        catch (LineUnavailableException ex)
        {
            // rslt.message = "LineUnavailableException:" + ex.getMessage();
        }
        catch (IOException ex)
        {
            // rslt.message = "IOException:" + ex.getMessage();
        }
        catch (UnsupportedAudioFileException ex)
        {
            //Utilities.out("SoundUtils.musicFileValid() UnsupportedAudioFileException:"+file.getName()); 
            // ex.getMessage();
        }
        catch (IllegalArgumentException ex)
        {
            //Utilities.out("SoundUtils.musicFileValid() IllegalArgumentException:" + ex.getMessage());
            // rslt.message = "IllegalArgumentException:" + ex.getMessage();
        }
        catch (Exception ex)
        {
            Utilities.out("SoundUtils musicFileValid() exception:" + ex.getMessage());
        }
        // rslt.bValid = false;
        return 0f;
    }

    @Override
    public void update(LineEvent event)
    {
        // TODO Auto-generated method stub
    }
}