package pkgForTTNew;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Backs up an original audio file into an "original files before
 * normalization" subdirectory next to it (deliberately distinct from the
 * "Original Files"/"Original Named Files" folders already used throughout
 * the library for pre-existing master recordings, so this backup is never
 * mistaken for -- or mixed in with -- those), then LUFS-normalizes it toward
 * a target loudness and writes the result back as WAV (the only format this
 * project can currently encode - FLAC/MP3 input is decoded but always
 * written out as WAV).
 */
public class TrackNormalizer
{
    public static final String ORIGINAL_FILES_DIR = "original files before normalization";

    // finalLufs is measured from the actual written output, not derived from
    // targetLufs -- when clipped is true the hard-limiting step pulls the real
    // result slightly off target, so re-measuring is what keeps the LUFS column
    // (and any report built from it) honest about what's really on disk.
    public record Result(File finalFile, boolean clipped, double originalLufs, double finalLufs, double appliedGainDb) {}

    public static Result normalize(File sourceFile, double targetLufs) throws Exception
    {
        backupOriginal(sourceFile);

        AudioData audio = AudioFileReader.read(sourceFile);
        double integrated = measureLufs(audio);
        double gainDb = (Double.isInfinite(integrated) || Double.isNaN(integrated))
                ? 0.0
                : targetLufs - integrated;

        boolean clipped = AudioFileWriter.wouldClip(audio, gainDb);
        AudioData gained = AudioFileWriter.applyGain(audio, gainDb);

        File outFile = wavTargetFor(sourceFile);
        AudioFileWriter.writeWav(gained, outFile);

        if (!outFile.equals(sourceFile) && sourceFile.exists())
            sourceFile.delete();

        double finalLufs = measureLufs(AudioFileReader.read(outFile));

        return new Result(outFile, clipped, integrated, finalLufs, gainDb);
    }

    /** Measures a file's current integrated LUFS without modifying it. */
    public static double measureLufs(File file) throws Exception
    {
        return measureLufs(AudioFileReader.read(file));
    }

    private static double measureLufs(AudioData audio)
    {
        return new LufsCalculator().calculate(audio).integratedLufs();
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
