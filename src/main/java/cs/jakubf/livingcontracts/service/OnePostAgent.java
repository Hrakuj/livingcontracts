package cs.jakubf.livingcontracts.service;

import cs.jakubf.livingcontracts.model.ContractResult;
import cs.jakubf.livingcontracts.model.ContractUpdate;
import cs.jakubf.livingcontracts.model.LegalAnalysisResult;
import cs.jakubf.livingcontracts.model.LegalReference;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class OnePostAgent {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd. MM. yyyy");

    private static final Pattern CLAUSE_PATTERN = Pattern.compile(
            "([A-Z][a-zá-ž]+\\s+[0-9]+\\.|čl\\.?\\s*[0-9]+|[0-9]+\\.\\s*[A-Z])",
            Pattern.CASE_INSENSITIVE);

    public ContractUpdate generateUpdate(
            String fileName,
            String contractText,
            List<LegalReference> references,
            List<LegalAnalysisResult> analysisResults,
            String counterpartyName,
            String counterpartyIco) {

        // Najdeme klauzuli, která obsahuje odkaz na změněný předpis
        String clauseOriginal = findAffectedClause(contractText, references, analysisResults);
        if (clauseOriginal == null) {
            clauseOriginal = "V souladu s platnými právními předpisy, zejména zákonem č. 121/2000 Sb.";
        }

        // Generujeme nové znění
        String clauseProposed = generateProposedClause(clauseOriginal);

        String coverLetter = generateCoverLetter(
                counterpartyName,
                clauseOriginal,
                clauseProposed,
                analysisResults);

        boolean readyForDispatch = isReadyForDispatch(analysisResults);

        return new ContractUpdate(
                fileName,
                clauseOriginal,
                clauseProposed,
                coverLetter,
                counterpartyName,
                counterpartyIco,
                readyForDispatch);
    }

    private String findAffectedClause(
            String contractText,
            List<LegalReference> references,
            List<LegalAnalysisResult> analysisResults) {

        String bestMatch = null;
        int bestScore = 0;

        for (LegalAnalysisResult result : analysisResults) {
            if (!result.hasChanged())
                continue;
            LegalReference ref = result.reference();
            if (ref.relevanceScore() > bestScore) {
                bestScore = ref.relevanceScore();
                bestMatch = ref.context();
            }
        }

        if (bestMatch != null && bestMatch.length() > 20) {
            return bestMatch;
        }

        Matcher m = CLAUSE_PATTERN.matcher(contractText);
        if (m.find()) {
            int start = m.start();
            int end = Math.min(contractText.length(), start + 200);
            return contractText.substring(start, end).trim();
        }

        return "Klauzule týkající se právních předpisů (nutno identifikovat)";
    }

    private String generateProposedClause(String original) {
        if (original.contains("zákon č. 121/2000")) {
            return original.replace(
                    "zákonem č. 121/2000 Sb.",
                    "zákonem č. 121/2000 Sb., ve znění pozdějších předpisů");
        }
        if (original.contains("89/2012")) {
            return original + " (ve znění účinném ke dni podpisu tohoto dodatku)";
        }
        return original + " (aktualizováno dle platné právní úpravy)";
    }

    private String generateCoverLetter(
            String counterparty,
            String original,
            String proposed,
            List<LegalAnalysisResult> results) {

        StringBuilder letter = new StringBuilder();
        letter.append("Vážená společnosti ").append(counterparty).append(",\n\n");

        letter.append("dovolujeme si Vás kontaktovat v souvislosti s novelizací právních předpisů,\n");
        letter.append("které jsou relevantní pro naši smluvní spolupráci.\n\n");

        letter.append("Na základě provedené analýzy jsme identifikovali následující změny:\n");

        for (LegalAnalysisResult result : results) {
            if (result.hasChanged()) {
                letter.append("  • ").append(result.reference().lawName()).append("\n");
                letter.append("    - Změna: ").append(result.changeDescription()).append("\n");
                letter.append("    - Dopad: ").append(result.impactDescription()).append("\n");
            }
        }

        letter.append("\nV návaznosti na tyto změny navrhujeme následující úpravu smluvního ustanovení:\n\n");
        letter.append("PŮVODNÍ ZNĚNÍ:\n");
        letter.append(original).append("\n\n");
        letter.append("NAVRHOVANÉ ZNĚNÍ:\n");
        letter.append(proposed).append("\n\n");

        letter.append("Věříme, že s návrhem souhlasíte. V opačném případě jsme připraveni k jednání.\n\n");
        letter.append("S pozdravem,\n");
        letter.append("Atlas Group a.s.");

        return letter.toString();
    }

    private boolean isReadyForDispatch(List<LegalAnalysisResult> results) {
        // Pokud existuje neúčinný předpis, čekáme na revizi
        for (LegalAnalysisResult result : results) {
            if (!result.isEffective()) {
                return false; // vyžaduje lidskou revizi
            }
        }
        return true;
    }

    public String buildSummary(List<ContractResult> results) {
        StringBuilder sb = new StringBuilder();

        List<ContractResult> successful = results.stream()
                .filter(ContractResult::isSuccess)
                .collect(Collectors.toList());

        List<ContractResult> needsUpdate = successful.stream()
                .filter(ContractResult::needsUpdate)
                .collect(Collectors.toList());

        List<ContractResult> failed = results.stream()
                .filter(r -> !r.isSuccess())
                .collect(Collectors.toList());

        sb.append("PŘEHLED ANALÝZY\n");
        sb.append("═".repeat(70)).append("\n");
        sb.append("   Celkem smluv:        ").append(results.size()).append("\n");
        sb.append("   Úspěšně zpracováno:  ").append(successful.size()).append("\n");
        sb.append("   Vyžadují aktualizaci: ").append(needsUpdate.size()).append("\n");
        sb.append("   S chybou:            ").append(failed.size()).append("\n");
        sb.append("\n");

        // Detailní výpis smluv vyžadujících aktualizaci
        if (!needsUpdate.isEmpty()) {
            sb.append("═".repeat(70)).append("\n");
            sb.append("SMLOUVY VYŽADUJÍCÍ AKTUALIZACI\n");
            sb.append("═".repeat(70)).append("\n");

            for (ContractResult result : needsUpdate) {
                sb.append(result.update().toString());
                sb.append("\n");
            }
        }

        // Smlouvy bez změn
        long noChange = successful.stream()
                .filter(r -> !r.needsUpdate())
                .count();
        if (noChange > 0) {
            sb.append("═".repeat(70)).append("\n");
            sb.append(" SMLOUVY BEZ POTŘEBY ZMĚNY\n");
            sb.append("═".repeat(70)).append("\n");
            for (ContractResult result : successful.stream()
                    .filter(r -> !r.needsUpdate())
                    .collect(Collectors.toList())) {
                sb.append(result.fileName()).append("\n");
            }
            sb.append("\n");
        }

        // Chyby
        if (!failed.isEmpty()) {
            sb.append("═".repeat(70)).append("\n");
            sb.append("CHYBY\n");
            sb.append("═".repeat(70)).append("\n");
            for (ContractResult result : failed) {
                sb.append(result.fileName())
                        .append(" – ").append(result.error()).append("\n");
            }
            sb.append("\n");
        }

        // Závěr
        sb.append("═".repeat(70)).append("\n");
        sb.append("Generováno: ").append(LocalDate.now().format(FMT)).append("\n");
        sb.append("AI režim: MOCK (pro demonstraci konceptu)\n");
        sb.append("═".repeat(70)).append("\n");

        return sb.toString();
    }
}
