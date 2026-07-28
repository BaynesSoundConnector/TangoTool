package pkgForTTNew;

import javax.sound.sampled.*;
import java.io.*;

public class AudioFileWriter {

    /**
     * Returns a new AudioData with every sample scaled by the given dB gain.
     * Samples are NOT clamped here - call wouldClip() first if you want to warn.
     */
    public static AudioData applyGain(AudioData audio, double gainDb) {
        double gainLinear = Math.pow(10.0, gainDb / 20.0);
        int channels   = audio.numChannels();
        int numSamples = audio.numSamples();

        float[][] out = new float[channels][numSamples];
        for (int ch = 0; ch < channels; ch++) {
            float[] src = audio.channelSamples()[ch];
            for (int i = 0; i < numSamples; i++) {
                out[ch][i] = (float) (src[i] * gainLinear);
            }
        }
        return new AudioData(out, audio.sampleRate(), audio.bitDepth());
    }

    /** True if any sample would exceed +/-1.0 after the gain is applied. */
    public static boolean wouldClip(AudioData audio, double gainDb) {
        double gainLinear = Math.pow(10.0, gainDb / 20.0);
        for (float[] ch : audio.channelSamples()) {
            for (float s : ch) {
                if (Math.abs(s) * gainLinear > 1.0) return true;
            }
        }
        return false;
    }

    /**
     * Writes audio to a WAV file at the original bit depth (16 or 24-bit).
     * Samples are clamped to [-1, 1] so any mild clipping is hard-limited
     * rather than wrapping.
     */
    public static void writeWav(AudioData audio, File outputFile) throws Exception {
        int channels   = audio.numChannels();
        int numSamples = audio.numSamples();
        int sampleRate = audio.sampleRate();

        // Round-trip at native depth; fall back to 16-bit for anything unusual.
        int bitDepth = (audio.bitDepth() == 24) ? 24 : 16;
        int bytesPerSample = bitDepth / 8;
        int frameSize      = channels * bytesPerSample;

        byte[] bytes = new byte[numSamples * frameSize];

        for (int frame = 0; frame < numSamples; frame++) {
            for (int ch = 0; ch < channels; ch++) {
                float s   = Math.max(-1.0f, Math.min(1.0f, audio.channelSamples()[ch][frame]));
                int   off = frame * frameSize + ch * bytesPerSample;
                floatToBytes(s, bytes, off, bytesPerSample);
            }
        }

        AudioFormat fmt = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                sampleRate, bitDepth, channels,
                frameSize, sampleRate, false);

        try (AudioInputStream ais = new AudioInputStream(
                new ByteArrayInputStream(bytes), fmt, numSamples)) {
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, outputFile);
        }
    }

    // Float in [-1,1] -> little-endian signed PCM bytes.
    private static void floatToBytes(float s, byte[] b, int off, int bytesPerSample) {
        switch (bytesPerSample) {
            case 2 -> {
                short v = (short) (s * 32767);
                b[off]     = (byte)  (v & 0xFF);
                b[off + 1] = (byte) ((v >> 8) & 0xFF);
            }
            case 3 -> {
                int v = (int) (s * 8388607);
                b[off]     = (byte)  (v & 0xFF);
                b[off + 1] = (byte) ((v >>  8) & 0xFF);
                b[off + 2] = (byte) ((v >> 16) & 0xFF);
            }
            default -> {
                int v = (int) (s * 2147483647);
                b[off]     = (byte)  (v & 0xFF);
                b[off + 1] = (byte) ((v >>  8) & 0xFF);
                b[off + 2] = (byte) ((v >> 16) & 0xFF);
                b[off + 3] = (byte) ((v >> 24) & 0xFF);
            }
        }
    }
}
