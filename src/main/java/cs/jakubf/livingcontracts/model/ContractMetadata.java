package cs.jakubf.livingcontracts.model;

public class ContractMetadata {

    private final String fullText;

    public ContractMetadata(String fullText) {
        this.fullText = fullText;
    }

    public String fullText() {
        return fullText;
    }
}
