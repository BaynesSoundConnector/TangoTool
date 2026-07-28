package pkgForTTNew;

import java.util.ArrayList;
import java.util.List;

/**
 * ITU-R BS.1770-4 integrated/short-term/momentary loudness.
 *
 * K-weighting is two cascaded biquad filters whose coefficients are computed
 * analytically for any sample rate via the bilinear transform so the tool
 * works with 44100, 48000, 96000, etc. without a lookup table.
 */
public class LufsCalculator {

    private static final double ABSOLUTE_GATE_LUFS   = -70.0;
    private static final double RELATIVE_GATE_OFFSET  = -10.0;

    // Window sizes follow BS.1770 section 3.
    private static final double MOMENTARY_WINDOW_S  = 0.400; // 400 ms
    private static final double MOMENTARY_HOP_S     = 0.100; // 75 % overlap
    private static final double SHORT_TERM_WINDOW_S = 3.000; // 3 s
    private static final double SHORT_TERM_HOP_S    = 1.000; // 66 % overlap

    // -------------------------------------------------------------------------

    public LufsResult calculate(AudioData audio) {
        int    fs          = audio.sampleRate();
        int    numChannels = audio.numChannels();
        int    numSamples  = audio.numSamples();
        double[] gains     = channelGains(numChannels);

        // Apply K-weighting to every channel.
        double[][] kw = new double[numChannels][];
        for (int ch = 0; ch < numChannels; ch++) {
            kw[ch] = applyKWeighting(audio.channelSamples()[ch], fs);
        }

        // Sample peak across all channels.
        double peak = 0;
        for (int ch = 0; ch < numChannels; ch++) {
            for (float s : audio.channelSamples()[ch]) {
                double abs = Math.abs(s);
                if (abs > peak) peak = abs;
            }
        }
        double samplePeakDbfs = peak > 0 ? 20.0 * Math.log10(peak) : Double.NEGATIVE_INFINITY;

        int mWin  = (int) Math.round(MOMENTARY_WINDOW_S  * fs);
        int mHop  = (int) Math.round(MOMENTARY_HOP_S     * fs);
        int stWin = (int) Math.round(SHORT_TERM_WINDOW_S * fs);
        int stHop = (int) Math.round(SHORT_TERM_HOP_S    * fs);

        // Pre-compute per-block mean-squares once for momentary windows (reused
        // for both the momentary loudness list and the integrated pass).
        BlockData mBlocks  = computeBlocks(kw, gains, mWin,  mHop,  numSamples);
        BlockData stBlocks = computeBlocks(kw, gains, stWin, stHop, numSamples);

        double momentaryMax  = maxLoudness(mBlocks.loudness());
        double shortTermMax  = maxLoudness(stBlocks.loudness());
        double integrated    = integratedLoudness(mBlocks, gains, numChannels);

        return new LufsResult(integrated, shortTermMax, momentaryMax, samplePeakDbfs);
    }

    // -------------------------------------------------------------------------
    // Block analysis

    private record BlockData(List<Double> loudness, double[][] meanSquares) {}

    private BlockData computeBlocks(double[][] kw, double[] gains,
                                    int winSize, int hop, int numSamples) {
        int numChannels = kw.length;
        var loudness = new ArrayList<Double>();
        var msList   = new ArrayList<double[]>();

        for (int start = 0; start + winSize <= numSamples; start += hop) {
            double[] ms  = new double[numChannels];
            double weightedSum = 0;
            for (int ch = 0; ch < numChannels; ch++) {
                double acc = 0;
                for (int i = start; i < start + winSize; i++) {
                    acc += kw[ch][i] * kw[ch][i];
                }
                ms[ch]       = acc / winSize;
                weightedSum += gains[ch] * ms[ch];
            }
            loudness.add(weightedSum > 0
                    ? -0.691 + 10.0 * Math.log10(weightedSum)
                    : Double.NEGATIVE_INFINITY);
            msList.add(ms);
        }

        double[][] msArray = msList.toArray(new double[0][]);
        return new BlockData(loudness, msArray);
    }

    private double maxLoudness(List<Double> list) {
        return list.stream()
                   .filter(v -> !Double.isInfinite(v) && !Double.isNaN(v))
                   .mapToDouble(Double::doubleValue)
                   .max()
                   .orElse(Double.NEGATIVE_INFINITY);
    }

    // -------------------------------------------------------------------------
    // Gated integrated loudness (BS.1770-4 section 2.8)

