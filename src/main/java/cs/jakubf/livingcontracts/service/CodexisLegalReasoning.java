package cs.jakubf.livingcontracts.service;

import cs.jakubf.livingcontracts.model.ContractMetadata;
import cs.jakubf.livingcontracts.model.LegalAnalysisResult;
import cs.jakubf.livingcontracts.model.LegalReference;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CodexisLegalReasoning {

    private final LocalDate today;
    private final Map<String, LegalStatus> lawDatabase = new HashMap<>();

    // Simulovaná databáze právních předpisů.
    private record LegalStatus(
            boolean effective,
            boolean hasChanged,
            String changeDescription,
            String impact) {
    }

    public CodexisLegalReasoning(LocalDate today) {
        this.today = today;
        initializeLawDatabase();
    }

    private void initializeLawDatabase() {
        // Simulace legislativního monitoringu
        lawDatabase.put("89/2012", new LegalStatus(
                true,
                true,
                "Novela občanského zákoníku účinná od 1.1.2025 mění § 605 – počítání lhůt",
                "Může ovlivnit výpočet výpovědních lhůt ve smlouvě"));
        lawDatabase.put("121/2000", new LegalStatus(
                true,
                true,
                "Novela autorského zákona účinná od 1.6.2025 upravuje licenční poplatky",
                "Může ovlivnit licenční ujednání a výši poplatků"));
        lawDatabase.put("262/2006", new LegalStatus(
                true,
                false,
                "Žádná významná změna v posledních 12 měsících",
                "Bez dopadu na smlouvu"));
        lawDatabase.put("GDPR", new LegalStatus(
                true,
                false,
                "Aktuální verze – žádné změny",
                "Bez dopadu na smlouvu"));
        // Předpis, který už není účinný
        lawDatabase.put("40/1964", new LegalStatus(
                false,
                true,
                "Tento předpis byl zrušen k 1.1.2014 občanským zákoníkem č. 89/2012 Sb.",
                "Kritický – smlouva odkazuje na neúčinný právní předpis!"));
    }

    public List<LegalAnalysisResult> analyzeReferences(
            List<LegalReference> references,
            ContractMetadata contractInfo) {

        List<LegalAnalysisResult> results = new ArrayList<>();
        boolean hasContractData = false;

        for (LegalReference ref : references) {
            String lawNumber = ref.lawNumber();
            LegalStatus status = lawDatabase.getOrDefault(lawNumber,
                    new LegalStatus(true, false, "Žádná známá změna", "Bez dopadu"));

            String suggestedAction = status.hasChanged() ? "Doporučuje se revize příslušných ustanovení smlouvy"
                    : "Bez zásahu – předpis je aktuální";

            // Speciální případ – neúčinný předpis
            if (!status.effective()) {
                suggestedAction = "KRITICKÉ - neprodleně aktualizovat odkaz na platný předpis!";
            }

            // Zohledníme relevanci při doporučení
            if (status.hasChanged() && ref.relevanceScore() > 70) {
                suggestedAction += " (VYSOKÁ PRIORITA - dotčená klauzule je klíčová)";
            }

            results.add(new LegalAnalysisResult(
                    ref,
                    status.effective(),
                    status.hasChanged(),
                    status.changeDescription(),
                    status.impact(),
                    suggestedAction));
        }

        // Kontrola, zda smlouva obsahuje "zákon" nebo "Sb." (indikace právního
        // dokumentu)
        String contractText = contractInfo != null ? contractInfo.fullText() : "";
        if (contractText.contains("zákon") || contractText.contains("Sb.") ||
                contractText.contains("č.")) {
            hasContractData = true;
        }

        // Pokud nebyly nalezeny žádné reference, přidáme simulovanou analýzu
        if (results.isEmpty()) {
            results.add(new LegalAnalysisResult(
                    new LegalReference(
                            "zákon č. 89/2012 Sb. (občanský zákoník)",
                            "89/2012",
                            "§ 605 zákona č. 89/2012 Sb.",
                            "",
                            50),
                    true,
                    true,
                    "Novela účinná od 1.1.2025",
                    "Může ovlivnit výpočet lhůt",
                    "Doporučuje se revize smlouvy"));
        }

        return results;
    }

    // Pomocná metoda pro extrakci kontextu
    private String getContext(String text, int position) {
        int start = Math.max(0, position - 50);
        int end = Math.min(text.length(), position + 50);
        return text.substring(start, end);
    }
}
