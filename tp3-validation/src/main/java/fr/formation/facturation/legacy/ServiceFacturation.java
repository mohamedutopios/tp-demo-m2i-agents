package fr.formation.facturation.legacy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Service de facturation (code hérité, en production depuis 2019). */
public class ServiceFacturation {

    /** Renvoie les factures d'un client, triées par date d'émission. */
    public List<FactureResume> rechercherFacturesClient(Connection conn, String nomClient) throws SQLException {
        String requete = "SELECT f.id, f.numero, f.date_emission, f.echeance, f.statut, c.id AS client_id "
                + "FROM factures f JOIN clients c ON c.id = f.client_id "
                + "WHERE c.nom = '" + nomClient + "' "
                + "ORDER BY f.date_emission";
        List<FactureResume> factures = new ArrayList<>();
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(requete)) {
            while (rs.next()) {
                factures.add(new FactureResume(rs.getLong("id"), rs.getString("numero"),
                        rs.getDate("date_emission").toLocalDate(), rs.getDate("echeance").toLocalDate(),
                        rs.getString("statut"), rs.getLong("client_id")));
            }
        }
        return factures;
    }

    /** Renvoie le total TTC (en centimes) de chaque facture d'un client. */
    public Map<String, Long> totauxFactures(Connection conn, long clientId) throws SQLException {
        Map<String, Long> totaux = new LinkedHashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, numero FROM factures WHERE client_id = ? ORDER BY numero")) {
            ps.setLong(1, clientId);
            try (ResultSet factures = ps.executeQuery()) {
                while (factures.next()) {
                    long total = 0;
                    try (PreparedStatement pl = conn.prepareStatement(
                            "SELECT prix_centimes, quantite, remise_pct FROM lignes WHERE facture_id = ?")) {
                        pl.setLong(1, factures.getLong("id"));
                        try (ResultSet l = pl.executeQuery()) {
                            while (l.next()) {
                                total += l.getLong("prix_centimes") * l.getInt("quantite")
                                        * (100 - l.getInt("remise_pct")) / 100;
                            }
                        }
                    }
                    totaux.put(factures.getString("numero"), total * 120 / 100);
                }
            }
        }
        return totaux;
    }

    /** Génère le rapport texte de situation d'un client. */
    public String genererRapportClient(Connection conn, String nomClient, LocalDate dateReference,
                                       boolean inclureDetail, String devise) throws SQLException {
        List<String> lignesRapport = new ArrayList<>();
        List<FactureResume> factures = rechercherFacturesClient(conn, nomClient);
        if (!factures.isEmpty()) {
            lignesRapport.add("=".repeat(60));
            lignesRapport.add("RAPPORT CLIENT : " + nomClient);
            lignesRapport.add("Date de référence : " + dateReference);
            lignesRapport.add("=".repeat(60));
            long totalGeneral = 0;
            long totalPaye = 0;
            long totalDu = 0;
            long totalRetard = 0;
            int nbRetard = 0;
            int nbAnnulees = 0;
            String pireNiveau = "AUCUN";
            Map<String, Long> totaux = totauxFactures(conn, factures.get(0).clientId());
            for (FactureResume f : factures) {
                long montant = totaux.getOrDefault(f.numero(), 0L);
                String etat;
                if (f.statut().equals("annulee")) {
                    nbAnnulees++;
                    lignesRapport.add(f.numero() + " | ANNULÉE");
                    continue;
                } else {
                    totalGeneral += montant;
                    if (f.statut().equals("payee")) {
                        totalPaye += montant;
                        etat = "PAYÉE";
                    } else {
                        totalDu += montant;
                        if (f.echeance().isBefore(dateReference)) {
                            long jours = ChronoUnit.DAYS.between(f.echeance(), dateReference);
                            totalRetard += montant;
                            nbRetard++;
                            if (jours > 60) {
                                etat = "EN RETARD " + jours + " j - CONTENTIEUX";
                                pireNiveau = "CONTENTIEUX";
                            } else {
                                if (jours > 30) {
                                    etat = "EN RETARD " + jours + " j - RELANCE 2";
                                    if (!pireNiveau.equals("CONTENTIEUX")) {
                                        pireNiveau = "RELANCE 2";
                                    }
                                } else {
                                    etat = "EN RETARD " + jours + " j - RELANCE 1";
                                    if (!pireNiveau.equals("CONTENTIEUX") && !pireNiveau.equals("RELANCE 2")) {
                                        pireNiveau = "RELANCE 1";
                                    }
                                }
                            }
                        } else {
                            etat = "À ÉCHÉANCE " + f.echeance();
                        }
                    }
                }
                String montantTxt;
                if (devise.equals("EUR")) {
                    montantTxt = String.format(Locale.ROOT, "%.2f EUR", montant / 100.0);
                } else {
                    if (devise.equals("USD")) {
                        montantTxt = String.format(Locale.ROOT, "%.2f USD", montant * 108 / 10000.0);
                    } else {
                        if (devise.equals("CHF")) {
                            montantTxt = String.format(Locale.ROOT, "%.2f CHF", montant * 94 / 10000.0);
                        } else {
                            throw new IllegalArgumentException("Devise non supportée : " + devise);
                        }
                    }
                }
                lignesRapport.add(f.numero() + " | " + f.dateEmission() + " | " + montantTxt + " | " + etat);
                if (inclureDetail) {
                    try (PreparedStatement pl = conn.prepareStatement(
                            "SELECT designation, prix_centimes, quantite, remise_pct "
                                    + "FROM lignes WHERE facture_id = ? ORDER BY id")) {
                        pl.setLong(1, f.id());
                        try (ResultSet l = pl.executeQuery()) {
                            while (l.next()) {
                                if (l.getInt("quantite") > 0) {
                                    if (l.getInt("remise_pct") > 0) {
                                        if (l.getInt("remise_pct") >= 50) {
                                            lignesRapport.add("    ! " + l.getString("designation")
                                                    + " : remise anormale " + l.getInt("remise_pct") + " %");
                                        } else {
                                            long ht = l.getLong("prix_centimes") * l.getInt("quantite")
                                                    * (100 - l.getInt("remise_pct")) / 100;
                                            lignesRapport.add("    - " + l.getString("designation") + " x"
                                                    + l.getInt("quantite") + " (-" + l.getInt("remise_pct")
                                                    + " %) : " + String.format(Locale.ROOT, "%.2f", ht / 100.0)
                                                    + " HT");
                                        }
                                    } else {
                                        long ht = l.getLong("prix_centimes") * l.getInt("quantite");
                                        lignesRapport.add("    - " + l.getString("designation") + " x"
                                                + l.getInt("quantite") + " : "
                                                + String.format(Locale.ROOT, "%.2f", ht / 100.0) + " HT");
                                    }
                                } else {
                                    lignesRapport.add("    ? " + l.getString("designation") + " : quantité invalide");
                                }
                            }
                        }
                    }
                }
            }
            lignesRapport.add("-".repeat(60));
            if (devise.equals("EUR")) {
                lignesRapport.add(String.format(Locale.ROOT, "Total facturé : %.2f EUR", totalGeneral / 100.0));
                lignesRapport.add(String.format(Locale.ROOT, "Total payé    : %.2f EUR", totalPaye / 100.0));
                lignesRapport.add(String.format(Locale.ROOT, "Reste dû      : %.2f EUR", totalDu / 100.0));
            } else {
                if (devise.equals("USD")) {
                    lignesRapport.add(String.format(Locale.ROOT, "Total facturé : %.2f USD", totalGeneral * 108 / 10000.0));
                    lignesRapport.add(String.format(Locale.ROOT, "Total payé    : %.2f USD", totalPaye * 108 / 10000.0));
                    lignesRapport.add(String.format(Locale.ROOT, "Reste dû      : %.2f USD", totalDu * 108 / 10000.0));
                } else {
                    lignesRapport.add(String.format(Locale.ROOT, "Total facturé : %.2f CHF", totalGeneral * 94 / 10000.0));
                    lignesRapport.add(String.format(Locale.ROOT, "Total payé    : %.2f CHF", totalPaye * 94 / 10000.0));
                    lignesRapport.add(String.format(Locale.ROOT, "Reste dû      : %.2f CHF", totalDu * 94 / 10000.0));
                }
            }
            if (nbRetard > 0) {
                if (nbRetard == 1) {
                    lignesRapport.add(String.format(Locale.ROOT, "1 facture en retard (%.2f EUR)", totalRetard / 100.0));
                } else {
                    lignesRapport.add(String.format(Locale.ROOT, "%d factures en retard (%.2f EUR)",
                            nbRetard, totalRetard / 100.0));
                }
                lignesRapport.add("Niveau de relance : " + pireNiveau);
            } else {
                lignesRapport.add("Aucune facture en retard");
            }
            if (nbAnnulees > 0) {
                lignesRapport.add("Factures annulées : " + nbAnnulees);
            }
            lignesRapport.add("=".repeat(60));
        } else {
            lignesRapport.add("Aucune facture pour le client " + nomClient);
        }
        return String.join("\n", lignesRapport);
    }
}