    private double integratedLoudness(BlockData blocks, double[] gains, int numChannels) {
        List<Double>  loudness = blocks.loudness();
        double[][]    ms       = blocks.meanSquares();
        int n = loudness.size();

        // Pass 1 - absolute gate (-70 LUFS).
        var absGated = new ArrayList<Integer>();
        for (int i = 0; i < n; i++) {
            if (loudness.get(i) >= ABSOLUTE_GATE_LUFS) absGated.add(i);
        }
        if (absGated.isEmpty()) return Double.NEGATIVE_INFINITY;

        // Ungated loudness from abs-gated blocks.
        double ungated = averageLoudness(absGated, ms, gains, numChannels);
        if (Double.isInfinite(ungated)) return Double.NEGATIVE_INFINITY;

        // Pass 2 - relative gate (ungated - 10 LU).
        double relGate = ungated + RELATIVE_GATE_OFFSET;
        var relGated = new ArrayList<Integer>();
        for (int idx : absGated) {
            if (loudness.get(idx) >= relGate) relGated.add(idx);
        }
        if (relGated.isEmpty()) return Double.NEGATIVE_INFINITY;

        return averageLoudness(relGated, ms, gains, numChannels);
    }

    private double averageLoudness(List<Integer> indices, double[][] ms,
                                    double[] gains, int numChannels) {
        double[] sumMs = new double[numChannels];
        for (int idx : indices) {
            for (int ch = 0; ch < numChannels; ch++) {
                sumMs[ch] += ms[idx][ch];
            }
        }
        double weighted = 0;
        int count = indices.size();
        for (int ch = 0; ch < numChannels; ch++) {
            weighted += gains[ch] * sumMs[ch] / count;
        }
        return weighted > 0 ? -0.691 + 10.0 * Math.log10(weighted) : Double.NEGATIVE_INFINITY;
    }

    // -------------------------------------------------------------------------
    // K-weighting (two cascaded biquads, analytical coefficients)

    private double[] applyKWeighting(float[] in, int fs) {
        double[] buf = new double[in.length];
        for (int i = 0; i < in.length; i++) buf[i] = in[i];

        applyBiquad(buf, preFilterCoeffs(fs));
        applyBiquad(buf, rlbFilterCoeffs(fs));
        return buf;
    }

    private void applyBiquad(double[] x, double[] c) {
        double b0 = c[0], b1 = c[1], b2 = c[2], a1 = c[3], a2 = c[4];
        double x1 = 0, x2 = 0, y1 = 0, y2 = 0;
        for (int i = 0; i < x.length; i++) {
            double x0 = x[i];
            double y0 = b0 * x0 + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;
            x2 = x1; x1 = x0;
            y2 = y1; y1 = y0;
            x[i] = y0;
        }
    }

    // Stage 1 - high-shelf pre-filter (~1682 Hz, +4 dB).
    // Analog prototype parameters from BS.1770 annex 1.
    private double[] preFilterCoeffs(double fs) {
        double f0 = 1681.974450955533;
        double G  =    3.999843853973347;
        double Q  =    0.7071752369554196;

        double K  = Math.tan(Math.PI * f0 / fs);
        double Vh = Math.pow(10.0, G / 20.0);
        double Vb = Math.pow(Vh, 0.4996667741545416);

        double a0 =       1.0 + K / Q + K * K;
        double b0 = (Vh + Vb * K / Q + K * K) / a0;
        double b1 =  2.0 * (K * K - Vh)        / a0;
        double b2 = (Vh - Vb * K / Q + K * K)  / a0;
        double a1 =  2.0 * (K * K - 1.0)        / a0;
        double a2 =  (1.0 - K / Q + K * K)      / a0;

        return new double[]{b0, b1, b2, a1, a2};
    }

    // Stage 2 - RLB high-pass filter (~38 Hz).
    private double[] rlbFilterCoeffs(double fs) {
        double f0 = 38.13547087613982;
        double Q  =  0.5003270373238773;

        double K  = Math.tan(Math.PI * f0 / fs);
        double a0 = 1.0 + K / Q + K * K;
        double b0 =  1.0 / a0;
        double b1 = -2.0 / a0;
        double b2 =  1.0 / a0;
        double a1 =  2.0 * (K * K - 1.0)   / a0;
        double a2 =  (1.0 - K / Q + K * K) / a0;

        return new double[]{b0, b1, b2, a1, a2};
    }

    // -------------------------------------------------------------------------
    // BS.1770 channel gain table (section 3).

    private double[] channelGains(int numChannels) {
        return switch (numChannels) {
            case 1  -> new double[]{ 1.0 };
            case 2  -> new double[]{ 1.0, 1.0 };
            case 3  -> new double[]{ 1.0, 1.0, 1.0 };            // L R C
            case 4  -> new double[]{ 1.0, 1.0, 1.0, 0.0 };       // L R C LFE
            case 5  -> new double[]{ 1.0, 1.0, 1.0, 1.41, 1.41 };// L R C Ls Rs
            case 6  -> new double[]{ 1.0, 1.0, 1.0, 0.0, 1.41, 1.41 }; // 5.1
            default -> {
                double[] g = new double[numChannels];
                java.util.Arrays.fill(g, 1.0);
                yield g;
            }
        };
    }
}
