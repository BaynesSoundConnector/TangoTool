package pkgForTTNew;

public record LufsResult(
        double integratedLufs,
        double shortTermMax,
        double momentaryMax,
        double samplePeakDbfs) {

    private static String fmtLufs(double v) {
        if (Double.isInfinite(v) || Double.isNaN(v)) return "-∞";
        return String.format("%.1f LUFS", v);
    }

    private static String fmtDbfs(double v) {
        if (Double.isInfinite(v) || Double.isNaN(v)) return "-∞";
        return String.format("%.1f dBFS", v);
    }

    public String integratedLufsString()  { return fmtLufs(integratedLufs); }
    public String shortTermMaxString()     { return fmtLufs(shortTermMax); }
    public String momentaryMaxString()     { return fmtLufs(momentaryMax); }
    public String samplePeakDbfsString()   { return fmtDbfs(samplePeakDbfs); }
}
