package designer.misc;

public class MergingUtil
{
    public static String merge(String newCode, String cachedCode)
    {
        if (cachedCode == null || cachedCode.trim().isEmpty() || cachedCode.equals(newCode)) {
            return newCode + "\n// ---- user code ----\n";
        }
        String[] parts = cachedCode.split("// ---- user code ----");
        return  parts.length > 1 ?
                newCode + "\n// ---- user code ----" + parts[1]
                :
                newCode + "\n// ---- user code ----";
    }
}
