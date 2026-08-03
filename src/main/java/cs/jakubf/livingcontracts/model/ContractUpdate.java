package cs.jakubf.livingcontracts.model;

public record ContractUpdate(
        String contractFile,
        String clauseOriginal, // původní znění klauzule
        String clauseProposed, // navrhované nové znění
        String coverLetter, // průvodní dopis pro druhou stranu
        String counterpartyName,
        String counterpartyIco,
        boolean readyForDispatch // připraveno k odeslání
) {
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("╔═══════════════════════════════════════════════════════════════╗\n");
        sb.append("║  ").append(contractFile).append("\n");
        sb.append("║   NÁVRH AKTUALIZACE SMLOUVY\n");
        sb.append("╠═══════════════════════════════════════════════════════════════╣\n");
        sb.append("║\n");
        sb.append("║  PŮVODNÍ ZNĚNÍ:\n");
        sb.append("║  ").append(clauseOriginal.replace("\n", "\n║  ")).append("\n");
        sb.append("║\n");
        sb.append("║  NAVRHOVANÉ ZNĚNÍ:\n");
        sb.append("║  ").append(clauseProposed.replace("\n", "\n║  ")).append("\n");
        sb.append("║\n");
        sb.append("║  PRŮVODNÍ DOPIS:\n");
        sb.append("║  ").append(coverLetter.replace("\n", "\n║  ")).append("\n");
        sb.append("║\n");
        sb.append("║  PŘÍJEMCE: ").append(counterpartyName).append(" (IČO ").append(counterpartyIco).append(")\n");
        sb.append("║  STAV: ").append(readyForDispatch ? "PŘIPRAVENO K ODESLÁNÍ" : "ČEKÁ NA REVIZI")
                .append("\n");
        sb.append("╚═══════════════════════════════════════════════════════════════╝\n");
        return sb.toString();
    }
}
