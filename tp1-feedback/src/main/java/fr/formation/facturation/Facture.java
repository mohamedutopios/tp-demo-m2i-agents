package fr.formation.facturation;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Résultat du calcul d'une facture. Ne pas modifier.
 *
 * @param totalHt     total HT après remise, frais de port inclus
 * @param remise      montant HT de la remise (positif)
 * @param fraisPortHt frais de port HT
 * @param tvaParTaux  montant de TVA par taux (clé : taux tel qu'il figure sur les lignes,
 *                    {@code new BigDecimal("20")} pour les frais de port)
 * @param totalTva    somme des montants de TVA
 * @param totalTtc    total toutes taxes comprises
 */
public record Facture(BigDecimal totalHt, BigDecimal remise, BigDecimal fraisPortHt,
                      Map<BigDecimal, BigDecimal> tvaParTaux, BigDecimal totalTva, BigDecimal totalTtc) {
}
