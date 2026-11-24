package fr.formation.facturation;

import java.math.BigDecimal;

/**
 * Ligne d'une facture. Ne pas modifier.
 *
 * @param designation    libellé de l'article
 * @param prixUnitaireHt prix unitaire hors taxes
 * @param quantite       quantité commandée
 * @param tauxTva        taux de TVA en pourcentage : 0, 5.5, 10 ou 20
 */
public record LigneFacture(String designation, BigDecimal prixUnitaireHt, int quantite, BigDecimal tauxTva) {
}
