package com.example.service;

import com.example.model.Salle;
import com.example.util.PaginationResult;

import java.time.LocalDateTime;
import java.util.List;

public interface SalleService {
    List<Salle> getAvailableRooms(LocalDateTime debut, LocalDateTime fin);
    List<Salle> searchRooms(String nom, Integer capaciteMin, LocalDateTime debut, LocalDateTime fin, int page, int size);
    long countRooms(String nom, Integer capaciteMin, LocalDateTime debut, LocalDateTime fin);
    PaginationResult<Salle> findPaginated(String nom, Integer capaciteMin, LocalDateTime debut, LocalDateTime fin, int page, int size);
}