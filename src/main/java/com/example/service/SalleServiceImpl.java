package com.example.service;

import com.example.model.Salle;
import com.example.repository.SalleRepository;
import com.example.util.PaginationResult;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.List;

@Stateless
public class SalleServiceImpl implements SalleService {

    @Inject
    private SalleRepository salleRepository;

    public SalleServiceImpl() { }   // requis par le conteneur EJB

    public SalleServiceImpl(SalleRepository salleRepository) {   // pour Java SE
        this.salleRepository = salleRepository;
    }

    @Override
    public List<Salle> getAvailableRooms(LocalDateTime debut, LocalDateTime fin) {
        verifierPeriode(debut, fin);
        return salleRepository.findAvailableRooms(debut, fin);
    }

    @Override
    public List<Salle> searchRooms(String nom, Integer capaciteMin,
                                   LocalDateTime debut, LocalDateTime fin,
                                   int page, int size) {
        verifierPagination(page, size);
        verifierPeriode(debut, fin);
        return salleRepository.searchRooms(nom, capaciteMin, debut, fin, page, size);
    }

    @Override
    public long countRooms(String nom, Integer capaciteMin,
                           LocalDateTime debut, LocalDateTime fin) {
        verifierPeriode(debut, fin);
        return salleRepository.countRooms(nom, capaciteMin, debut, fin);
    }

    @Override
    public PaginationResult<Salle> findPaginated(String nom, Integer capaciteMin,
                                                 LocalDateTime debut, LocalDateTime fin,
                                                 int page, int size) {
        List<Salle> items = searchRooms(nom, capaciteMin, debut, fin, page, size);
        long total = countRooms(nom, capaciteMin, debut, fin);
        return new PaginationResult<>(items, page, size, total);
    }

    // ---- Règles métier ----
    private void verifierPagination(int page, int size) {
        if (page < 1 || size < 1) {
            throw new IllegalArgumentException("page et size doivent être >= 1");
        }
    }

    private void verifierPeriode(LocalDateTime debut, LocalDateTime fin) {
        if (debut != null && fin != null && !debut.isBefore(fin)) {
            throw new IllegalArgumentException("debut doit être avant fin");
        }
    }
}