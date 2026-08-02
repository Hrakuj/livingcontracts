package cs.jakubf.livingcontracts.service;

import cs.jakubf.livingcontracts.model.LegalReference;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DatifyExtractor {

    // Regex pro nalezení právních odkazů, I fucking love rexegg
    private static final Pattern LEGAL_REF_PATTERN = Pattern.compile(
            "(§\\s*\\d+\\s*(?:zákona|zák.|z.)?\\s*č\\.?\\s*\\d+\\s*/\\s*\\d+\\s*Sb\\.?)|" +
                    "(zákon(?:a)?\\s*č\\.?\\s*\\d+\\s*/\\s*\\d+\\s*Sb\\.?)|" +
                    "(nařízení\\s*(?:vlády|EU)?\\s*č\\.?\\s*\\d+\\s*/\\s*\\d+\\s*Sb\\.?)|" +
                    "(GDPR)|" +
                    "(Občanský zákoník)|" +
                    "(občanského zákoníku)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    public List<LegalReference> extract(String fileName, String contractText) {
        List<LegalReference> references = new ArrayList<>();
        Matcher matcher = LEGAL_REF_PATTERN.matcher(contractText);

        // Simulace AI extrakce
        while (matcher.find()) {
            String citation = matcher.group();
            String context = getContext(contractText, matcher.start(), matcher.end());
            String lawNumber = extractLawNumber(citation);
            String lawName = extractLawName(citation);
            int relevance = calculateRelevance(context);

            references.add(new LegalReference(
                    lawName,
                    lawNumber,
                    citation,
                    context,
                    relevance));
        }

        // Pokud nebyly nalezeny žádné reference, přidáme ukázkovou
        if (references.isEmpty()) {
            references.add(new LegalReference(
                    "zákon č. 121/2000 Sb. (autorský zákon)",
                    "121/2000",
                    "zákon č. 121/2000 Sb.",
                    "Licenční poplatky se řídí zákonem č. 121/2000 Sb.",
                    75));
        }

        return references;
    }

    private String getContext(String text, int start, int end) {
        int contextStart = Math.max(0, start - 80);
        int contextEnd = Math.min(text.length(), end + 80);
        return text.substring(contextStart, contextEnd).trim();
    }

    private String extractLawNumber(String citation) {
        Pattern p = Pattern.compile("(\\d+\\s*/\\s*\\d+)");
        Matcher m = p.matcher(citation);
        if (m.find()) {
            return m.group(1).replaceAll("\\s+", "");
        }
        return "NEZNÁMO";
    }

    private String extractLawName(String citation) {
        if (citation.toLowerCase().contains("občanský") || citation.toLowerCase().contains("občanského")) {
            return "zákon č. 89/2012 Sb. (občanský zákoník)";
        }
        if (citation.toLowerCase().contains("autorský")) {
            return "zákon č. 121/2000 Sb. (autorský zákon)";
        }
        if (citation.toLowerCase().contains("gdpr")) {
            return "nařízení GDPR (2016/679)";
        }
        if (citation.toLowerCase().contains("nařízení")) {
            return "nařízení vlády č. " + extractLawNumber(citation);
        }
        // Pokusíme se najít název podle čísla
        String num = extractLawNumber(citation);
        return switch (num) {
            case "89/2012" -> "zákon č. 89/2012 Sb. (občanský zákoník)";
            case "121/2000" -> "zákon č. 121/2000 Sb. (autorský zákon)";
            case "262/2006" -> "zákon č. 262/2006 Sb. (zákoník práce)";
            default -> "zákon č. " + num + " Sb.";
        };
    }

    private int calculateRelevance(String context) {
        // Vyšší relevance, pokud je odkaz v důležité části smlouvy
        String lower = context.toLowerCase();
        if (lower.contains("cena") || lower.contains("poplatek") ||
                lower.contains("sankce") || lower.contains("penále")) {
            return 90;
        }
        if (lower.contains("trvání") || lower.contains("výpověď") ||
                lower.contains("ukončení")) {
            return 80;
        }
        if (lower.contains("zodpovědnost") || lower.contains("odpovědnost")) {
            return 70;
        }
        return 50;
    }
}
