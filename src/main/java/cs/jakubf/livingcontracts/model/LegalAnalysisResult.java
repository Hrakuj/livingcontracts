package cs.jakubf.livingcontracts.model;

public record LegalAnalysisResult(
        LegalReference reference,
        boolean isEffective, // je předpis stále účinný
        boolean hasChanged, // došlo k významné změně
        String changeDescription, // popis změny
        String impactDescription, // jak změna ovlivňuje smlouvu
        String suggestedAction // navrhovaná akce
) {
}
