package com.example.repository;

import com.example.model.Reservation;
import com.example.model.Salle;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Stateless
public class SalleRepositoryImpl implements SalleRepository {
    public SalleRepositoryImpl() { }

    public SalleRepositoryImpl(EntityManager em) {
        this.entityManager = em;
    }
    @PersistenceContext(unitName = "SallePU")
    private EntityManager entityManager;

    @Override
    public List<Salle> findAvailableRooms(LocalDateTime debut, LocalDateTime fin) {
        String jpql = "SELECT s FROM Salle s WHERE NOT EXISTS (" +
                "SELECT r FROM Reservation r WHERE r.salle = s " +
                "AND :debut < r.fin AND :fin > r.debut)";
        return entityManager.createQuery(jpql, Salle.class)
                .setParameter("debut", debut)
                .setParameter("fin", fin)
                .getResultList();
    }

    @Override
    public List<Salle> searchRooms(String nom, Integer capaciteMin,
                                   LocalDateTime debut, LocalDateTime fin,
                                   int page, int size) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Salle> cq = cb.createQuery(Salle.class);
        Root<Salle> salle = cq.from(Salle.class);

        List<Predicate> predicates = buildPredicates(cb, cq, salle, nom, capaciteMin, debut, fin);
        cq.select(salle)
                .where(cb.and(predicates.toArray(new Predicate[0])))
                .orderBy(cb.asc(salle.get("nom")));

        TypedQuery<Salle> query = entityManager.createQuery(cq);
        query.setFirstResult((page - 1) * size);   // page 1-based
        query.setMaxResults(size);
        return query.getResultList();
    }

    @Override
    public long countRooms(String nom, Integer capaciteMin,
                           LocalDateTime debut, LocalDateTime fin) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Salle> salle = cq.from(Salle.class);

        // Mêmes filtres, reconstruits sur la racine de CETTE requête
        List<Predicate> predicates = buildPredicates(cb, cq, salle, nom, capaciteMin, debut, fin);
        cq.select(cb.count(salle))
                .where(cb.and(predicates.toArray(new Predicate[0])));

        return entityManager.createQuery(cq).getSingleResult();
    }

    private List<Predicate> buildPredicates(CriteriaBuilder cb, CriteriaQuery<?> cq,
                                            Root<Salle> salle,
                                            String nom, Integer capaciteMin,
                                            LocalDateTime debut, LocalDateTime fin) {
        List<Predicate> predicates = new ArrayList<>();

        if (nom != null && !nom.trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(salle.<String>get("nom")),
                    "%" + nom.trim().toLowerCase() + "%"));
        }

        if (capaciteMin != null) {
            predicates.add(cb.greaterThanOrEqualTo(salle.<Integer>get("capacite"), capaciteMin));
        }

        if (debut != null && fin != null) {
            Subquery<Long> sub = cq.subquery(Long.class);
            Root<Reservation> r = sub.from(Reservation.class);
            sub.select(r.get("salle").<Long>get("id"))
                    .where(cb.and(
                            cb.lessThan(r.<LocalDateTime>get("debut"), fin),
                            cb.greaterThan(r.<LocalDateTime>get("fin"), debut)
                    ));
            predicates.add(cb.not(salle.get("id").in(sub)));
        }

        return predicates;
    }
}