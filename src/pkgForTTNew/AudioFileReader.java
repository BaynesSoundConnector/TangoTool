package pkgForTTNew;

import javax.sound.sampled.*;
import java.io.File;

public class AudioFileReader {

    public static AudioData read(File file) throws Exception {
        // Not AudioSystem.getAudioInputStream(file) directly -- see SoundUtils.probeProviders()
        // for why: that call shares one stream across all format providers and breaks once
        // other files have already been decoded earlier in the same JVM run, which is exactly
        // what happens measuring/normalizing many tracks in one session (e.g. a playlist LUFS
        // report).
        AudioInputStream original = SoundUtils.getAudioInputStreamRobust(file);
        AudioFormat fmt = original.getFormat();

        int sampleRate  = (int) fmt.getSampleRate();
        int channels    = fmt.getChannels();
        int originalBit = fmt.getSampleSizeInBits();

        // Normalise to signed 16/24-bit little-endian PCM so byte parsing is uniform.
        // Preserve 24-bit depth to avoid truncating mastering-quality files.
        int targetBits = switch (originalBit) {
            case 8, 16 -> 16;
            case 24    -> 24;
            default    -> 16; // float PCM, MP3, etc. -> 16-bit
        };

        boolean alreadyTarget = fmt.getEncoding() == AudioFormat.Encoding.PCM_SIGNED
                && fmt.getSampleSizeInBits() == targetBits
                && !fmt.isBigEndian();

        AudioFormat targetFmt = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                sampleRate, targetBits, channels,
                channels * (targetBits / 8),
                sampleRate, false);

        AudioInputStream pcm = alreadyTarget ? original
                : AudioSystem.getAudioInputStream(targetFmt, original);

        byte[] bytes = pcm.readAllBytes();
        pcm.close();

        int bytesPerSample = targetBits / 8;
        int frameSize      = channels * bytesPerSample;
        int numFrames      = bytes.length / frameSize;

        float[][] channelSamples = new float[channels][numFrames];

        for (int frame = 0; frame < numFrames; frame++) {
            for (int ch = 0; ch < channels; ch++) {
                int off = frame * frameSize + ch * bytesPerSample;
                channelSamples[ch][frame] = toFloat(bytes, off, bytesPerSample);
            }
        }

        return new AudioData(channelSamples, sampleRate, originalBit > 0 ? originalBit : targetBits);
    }

    // Little-endian bytes -> float in [-1, 1).
    private static float toFloat(byte[] b, int off, int bytesPerSample) {
        return switch (bytesPerSample) {
            case 2 -> {
                short s = (short) (((b[off + 1] & 0xFF) << 8) | (b[off] & 0xFF));
                yield s / 32768.0f;
            }
            case 3 -> {
                int v = ((b[off + 2] & 0xFF) << 16) | ((b[off + 1] & 0xFF) << 8) | (b[off] & 0xFF);
                if (v >= 0x800000) v -= 0x1000000; // sign-extend 24->32
                yield v / 8388608.0f;
            }
            default -> {
                int v = ((b[off + 3] & 0xFF) << 24) | ((b[off + 2] & 0xFF) << 16)
                      | ((b[off + 1] & 0xFF) << 8)  |  (b[off] & 0xFF);
                yield v / 2147483648.0f;
            }
        };
    }
}
