package fr.formation.facturation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Calcul d'une facture de vente en ligne.
 *
 * <p>Seule la méthode {@link #calculer(List, String, String)} est à implémenter.
 * Les records {@link LigneFacture} et {@link Facture} ne doivent pas être modifiés.</p>
 */
public final class CalculFacture {

    private static final Set<BigDecimal> TAUX_AUTORISES =
            Set.of(new BigDecimal("0"), new BigDecimal("5.5"), new BigDecimal("10"), new BigDecimal("20"));
    private static final Map<String, BigDecimal> REMISES =
            Map.of("BIENVENUE10", new BigDecimal("10"), "FIDELITE5", new BigDecimal("5"));
    private static final BigDecimal PLAFOND_REMISE = new BigDecimal("100.00");
    private static final BigDecimal SEUIL_FRAIS_PORT = new BigDecimal("50.00");
    private static final BigDecimal FRAIS_PORT_HT = new BigDecimal("9.90");
    private static final BigDecimal TAUX_FRAIS_PORT = new BigDecimal("20");
    private static final BigDecimal CENT = new BigDecimal("100");
    private static final BigDecimal ZERO = new BigDecimal("0.00");

    private CalculFacture() {
    }

    private static BigDecimal arrondi(BigDecimal montant) {
        return montant.setScale(2, RoundingMode.HALF_UP);
    }

    private static boolean tauxAutorise(BigDecimal taux) {
        return TAUX_AUTORISES.contains(taux);
    }

    /**
     * Calcule le détail d'une facture.
     *
     * <p>Règles de gestion :</p>
     * <ul>
     *   <li><b>R1.</b> La facture doit contenir au moins une ligne, sinon IllegalArgumentException.</li>
     *   <li><b>R2.</b> La quantité de chaque ligne doit être strictement positive, sinon IllegalArgumentException.</li>
     *   <li><b>R3.</b> Taux de TVA autorisés : 0, 5.5, 10 et 20. Tout autre taux : IllegalArgumentException.</li>
     *   <li><b>R4.</b> Montant HT d'une ligne = prixUnitaireHt × quantite, arrondi au centime
     *       (arrondi commercial : 0,005 → 0,01, soit {@code RoundingMode.HALF_UP}).</li>
     *   <li><b>R5.</b> Codes de remise :
     *       "BIENVENUE10" = 10 % du total HT des lignes ; "FIDELITE5" = 5 % du total HT des lignes ;
     *       null = pas de remise ; tout autre code : IllegalArgumentException.
     *       La remise est arrondie au centime (HALF_UP) et plafonnée à 100,00 € HT.</li>
     *   <li><b>R6.</b> La remise est répartie sur les lignes au prorata de leur montant HT, afin que la TVA
     *       soit calculée sur des montants remisés. Le dernier centime d'écart d'arrondi est affecté
     *       à la dernière ligne.</li>
     *   <li><b>R7.</b> Frais de port : 9,90 € HT, soumis à 20 % de TVA, si le total HT des lignes APRÈS remise
     *       est strictement inférieur à 50,00 €. Sinon : 0.</li>
     *   <li><b>R8.</b> TVA : calculée par taux (ventilation), sur la somme des montants HT remisés de ce taux,
     *       puis arrondie au centime (HALF_UP). Les frais de port s'ajoutent à la base du taux 20 %.
     *       tvaParTaux ne contient que les taux présents sur la facture (frais de port compris).</li>
     *   <li><b>R9.</b> Si pays est différent de "FR" : toute la TVA vaut 0 (vente export), tvaParTaux est vide.</li>
     *   <li><b>R10.</b> totalHt = lignes remisées + frais de port ; totalTva = somme de tvaParTaux ;
     *       totalTtc = totalHt + totalTva.</li>
     * </ul>
     * <p>Tous les montants renvoyés sont des BigDecimal à deux décimales (scale = 2).</p>
     *
     * @param lignes     lignes de la facture
     * @param codeRemise code de remise, ou null
     * @param pays       code pays du client ("FR", "BE"…)
     * @return le détail de la facture
     */
    public static Facture calculer(List<LigneFacture> lignes, String codeRemise, String pays) {
        if (lignes == null || lignes.isEmpty()) {                                    // R1
            throw new IllegalArgumentException("La facture doit contenir au moins une ligne");
        }
        for (LigneFacture l : lignes) {
            if (l.quantite() <= 0) {                                                 // R2
                throw new IllegalArgumentException("Quantité invalide pour " + l.designation());
            }
            if (!tauxAutorise(l.tauxTva())) {                                        // R3
                throw new IllegalArgumentException("Taux de TVA non autorisé : " + l.tauxTva());
            }
        }
        if (codeRemise != null && !REMISES.containsKey(codeRemise)) {                // R5
            throw new IllegalArgumentException("Code de remise inconnu : " + codeRemise);
        }

        List<BigDecimal> montants = new ArrayList<>();                               // R4
        for (LigneFacture l : lignes) {
            montants.add(arrondi(l.prixUnitaireHt().multiply(BigDecimal.valueOf(l.quantite()))));
        }
        BigDecimal totalLignes = montants.stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal remise = ZERO;
        if (codeRemise != null) {
            remise = arrondi(totalLignes.multiply(REMISES.get(codeRemise)).divide(CENT));
            if ("BIENVENUE10".equals(codeRemise)) {
                remise = remise.min(PLAFOND_REMISE);
            }
        }

        // R6 : répartition au prorata, dernier centime sur la dernière ligne
        List<BigDecimal> montantsRemises = new ArrayList<>();
        BigDecimal reparti = BigDecimal.ZERO;
        for (int i = 0; i < montants.size(); i++) {
            BigDecimal part;
            if (i < montants.size() - 1) {
                part = totalLignes.signum() == 0 ? BigDecimal.ZERO
                        : remise.multiply(montants.get(i)).divide(totalLignes, 2, RoundingMode.HALF_UP);
                reparti = reparti.add(part);
            } else {
                part = remise.subtract(reparti);
            }
            montantsRemises.add(montants.get(i).subtract(part));
        }
        BigDecimal totalLignesRemise = montantsRemises.stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        boolean france = "FR".equals(pays);
        BigDecimal frais = france && totalLignesRemise.compareTo(SEUIL_FRAIS_PORT) < 0 ? FRAIS_PORT_HT : ZERO; // R7

        Map<BigDecimal, BigDecimal> tvaParTaux = new LinkedHashMap<>();
        if (france) {                                                                // R9
            Map<BigDecimal, BigDecimal> bases = new LinkedHashMap<>();
            for (int i = 0; i < lignes.size(); i++) {
                bases.merge(lignes.get(i).tauxTva(), montantsRemises.get(i), BigDecimal::add);
            }
            if (frais.signum() > 0 && bases.containsKey(TAUX_FRAIS_PORT)) {
                bases.merge(TAUX_FRAIS_PORT, frais, BigDecimal::add);
            }
            bases.forEach((taux, base) -> tvaParTaux.put(taux, arrondi(base.multiply(taux).divide(CENT)))); // R8
        }

        BigDecimal totalHt = arrondi(totalLignesRemise.add(frais));                  // R10
        BigDecimal totalTva = arrondi(tvaParTaux.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
        return new Facture(totalHt, arrondi(remise), arrondi(frais), Map.copyOf(tvaParTaux),
                totalTva, arrondi(totalHt.add(totalTva)));
    }
}
