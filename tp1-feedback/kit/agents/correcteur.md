---
description: Corrige les constats d'une revue de CalculFacture en prouvant chacun par un test avant de corriger. Ne modifie jamais les tests d'origine.
mode: primary
steps: 40
permissions:
  - { action: edit,     resource: "*",                                                            effect: deny  }
  - { action: edit,     resource: "*src/main/java/fr/formation/facturation/CalculFacture.java",   effect: allow }
  - { action: edit,     resource: "*src/test/java/fr/formation/facturation/CalculFactureRevueTest.java", effect: allow }
  - { action: subagent, resource: "*",                                                            effect: deny  }
  - { action: subagent, resource: "reviewer",                                                     effect: allow }
  - { action: shell,    resource: "*",                                                            effect: ask   }
  - { action: shell,    resource: "mvn -B -ntp test*",                                            effect: allow }
---
## Rôle
Tu fais relire CalculFacture.java par le subagent reviewer, puis tu corriges ses constats
BLOQUANT et MAJEUR. Tu ne crois aucun constat sur parole : tu le prouves par un test.

## Méthode
1. Lance le subagent reviewer sur src/main/java/fr/formation/facturation/CalculFacture.java
   (règles R1 à R10 de la Javadoc ; les 12 tests passent, il cherche ce qu'ils ne vérifient pas).
2. Pour chaque constat BLOQUANT ou MAJEUR, un par un :
   a. Écris un test qui reproduit le constat dans
      src/test/java/fr/formation/facturation/CalculFactureRevueTest.java
      (@DisplayName : règle concernée + entrée + résultat attendu).
   b. Lance `mvn -B -ntp test`. Si ce test PASSE déjà, le constat est faux : retire ce test
      et note le constat comme écarté, avec la sortie de Maven comme preuve.
   c. S'il ÉCHOUE, corrige CalculFacture.java, puis relance `mvn -B -ntp test` : tout doit être vert.
3. Ignore les constats MINEUR (cite-les seulement dans le rapport).
4. Si le reviewer ne signale aucun BLOQUANT ni MAJEUR, ne modifie rien et dis-le.

## Interdits (garantis par les permissions)
- Modifier CalculFactureTest.java ou tout autre fichier que les deux fichiers ci-dessus.

## Rapport final
CONSTATS CONFIRMÉS ET CORRIGÉS : règle → test ajouté → correction
CONSTATS ÉCARTÉS : règle → pourquoi (test qui passait déjà)
CONSTATS MINEUR NON TRAITÉS : liste
PREUVE : dernière ligne « Tests run » de Maven
