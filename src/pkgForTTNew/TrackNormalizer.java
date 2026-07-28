package pkgForTTNew;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Backs up an original audio file into an "Original Files" subdirectory next
 * to it, then LUFS-normalizes it toward a target loudness and writes the
 * result back as WAV (the only format this project can currently encode -
 * FLAC/MP3 input is decoded but always written out as WAV).
 */
public class TrackNormalizer
{
    public static final String ORIGINAL_FILES_DIR = "Original Files";

    public record Result(File finalFile, boolean clipped, double measuredLufs, double appliedGainDb) {}

    public static Result normalize(File sourceFile, double targetLufs) throws Exception
    {
        backupOriginal(sourceFile);

        AudioData audio = AudioFileReader.read(sourceFile);
        LufsResult lufs = new LufsCalculator().calculate(audio);
        double integrated = lufs.integratedLufs();
        double gainDb = (Double.isInfinite(integrated) || Double.isNaN(integrated))
                ? 0.0
                : targetLufs - integrated;

        boolean clipped = AudioFileWriter.wouldClip(audio, gainDb);
        AudioData gained = AudioFileWriter.applyGain(audio, gainDb);

        File outFile = wavTargetFor(sourceFile);
        AudioFileWriter.writeWav(gained, outFile);

        if (!outFile.equals(sourceFile) && sourceFile.exists())
            sourceFile.delete();

        return new Result(outFile, clipped, integrated, gainDb);
    }

    private static void backupOriginal(File sourceFile) throws IOException
    {
        File backupDir = new File(sourceFile.getParentFile(), ORIGINAL_FILES_DIR);
        if (!backupDir.exists())
            backupDir.mkdirs();
        File backupFile = new File(backupDir, sourceFile.getName());
        if (!backupFile.exists())
            Files.copy(sourceFile.toPath(), backupFile.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
    }

    private static File wavTargetFor(File sourceFile)
    {
        String name = sourceFile.getName();
        int dot = name.lastIndexOf('.');
        String ext = dot == -1 ? "" : name.substring(dot + 1);
        if (ext.equalsIgnoreCase("wav"))
            return sourceFile;
        String base = dot == -1 ? name : name.substring(0, dot);
        return new File(sourceFile.getParentFile(), base + ".wav");
    }
}
