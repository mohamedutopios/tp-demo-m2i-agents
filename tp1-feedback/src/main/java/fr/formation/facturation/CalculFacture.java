package fr.formation.facturation;

import java.util.List;

/**
 * Calcul d'une facture de vente en ligne.
 *
 * <p>Seule la méthode {@link #calculer(List, String, String)} est à implémenter.
 * Les records {@link LigneFacture} et {@link Facture} ne doivent pas être modifiés.</p>
 */
public final class CalculFacture {

    private CalculFacture() {
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
        throw new UnsupportedOperationException("À implémenter");
    }
}
