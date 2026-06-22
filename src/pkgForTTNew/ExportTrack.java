package pkgForTTNew;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class ExportTrack
{
    public static boolean exportTrack(Track track, String musicBasePath, String exportDirectory)
    {
        String sourcePath = musicBasePath + "\\" + track.relativePath + "\\" + track.fileName;
        File sourceFile = new File(sourcePath);
        if (!sourceFile.exists())
        {
            Utilities.msg("Source file not found:\n" + sourcePath);
            return false;
        }

        File destDir = new File(exportDirectory);
        if (!destDir.exists())
            destDir.mkdirs();

        String newFileName = sanitizeFileName(track.title + " - " + track.orchestra) + track.suffix;
        File destFile = new File(exportDirectory + File.separator + newFileName);

        if (track.fileType == Track.Type.WAV)
            return exportWav(sourceFile, destFile, track);

        try
        {
            Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return true;
        }
        catch (IOException e)
        {
            Utilities.msg("Export failed: " + e.getMessage());
            return false;
        }
    }

    private static boolean exportWav(File sourceFile, File destFile, Track track)
    {
        try
        {
            byte[] src = Files.readAllBytes(sourceFile.toPath());

            if (src.length < 12
                    || src[0] != 'R' || src[1] != 'I' || src[2] != 'F' || src[3] != 'F'
                    || src[8] != 'W' || src[9] != 'A' || src[10] != 'V' || src[11] != 'E')
            {
                Utilities.msg("Not a valid WAV file: " + sourceFile.getName());
                return false;
            }

            // Copy all chunks except any existing LIST chunk
            ByteArrayOutputStream chunks = new ByteArrayOutputStream();
            int offset = 12;
            while (offset + 8 <= src.length)
            {
                String chunkId = new String(src, offset, 4, java.nio.charset.StandardCharsets.US_ASCII);
                int chunkSize = readInt32LE(src, offset + 4);
                if (chunkSize < 0)
                    break;
                int step = 8 + chunkSize + (chunkSize % 2);
                if (!chunkId.equals("LIST"))
                    chunks.write(src, offset, Math.min(step, src.length - offset));
                offset += step;
            }

            byte[] listChunk = buildListInfoChunk(track);
            int riffDataSize = 4 + chunks.size() + listChunk.length;

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(new byte[]{'R', 'I', 'F', 'F'});
            out.write(int32ToLE(riffDataSize));
            out.write(new byte[]{'W', 'A', 'V', 'E'});
            out.write(chunks.toByteArray());
            out.write(listChunk);

            Files.write(destFile.toPath(), out.toByteArray());
            return true;
        }
        catch (IOException e)
        {
            Utilities.msg("WAV export failed: " + e.getMessage());
            return false;
        }
    }

    private static byte[] buildListInfoChunk(Track track)
    {
        ByteArrayOutputStream info = new ByteArrayOutputStream();
        writeInfoSubChunk(info, "INAM", track.title);
        writeInfoSubChunk(info, "IART", track.orchestra);
        if (track.style != null)
            writeInfoSubChunk(info, "IGNR", track.style.toString());
        if (track.year != null && !track.year.isEmpty())
            writeInfoSubChunk(info, "ICRD", track.year);
        if (track.comment != null && !track.comment.isEmpty())
            writeInfoSubChunk(info, "ICMT", track.comment);

        byte[] infoBytes = info.toByteArray();
        // LIST size = 4 bytes for "INFO" + all sub-chunk bytes
        int listSize = 4 + infoBytes.length;

        ByteArrayOutputStream list = new ByteArrayOutputStream();
        try
        {
            list.write(new byte[]{'L', 'I', 'S', 'T'});
            list.write(int32ToLE(listSize));
            list.write(new byte[]{'I', 'N', 'F', 'O'});
            list.write(infoBytes);
        }
        catch (IOException e) {} // ByteArrayOutputStream never throws
        return list.toByteArray();
    }

    private static void writeInfoSubChunk(ByteArrayOutputStream out, String code, String value)
    {
        if (value == null || value.isEmpty())
            return;
        try
        {
            byte[] strBytes = value.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            int dataSize = strBytes.length + 1; // +1 for null terminator
            out.write(code.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
            out.write(int32ToLE(dataSize));
            out.write(strBytes);
            out.write(0); // null terminator
            if (dataSize % 2 != 0)
                out.write(0); // pad to even boundary
        }
        catch (IOException e) {} // ByteArrayOutputStream never throws
    }

    private static int readInt32LE(byte[] data, int offset)
    {
        return (data[offset] & 0xFF)
             | ((data[offset + 1] & 0xFF) << 8)
             | ((data[offset + 2] & 0xFF) << 16)
             | ((data[offset + 3] & 0xFF) << 24);
    }

    private static byte[] int32ToLE(int value)
    {
        return new byte[]{
            (byte) (value & 0xFF),
            (byte) ((value >> 8) & 0xFF),
            (byte) ((value >> 16) & 0xFF),
            (byte) ((value >> 24) & 0xFF)
        };
    }

    private static String sanitizeFileName(String name)
    {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }
}
