// ContractLivingAI.java
//
// Tento PoC demonstruje proaktivní aktualizaci smluv při legislativních změnách.
//
// SPUŠTĚNÍ: java -jar livingcontracts-ai.jar [cesta_ke_smlouvam]
//
// POZNÁMKA: Demonstrační verze s mock daty
// no real AI / API links / calls

package cs.jakubf.livingcontracts;

import cs.jakubf.livingcontracts.model.ContractMetadata;
import cs.jakubf.livingcontracts.model.ContractResult;
import cs.jakubf.livingcontracts.model.ContractUpdate;
import cs.jakubf.livingcontracts.model.LegalAnalysisResult;
import cs.jakubf.livingcontracts.model.LegalReference;
import cs.jakubf.livingcontracts.service.CodexisLegalReasoning;
import cs.jakubf.livingcontracts.service.DatifyExtractor;
import cs.jakubf.livingcontracts.service.OnePostAgent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ContractLivingAI {

    private static final Path OUT_DIR = Paths.get("vystup");

    public static void main(String[] args) throws IOException, InterruptedException {
        Path contractsDir = Paths.get(args.length > 0 ? args[0] : "smlouvy");
        LocalDate today = LocalDate.now();

        if (!Files.isDirectory(contractsDir)) {
            System.out.println("Složka '" + contractsDir + "' neexistuje.");
            System.out.println("Vytvářím ukázkové smlouvy...\n");
            Files.createDirectories(contractsDir);
            createSampleContracts(contractsDir);
            System.out.println("Ukázkové smlouvy vytvořeny. Spusťte znovu.\n");
            return;
        }

        // Načtení souborů
        List<Path> files;
        try (var stream = Files.list(contractsDir)) {
            files = stream.filter(p -> p.toString().toLowerCase().endsWith(".txt"))
                    .sorted()
                    .collect(Collectors.toList());
        }

        if (files.isEmpty()) {
            System.out.println("Ve složce '" + contractsDir + "' nejsou žádné .txt soubory.");
            System.out.println("Vytvářím ukázkové smlouvy...\n");
            createSampleContracts(contractsDir);
            System.out.println("Ukázkové smlouvy vytvořeny. Spusťte znovu.\n");
            return;
        }

        System.out.println("Nalezeno " + files.size() + " smluv ke zpracování.\n");
        System.out.println();

        DatifyExtractor datify = new DatifyExtractor();
        CodexisLegalReasoning codexis = new CodexisLegalReasoning(today);
        OnePostAgent onepost = new OnePostAgent();

        // Zpracování
        List<ContractResult> results = new ArrayList<>();
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<ContractResult>> futures = new ArrayList<>();

            for (Path file : files) {
                futures.add(pool.submit(() -> processContract(file, datify, codexis, onepost)));
            }

            for (Future<ContractResult> future : futures) {
                try {
                    results.add(future.get());
                } catch (Exception e) {
                    System.err.println("Unexpected error: " + e.getMessage());
                }
            }
        }

        // Generování reportu
        String report = onepost.buildSummary(results);

        // Uložení
        Files.createDirectories(OUT_DIR);
        Path outFile = OUT_DIR.resolve("living_contracts_report_" + today + ".txt");
        Files.writeString(outFile, report, StandardCharsets.UTF_8);

        System.out.println(report);
        System.out.println(" Report uložen do: " + outFile.toAbsolutePath());
    }

    private static ContractResult processContract(
            Path file,
            DatifyExtractor datify,
            CodexisLegalReasoning codexis,
            OnePostAgent onepost) {

        String fileName = file.getFileName().toString();
        System.out.println("Zpracovávám: " + fileName);

        try {
            String text = Files.readString(file, StandardCharsets.UTF_8);

            // 1. DATIFY – extrakce právních odkazů
            System.out.println("DATIFY: Extrakce právních odkazů...");
            List<LegalReference> references = datify.extract(fileName, text);
            System.out.println("Nalezeno " + references.size() + " právních odkazů");

            // 2. CODEXIS – právní analýza
            System.out.println("CODEXIS: Právní reasoning...");
            ContractMetadata metadata = new ContractMetadata(text);
            List<LegalAnalysisResult> analysis = codexis.analyzeReferences(references, metadata);

            long changes = analysis.stream().filter(LegalAnalysisResult::hasChanged).count();
            System.out.println("Identifikováno " + changes + " změn v právních předpisech");

            // 3. ONEPOST – generování návrhu
            ContractUpdate update = null;
            boolean needsUpdate = analysis.stream().anyMatch(LegalAnalysisResult::hasChanged);

            if (needsUpdate) {
                System.out.println("ONEPOST: Generuji návrh aktualizace...");
                // Extraktor protistrany
                String counterparty = extractCounterparty(text);
                String ico = extractIco(text);
                update = onepost.generateUpdate(fileName, text, references, analysis, counterparty, ico);
                System.out.println("Návrh připraven" +
                        (update.readyForDispatch() ? "k odeslání" : "čeká na revizi"));
            } else {
                System.out.println("ONEPOST: Smlouva je aktuální – bez potřeby změny");
            }

            System.out.println("Hotovo\n");
            return new ContractResult(fileName, references, analysis, update, null);

        } catch (Exception e) {
            System.err.println("Chyba: " + e.getMessage() + "\n");
            return new ContractResult(fileName, List.of(), List.of(), null, e.getMessage());
        }
    }

    private static String extractCounterparty(String text) {
        Pattern p = Pattern
                .compile("(?:Protistrana|Dodavatel|Poskytovatel|Nájemce|Licenciát|Objednatel):\\s*([^,\\n]+)");
        Matcher m = p.matcher(text);
        if (m.find()) {
            return m.group(1).trim();
        }
        return "NEZNÁMÁ PROTISTRANA";
    }

    private static String extractIco(String text) {
        Pattern p = Pattern.compile("IČO:\\s*(\\d{8})");
        Matcher m = p.matcher(text);
        if (m.find()) {
            return m.group(1);
        }
        return "NEZNÁMO";
    }

    // UKÁZKOVÉ SMLOUVY

    private static void createSampleContracts(Path dir) throws IOException {
        // 1. IT smlouva s odkazem na autorský zákon
        String itContract = """
                SMLOUVA O POSKYTOVÁNÍ IT SLUŽEB

                Smluvní strany:
                Objednatel: Atlas Group a.s., IČO: 12345678
                Poskytovatel: IT Solutions s.r.o., IČO: 12345678

                Článek 7 – Licenční ujednání:
                Poskytovatel uděluje Objednateli nevýhradní licenci k užívání softwaru.
                Licenční poplatky se řídí zákonem č. 121/2000 Sb. (autorský zákon).

                Článek 12 – Odpovědnost:
                Odpovědnost za škodu se řídí ustanoveními zákona č. 89/2012 Sb., občanského zákoníku.

                Doba trvání: Smlouva se uzavírá na dobu neurčitou.
                Výpovědní lhůta: 3 měsíce.

                Datum uzavření: 1.1.2024
                """;
        Files.writeString(dir.resolve("IT_smlouva.txt"), itContract);

        // 2. Dodavatelská smlouva s odkazem na občanský zákoník
        String dodavatelContract = """
                DODAVATELSKÁ SMLOUVA

                Smluvní strany:
                Odběratel: Atlas Group a.s., IČO: 12345678
                Dodavatel: Dodavatelství CZ a.s., IČO: 98765432

                Článek 5 – Výpověď:
                Výpovědní lhůta činí 6 měsíců dle § 605 zákona č. 89/2012 Sb.

                Článek 10 – Sankce:
                Smluvní pokuty se řídí ustanoveními občanského zákoníku.

                Doba trvání: Smlouva se uzavírá na dobu neurčitou.
                Smlouva se automaticky prodlužuje.

                Datum uzavření: 1.2.2024
                """;
        Files.writeString(dir.resolve("Dodavatelska_smlouva.txt"), dodavatelContract);

        // 3. GDPR smlouva
        String gdprContract = """
                SMLOUVA O ZPRACOVÁNÍ OSOBNÍCH ÚDAJŮ

                Smluvní strany:
                Správce: Atlas Group a.s., IČO: 12345678
                Zpracovatel: DataPro s.r.o., IČO: 44445555

                Článek 1 – Předmět:
                Zpracovatel bude zpracovávat osobní údaje dle nařízení GDPR (2016/679).

                Článek 3 – Zabezpečení:
                Zpracovatel přijme technická a organizační opatření dle GDPR.

                Doba trvání: Smlouva se uzavírá na dobu neurčitou.
                Výpovědní lhůta: 2 měsíce.

                Datum uzavření: 1.3.2025
                """;
        Files.writeString(dir.resolve("GDPR_smlouva.txt"), gdprContract);

        // 4. Smlouva s odkazem na neúčinný předpis
        String staraContract = """
                SMLOUVA O DÍLO

                Smluvní strany:
                Objednatel: Atlas Group a.s., IČO: 12345678
                Zhotovitel: Stavby s.r.o., IČO: 66667777

                Článek 8 – Odpovědnost:
                Odpovědnost za vady se řídí zákonem č. 40/1964 Sb., občanským zákoníkem.

                Doba trvání: Smlouva se uzavírá do 31.12.2026.

                Datum uzavření: 1.1.2026
                """;
        Files.writeString(dir.resolve("Stara_smlouva.txt"), staraContract);
    }
}
