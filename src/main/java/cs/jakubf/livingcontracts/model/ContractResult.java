package cs.jakubf.livingcontracts.model;

import java.util.List;

/** Výsledek zpracování jedné smlouvy. */
public record ContractResult(
        String fileName,
        List<LegalReference> references,
        List<LegalAnalysisResult> analysisResults,
        ContractUpdate update,          // pokud je potřeba aktualizace
        String error
) {
    public boolean isSuccess() {
        return error == null;
    }

    public boolean needsUpdate() {
        return update != null;
    }
}
