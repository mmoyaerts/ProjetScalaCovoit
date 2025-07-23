package services

import dao.{ReservationDAO, TripDAO}
import models.{Reservation, Trip}
import java.time.LocalDateTime
import scala.util.{Try, Success, Failure}

class ReservationService {
  private val reservationDAO = new ReservationDAO()
  private val tripDAO = new TripDAO()
  
  def makeReservation(tripId: Long, passengerId: Long, seatsRequested: Int): Try[Long] = {
    tripDAO.findById(tripId) match {
      case Success(Some(trip)) =>
        if (trip.driverId == passengerId) {
          Failure(new RuntimeException("Vous ne pouvez pas réserver votre propre trajet"))
        } else if (trip.availableSeats < seatsRequested) {
          Failure(new RuntimeException(s"Pas assez de places disponibles. Places restantes: ${trip.availableSeats}"))
        } else if (trip.departureTime.isBefore(LocalDateTime.now())) {
          Failure(new RuntimeException("Ce trajet est déjà passé"))
        } else if (!trip.isActive) {
          Failure(new RuntimeException("Ce trajet n'est plus actif"))
        } else {
          val totalPrice = trip.pricePerSeat * seatsRequested
          val reservation = Reservation(
            tripId = tripId,
            passengerId = passengerId,
            seatsReserved = seatsRequested,
            totalPrice = totalPrice
          )
          
          // Créer la réservation
          reservationDAO.create(reservation) match {
            case Success(reservationId) =>
              // Mettre à jour les places disponibles
              val updatedTrip = trip.copy(availableSeats = trip.availableSeats - seatsRequested)
              tripDAO.update(updatedTrip) match {
                case Success(_) => Success(reservationId)
                case Failure(exception) => Failure(exception)
              }
            case Failure(exception) => Failure(exception)
          }
        }
      case Success(None) =>
        Failure(new RuntimeException("Trajet non trouvé"))
      case Failure(exception) =>
        Failure(exception)
    }
  }
  
  def cancelReservation(reservationId: Long, passengerId: Long): Try[Boolean] = {
    reservationDAO.findById(reservationId) match {
      case Success(Some(reservation)) =>
        if (reservation.passengerId != passengerId) {
          Failure(new RuntimeException("Cette réservation ne vous appartient pas"))
        } else if (reservation.status != "CONFIRMED") {
          Failure(new RuntimeException("Cette réservation est déjà annulée"))
        } else {
          // Vérifier que le trajet n'est pas déjà passé
          tripDAO.findById(reservation.tripId) match {
            case Success(Some(trip)) =>
              if (trip.departureTime.isBefore(LocalDateTime.now())) {
                Failure(new RuntimeException("Impossible d'annuler une réservation pour un trajet déjà passé"))
              } else {
                // Annuler la réservation
                reservationDAO.updateStatus(reservationId, "CANCELLED") match {
                  case Success(_) =>
                    // Remettre les places disponibles
                    val updatedTrip = trip.copy(availableSeats = trip.availableSeats + reservation.seatsReserved)
                    tripDAO.update(updatedTrip).map(_ > 0)
                  case Failure(exception) => Failure(exception)
                }
              }
            case Success(None) =>
              Failure(new RuntimeException("Trajet non trouvé"))
            case Failure(exception) =>
              Failure(exception)
          }
        }
      case Success(None) =>
        Failure(new RuntimeException("Réservation non trouvée"))
      case Failure(exception) =>
        Failure(exception)
    }
  }
  
  def getPassengerReservations(passengerId: Long): Try[List[Reservation]] = {
    reservationDAO.findByPassengerId(passengerId)
  }
  
  def getDriverReservations(driverId: Long): Try[List[Reservation]] = {
    reservationDAO.findByDriverId(driverId)
  }
  
  def getReservationById(reservationId: Long): Try[Option[Reservation]] = {
    reservationDAO.findById(reservationId)
  }
}