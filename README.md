#LIVING CONTRACTS
Nástroj na hledání změn v legislativě týkající se specifické smlouvy a potřebných změn.

(ContractLivingAI.java): vstupní bod
Kontroluje existenci vstupních dat (pokud chybí, automaticky vygeneruje sadu ukázkových smluv). 
Následně načte všechny textové soubory a zpracuje je. Na závěr sestaví a uloží výstup.  

(DatifyExtractor.java): Analyzuje text smlouvy a vyhledává v ní právní odkazy, jako jsou čísla zákonů a podobně.
Spolu s odkazem ukládá i okolní text (kontext - nemusí být v mém porvedení 100%) a na základě klíčových slov počítá relevanci dané části.
Proč tam je: Reprezentuje DATIFY. Pro účely tohoto PoC dema je AI extrakce simulována pomocí regulárních výrazů. V produkční verzi by tu byl wrapper pro API.

(CodexisLegalReasoning.java): Přebírá extrahované odkazy z předchozí fáze a porovnává je s databází legislativy, zda nedošlo ke změně co nás ovlivní.
Proč tam je: Reprezentuje CODEXIS a dodává reasoning. Je to zsae v aktualníá verzi bez API calls.

(OnePostAgent.java): Pokud se detekuje legislativní změna vyžadující zásah, vezme původní část smlouvy a vygeneruje její nové, legislativně aktuální znění.
Následně sestaví dopis pro protistranu a rozhodne, zda je návrh připraven k automatickému odeslání, nebo vyžaduje lidskou kontrolu.
Proč tam je: Reprezentuje ONEPOST. Systém u uživatele nekončí pouhou notifikací o problému, ale proaktivně předkládá hotové řešení a připravuje kroky pro formální komunikaci.  

(Složka model): Obsahuje struktury pro uchovávání dat.
Proč tam je: protože mě to tak naučili ve škole dělat.

Při vývoji byly použiti AI asistenti pro kontrolu a zrychlení práce.
