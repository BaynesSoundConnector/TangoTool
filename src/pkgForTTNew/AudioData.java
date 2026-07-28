package pkgForTTNew;

public record AudioData(float[][] channelSamples, int sampleRate, int bitDepth) {

    public int numChannels() {
        return channelSamples.length;
    }

    public int numSamples() {
        return channelSamples.length > 0 ? channelSamples[0].length : 0;
    }

    public double durationSeconds() {
        return (double) numSamples() / sampleRate;
    }

    public String channelDescription() {
        return switch (numChannels()) {
            case 1 -> "Mono";
            case 2 -> "Stereo";
            case 3 -> "L/R/C";
            case 4 -> "Quadraphonic";
            case 5 -> "5.0 Surround";
            case 6 -> "5.1 Surround";
            default -> numChannels() + " channels";
        };
    }
}
