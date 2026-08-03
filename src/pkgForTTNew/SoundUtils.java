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
        try
        {
            stream = AudioSystem.getAudioInputStream(new File(path));
            AudioFormat baseFormat = stream.getFormat();
            // Non-PCM formats (MP3, float WAV) must be converted to a fixed target
            // format, not derived from their own bit depth: compressed encodings
            // report AudioSystem.NOT_SPECIFIED (-1) for sampleSizeInBits, which broke
            // this when it was doubled instead of fixed at 16.
            if (baseFormat.getEncoding() != AudioFormat.Encoding.PCM_SIGNED)
            {
                AudioFormat pcmFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                        baseFormat.getSampleRate(), 16, baseFormat.getChannels(),
                        baseFormat.getChannels() * 2, baseFormat.getSampleRate(), false);
                stream = AudioSystem.getAudioInputStream(pcmFormat, stream);
            }
            AudioFormat format = stream.getFormat();
            return stream.getFrameLength() / format.getFrameRate();
        }
        catch (IOException ex)
        {
            Utilities.out("SoundUtils.getLength() IOException:" + ex.getMessage());
            Utilities.out(path);
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
        try
        {
            AudioInputStream stream = AudioSystem.getAudioInputStream(file);
            AudioFormat baseFormat = stream.getFormat();
            // Same fixed-16-bit-target conversion as Utilities.playFile2() uses for
            // actual playback; see getLength() above for why doubling the reported
            // bit depth (the old approach here) broke on compressed/float formats.
            if (baseFormat.getEncoding() != AudioFormat.Encoding.PCM_SIGNED)
            {
                AudioFormat pcmFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                        baseFormat.getSampleRate(), 16, baseFormat.getChannels(),
                        baseFormat.getChannels() * 2, baseFormat.getSampleRate(), false);
                stream = AudioSystem.getAudioInputStream(pcmFormat, stream);
            }
            AudioFormat format = stream.getFormat();
            if (!AudioSystem.isLineSupported(new DataLine.Info(Clip.class, format)))
            {
                AudioFormat fallbackFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                        format.getSampleRate(), 16, format.getChannels(),
                        format.getChannels() * 2, format.getSampleRate(), false);
                stream = AudioSystem.getAudioInputStream(fallbackFormat, stream);
                format = stream.getFormat();
            }
            Clip clip = AudioSystem.getClip();
            clip.open(stream);
            float time = clip.getFrameLength() / format.getFrameRate();
            clip.close();
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