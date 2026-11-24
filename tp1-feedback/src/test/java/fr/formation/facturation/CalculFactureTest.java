package fr.formation.facturation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Tests de CalculFacture.calculer() — 12 tests, un ou plusieurs par règle R1 à R10.
 * Ces tests ne doivent pas être modifiés pendant le TP.
 */
class CalculFactureTest {

    private static BigDecimal bd(String valeur) {
        return new BigDecimal(valeur);
    }

    private static LigneFacture ligne(String prix, int quantite, String tva) {
        return new LigneFacture("article", bd(prix), quantite, bd(tva));
    }

    private static LigneFacture ligne(String prix) {
        return ligne(prix, 1, "20");
    }

    // R1
    @Test
    void r1_facture_vide_refusee() {
        assertThrows(IllegalArgumentException.class, () -> CalculFacture.calculer(List.of(), null, "FR"));
    }

    // R2
    @Test
    void r2_quantite_nulle_refusee() {
        assertThrows(IllegalArgumentException.class,
                () -> CalculFacture.calculer(List.of(ligne("10.00", 0, "20")), null, "FR"));
    }

    // R3
    @Test
    void r3_taux_tva_inconnu_refuse() {
        assertThrows(IllegalArgumentException.class,
                () -> CalculFacture.calculer(List.of(ligne("100.00", 1, "19.6")), null, "FR"));
    }

    // R4
    @Test
    void r4_arrondi_commercial_au_centime() {
        Facture f = CalculFacture.calculer(List.of(ligne("33.335", 3, "20")), null, "FR"); // 100.005 -> 100.01
        assertEquals(bd("100.01"), f.totalHt());
        assertEquals(bd("120.01"), f.totalTtc());
    }

    // R5
    @Test
    void r5_code_remise_inconnu_refuse() {
        assertThrows(IllegalArgumentException.class,
                () -> CalculFacture.calculer(List.of(ligne("100.00")), "PROMO50", "FR"));
    }

    @Test
    void r5_remise_bienvenue_10_pourcent() {
        Facture f = CalculFacture.calculer(List.of(ligne("100.00", 2, "20")), "BIENVENUE10", "FR");
        assertEquals(bd("20.00"), f.remise());
        assertEquals(bd("180.00"), f.totalHt());
        assertEquals(bd("36.00"), f.totalTva());
        assertEquals("216.00", f.totalTtc().toPlainString());
    }

    @Test
    void r5_remise_plafonnee_a_100() {
        Facture f = CalculFacture.calculer(List.of(ligne("1500.00")), "BIENVENUE10", "FR");
        assertEquals(bd("100.00"), f.remise());
        assertEquals(bd("1400.00"), f.totalHt());
        assertEquals(bd("1680.00"), f.totalTtc());
    }

    // R6 + R8
    @Test
    void r6_r8_repartition_remise_et_ventilation_tva() {
        List<LigneFacture> lignes = List.of(
                new LigneFacture("livre", bd("33.33"), 1, bd("5.5")),
                new LigneFacture("repas", bd("33.33"), 1, bd("10")),
                new LigneFacture("casque", bd("33.34"), 1, bd("20")));
        Facture f = CalculFacture.calculer(lignes, "BIENVENUE10", "FR");
        assertEquals(bd("10.00"), f.remise());
        assertEquals(bd("90.00"), f.totalHt()); // le centime d'écart va sur la dernière ligne
        assertEquals(Map.of(bd("5.5"), bd("1.65"), bd("10"), bd("3.00"), bd("20"), bd("6.00")), f.tvaParTaux());
        assertEquals(bd("10.65"), f.totalTva());
        assertEquals(bd("100.65"), f.totalTtc());
    }

    // R7
    @Test
    void r7_frais_de_port_sous_50_euros() {
        Facture f = CalculFacture.calculer(List.of(ligne("15.00", 2, "20")), null, "FR");
        assertEquals(bd("9.90"), f.fraisPortHt());
        assertEquals(bd("39.90"), f.totalHt());
        assertEquals(Map.of(bd("20"), bd("7.98")), f.tvaParTaux());
        assertEquals(bd("47.88"), f.totalTtc());
    }

    @Test
    void r7_pas_de_frais_de_port_a_50_euros_pile() {
        Facture f = CalculFacture.calculer(List.of(ligne("50.00")), null, "FR");
        assertEquals(bd("0.00"), f.fraisPortHt());
        assertEquals(bd("60.00"), f.totalTtc());
    }

    @Test
    void r7_frais_de_port_calcules_apres_remise() {
        Facture f = CalculFacture.calculer(List.of(ligne("52.00")), "BIENVENUE10", "FR"); // 46.80 après remise
        assertEquals(bd("9.90"), f.fraisPortHt());
        assertEquals(bd("56.70"), f.totalHt());
        assertEquals(bd("11.34"), f.totalTva());
        assertEquals(bd("68.04"), f.totalTtc());
    }

    // R9
    @Test
    void r9_export_sans_tva() {
        Facture f = CalculFacture.calculer(List.of(ligne("100.00")), null, "BE");
        assertEquals(Map.of(), f.tvaParTaux());
        assertEquals(bd("0.00"), f.totalTva());
        assertEquals(bd("100.00"), f.totalTtc());
    }
}
