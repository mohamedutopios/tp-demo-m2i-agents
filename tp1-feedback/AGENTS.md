# Projet tp1-feedback

Calcul de facture en Java 21 (Maven). Classe à implémenter :
`src/main/java/fr/formation/facturation/CalculFacture.java`.

## Commandes
- Compiler : `mvn -B -ntp compile`
- Tests : `mvn -B -ntp test`

## Conventions
- Montants : `java.math.BigDecimal`, jamais `double` ni `float`.
- Les records `LigneFacture` et `Facture` ne doivent pas être modifiés.
- Pas de dépendance supplémentaire.
