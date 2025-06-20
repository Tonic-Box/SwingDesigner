package designer.misc;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility to merge two code snippets by inserting lines present in newCode into baseCode.
 * It performs duplicate detection (including method-suffix heuristic) and contextual insertion.
 * Empty lines are preserved from the original base snippet and collapsed to single runs.
 */
public class CodeMergeUtil {
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.8;

    public static String merge(String baseCode, String newCode) {
        if(newCode == null || newCode.isEmpty() || newCode.trim().isEmpty() || newCode.equals(baseCode)) {
            return baseCode;
        }
        System.out.println("lol");
        return merge(baseCode, newCode, DEFAULT_SIMILARITY_THRESHOLD);
    }

    public static String merge(String baseCode, String newCode, double threshold) {
        List<String> baseLines = toLines(baseCode);
        List<String> newLines = toLines(newCode);
        List<String> merged = new ArrayList<>(baseLines);

        for (int i = 0; i < newLines.size(); i++) {
            String candidate = newLines.get(i);
            // Skip blank lines; preserve only original base blanks
            if (candidate.trim().isEmpty()) {
                continue;
            }
            // Skip if exact exists
            if (merged.contains(candidate)) {
                continue;
            }
            // Skip if suffix matches (var renames)
            if (suffixExists(candidate, merged)) {
                continue;
            }
            // Skip if fuzzy-similar
            if (existsFuzzy(candidate, merged, threshold)) {
                continue;
            }

            int pos = findContextualPosition(candidate, merged, newLines, i, threshold);
            if (pos < 0) {
                merged.add(candidate);
            } else {
                merged.add(pos, candidate);
            }
        }

        // Collapse consecutive empty lines
        List<String> result = new ArrayList<>();
        for (String line : merged) {
            if (line.trim().isEmpty()) {
                if (!result.isEmpty() && result.get(result.size() - 1).trim().isEmpty()) {
                    continue;
                }
            }
            result.add(line);
        }

        return String.join(System.lineSeparator(), result);
    }

    private static List<String> toLines(String code) {
        return Stream.of(code.split("\\r?\\n", -1))
                .collect(Collectors.toList());
    }

    // Heuristic: match method call suffixes ignoring variable name
    private static boolean suffixExists(String line, List<String> list) {
        int idx = line.indexOf(".");
        if (idx < 0) return false;
        String suffix = line.substring(idx + 1).trim();
        for (String l : list) {
            int i = l.indexOf(".");
            if (i < 0) continue;
            String s2 = l.substring(i + 1).trim();
            if (normalize(suffix).equals(normalize(s2))) {
                return true;
            }
        }
        return false;
    }

    private static boolean existsFuzzy(String line, List<String> list, double threshold) {
        String n1 = normalize(line);
        for (String l : list) {
            if (similarity(n1, normalize(l)) >= threshold) {
                return true;
            }
        }
        return false;
    }

    private static int findContextualPosition(String line, List<String> base, List<String> updated,
                                              int idx, double threshold) {
        // backward neighbor context
        for (int i = idx - 1; i >= 0; i--) {
            String prev = updated.get(i).trim().isEmpty() ? null : updated.get(i);
            if (prev == null) continue;
            for (int j = 0; j < base.size(); j++) {
                if (similarity(normalize(prev), normalize(base.get(j))) >= threshold) {
                    return j + 1;
                }
            }
        }
        // forward neighbor context
        for (int i = idx + 1; i < updated.size(); i++) {
            String nxt = updated.get(i).trim().isEmpty() ? null : updated.get(i);
            if (nxt == null) continue;
            for (int j = 0; j < base.size(); j++) {
                if (similarity(normalize(nxt), normalize(base.get(j))) >= threshold) {
                    return j;
                }
            }
        }
        return -1;
    }

    private static String normalize(String s) {
        return s.toLowerCase().replaceAll("\\s+", " ")
                .replaceAll("[^a-z0-9()\\[\\] {};,\\.\\\"']", "").trim();
    }

    private static double similarity(String a, String b) {
        if (a.equals(b)) return 1.0;
        int dist = levenshtein(a, b);
        int max = Math.max(a.length(), b.length());
        return max == 0 ? 1.0 : 1.0 - ((double) dist / max);
    }

    private static int levenshtein(String s1, String s2) {
        int[] prev = new int[s2.length() + 1];
        for (int j = 0; j <= s2.length(); j++) prev[j] = j;
        for (int i = 1; i <= s1.length(); i++) {
            int[] curr = new int[s2.length() + 1];
            curr[0] = i;
            for (int j = 1; j <= s2.length(); j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            prev = curr;
        }
        return prev[s2.length()];
    }
}
