package pkgForTTNew;

import java.text.Normalizer;

public class SearchTermBuilder
{
    // Remove all accents and produce ASCII-only output
    public static String stripAccents(String input)
    {
        if (input == null)
            return "";
        String norm = Normalizer.normalize(input, Normalizer.Form.NFD);
        // remove all diacritical marks
        return norm.replaceAll("\\p{M}", "");
    }

    // Build the searchTerm following your rules
    public static String buildSearchTerm(String title, String orchestra)
    {
        if (title == null)
            title = "";
        if (orchestra == null)
            orchestra = "";
        // 1. Concatenate title + orchestra
        String combined = title.trim() + " " + orchestra.trim();
        // 2. Lowercase
        combined = combined.toLowerCase();
        // 3. Remove accents → ASCII only
        combined = stripAccents(combined);
        return combined;
    }

    // Optional helper to apply to a Track object
    public static void updateTrackSearchTerm(Track t)
    {
        t.searchTerm = buildSearchTerm(t.title, t.orchestra);
    }
}
