package com.example;

import com.example.model.Reservation;
import com.example.model.Salle;
import com.example.repository.SalleRepositoryImpl;
import com.example.service.SalleService;
import com.example.service.SalleServiceImpl;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.time.LocalDateTime;
import java.util.List;

public class App {

    static LocalDateTime h(int heure) {
        return LocalDateTime.of(2026, 10, 15, heure, 0);
    }

    public static void main(String[] args) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("SallePU");
        EntityManager em = emf.createEntityManager();
        SalleService service = new SalleServiceImpl(new SalleRepositoryImpl(em));

        try {
            // ---- Objets de départ ----
            Salle amphi = creerSalle(em, "Amphi 1", 100);
            Salle b1    = creerSalle(em, "Salle B1", 30);
            Salle labo  = creerSalle(em, "Labo C1", 20);

            creerReservation(em, amphi, h(9), h(11));
            creerReservation(em, amphi, h(14), h(16));
            Reservation r3 = creerReservation(em, b1, h(10), h(12));

            // ---- Scénario 1 ----
            afficher("1. Disponibles 10h-12h (attendu : Labo C1)",
                    service.getAvailableRooms(h(10), h(12)));

            // ---- Scénario 2 : on réserve le labo ----
            creerReservation(em, labo, h(10), h(11));
            afficher("2. Après réservation du labo 10h-11h (attendu : aucune)",
                    service.getAvailableRooms(h(10), h(12)));

            // ---- Scénario 3 : créneaux adjacents ----
            afficher("3. Disponibles 11h-14h (attendu : Amphi 1, Labo C1)",
                    service.getAvailableRooms(h(11), h(14)));

            // ---- Scénario 4 : navigation Salle -> Réservations ----
            em.clear();   // on force un rechargement depuis la base
            Salle amphiRecharge = em.find(Salle.class, amphi.getId());
            System.out.println("\n4. Réservations de " + amphiRecharge.getNom()
                    + " : " + amphiRecharge.getReservations().size() + " (attendu : 2)");
            for (Reservation r : amphiRecharge.getReservations()) {
                System.out.println("   " + r.getDebut().toLocalTime()
                        + " -> " + r.getFin().toLocalTime());
            }

            // ---- Scénario 5 : annulation ----
            supprimerReservation(em, r3.getId());
            afficher("5. Après annulation de B1 10h-12h (attendu : Salle B1)",
                    service.getAvailableRooms(h(10), h(12)));

            // ---- Scénario 6 : filtres combinés ----
            long n = service.countRooms(null, 30, h(10), h(12));
            System.out.println("\n6. Disponibles 10h-12h avec capacité >= 30 : "
                    + n + " (attendu : 1)");

        } finally {
            em.close();
            emf.close();
        }
    }

    // ---------- Méthodes utilitaires ----------

    private static Salle creerSalle(EntityManager em, String nom, int capacite) {
        Salle s = new Salle();
        s.setNom(nom);
        s.setCapacite(capacite);
        em.getTransaction().begin();
        em.persist(s);
        em.getTransaction().commit();
        return s;
    }

    private static Reservation creerReservation(EntityManager em, Salle salle,
                                                LocalDateTime debut, LocalDateTime fin) {
        Reservation r = new Reservation();
        r.setSalle(salle);
        r.setDebut(debut);
        r.setFin(fin);
        em.getTransaction().begin();
        em.persist(r);
        em.getTransaction().commit();
        return r;
    }

    private static void supprimerReservation(EntityManager em, Long id) {
        em.getTransaction().begin();
        Reservation r = em.find(Reservation.class, id);
        if (r != null) {
            em.remove(r);
        }
        em.getTransaction().commit();
    }

    private static void afficher(String titre, List<Salle> salles) {
        System.out.println("\n" + titre);
        if (salles.isEmpty()) {
            System.out.println("   (aucune salle)");
        }
        salles.forEach(s -> System.out.println("   - " + s.getNom()
                + " (capacité " + s.getCapacite() + ")"));
    }
}