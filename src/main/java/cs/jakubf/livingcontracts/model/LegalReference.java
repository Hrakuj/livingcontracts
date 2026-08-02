package cs.jakubf.livingcontracts.model;

public record LegalReference(
        String lawName, // název zákona
        String lawNumber, // číslo zákona
        String citation, // původní citace
        String context, // kontext – okolní text, kde byl odkaz nalezen
        int relevanceScore // 0-100, jak je odkaz pro smlouvu důležitý
) {
}
