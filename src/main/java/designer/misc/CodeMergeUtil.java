package designer.misc;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Simplest merge: for each non-blank line in newCode that
 * isn't already in baseCode, append it at the end.
 */
public class CodeMergeUtil {

    public static String merge(String baseCode, String newCode) {
        if (newCode == null || newCode.trim().isEmpty() || newCode.equals(baseCode)) {
            return baseCode;
        }

        List<String> baseLines = toLines(baseCode);
        Set<String>   baseSet   = new HashSet<>(baseLines);  // fast contains
        List<String> merged    = new ArrayList<>(baseLines);

        for (String line : toLines(newCode)) {
            if (line.trim().isEmpty()) continue;               // skip blanks
            if (baseSet.contains(line))   continue;            // already there
            merged.add(line);
        }

        // keep single blank runs
        List<String> result = new ArrayList<>();
        for (String l : merged) {
            if (l.trim().isEmpty() &&
                    !result.isEmpty() &&
                    result.get(result.size()-1).trim().isEmpty()) continue;
            result.add(l);
        }

        return String.join(System.lineSeparator(), result);
    }

    private static List<String> toLines(String code) {
        return Stream.of(code.split("\\r?\\n", -1))
                .collect(Collectors.toList());
    }
}
