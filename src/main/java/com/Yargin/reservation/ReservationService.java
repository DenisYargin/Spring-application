package com.Yargin.reservation;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ReservationService {
    private final Map<Long, Reservation> reservationMap;
    private final AtomicLong idCounter;
    private final ReservationRepository repository;

    public ReservationService(ReservationRepository repository) {
        this.repository = repository;
        reservationMap = new HashMap<>();
        idCounter = new AtomicLong();
    }

    public Reservation getReservationById(
            Long id
    ) {
        if (!reservationMap.containsKey(id)){
            throw new NoSuchElementException("Нет бронирования у клиента = " + id);
        }
        return reservationMap.get(id);
    }

    public List<Reservation> findAllReservation() {
        List<ReservationEntity> allEntities = repository.findAll();
        return allEntities.stream().map(it ->
            new Reservation(
                it.getId(),
                it.getUserId(),
                it.getRoomId(),
                it.getStartDate(),
                it.getEndDate(),
                it.getStatus()
            )
        ).toList();
    }

    public Reservation createReservation(Reservation reservationToCreate) {
        if (reservationToCreate.id() != null){
            throw new IllegalArgumentException("Нельзя задавать id, задается системой");
        }
        if (reservationToCreate.status() != null){
            throw new IllegalArgumentException("Нельзя задавать статус, задается системой");
        }
        var newReservation = new Reservation(
                idCounter.incrementAndGet(),
                reservationToCreate.userId(),
                reservationToCreate.roomId(),
                reservationToCreate.startDate(),
                reservationToCreate.endDate(),
                ReservationStatus.PENDING
        );
        reservationMap.put(newReservation.id(), newReservation);
        return newReservation;

    }

    public Reservation updateReservation(
            Long id,
            Reservation reservationToUpdate
    ) {
        if (!reservationMap.containsKey(id)){
            throw new NoSuchElementException("По этому id брони не существует");
        }
        var reservation = reservationMap.get(id);
        if (reservation.status() != ReservationStatus.PENDING){
            throw new IllegalStateException("Не возможно изменить, статус =" + reservation.status());
        }
        var updatedReservation = new Reservation(
                reservation.id(),
                reservationToUpdate.userId(),
                reservationToUpdate.roomId(),
                reservationToUpdate.startDate(),
                reservationToUpdate.endDate(),
                ReservationStatus.PENDING
        );
        reservationMap.put(reservation.id(), reservationToUpdate);
        return updatedReservation;
    }

    public void deleteReservation(Long id) {
        if (!reservationMap.containsKey(id)){
            throw new NoSuchElementException("По этому id брони не существует");
        }
        reservationMap.remove(id);
    }

    public Reservation approveReservation(Long id) {
        if (!reservationMap.containsKey(id)){
            throw new NoSuchElementException("По этому id брони не существует");
        }
        var reservation = reservationMap.get(id);
        if(reservation.status() != ReservationStatus.PENDING){
            throw new IllegalStateException("Нельзя подтвердить бронь, статус=" + reservation.status());
        }
        var isConflict = reservationConflict(reservation);
        if(isConflict){
            throw new IllegalStateException("Нельзя подтвердить бронь, есть пересечение сдругими датами");
        }
        var approvedReservation = new Reservation(
                reservation.id(),
                reservation.userId(),
                reservation.roomId(),
                reservation.startDate(),
                reservation.endDate(),
                ReservationStatus.APPROVED
        );
        reservationMap.put(reservation.id(), approvedReservation);
        return approvedReservation;

    }

    private Boolean reservationConflict(
            Reservation reservation
    ) {
        for (Reservation existingReservation: reservationMap.values()){
            if(reservation.id().equals(existingReservation.id())) {
                continue;
            }
            if(!reservation.roomId().equals(existingReservation.roomId())){
                continue;
            }
            if(!reservation.status().equals(ReservationStatus.APPROVED)){
                continue;
            }
            if(reservation.startDate().isBefore(existingReservation.endDate())
                && existingReservation.startDate().isBefore(reservation.endDate())){
                return true;
            }
        }
        return false;
    }
}
