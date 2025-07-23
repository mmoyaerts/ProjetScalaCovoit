package services

import dao.{TripDAO, VehicleDAO}
import models.Trip
import java.time.LocalDateTime
import scala.util.{Try, Success, Failure}

class TripService {
  private val tripDAO = new TripDAO()
  private val vehicleDAO = new VehicleDAO()
  
  def createTrip(driverId: Long, vehicleId: Long, departureCity: String, arrivalCity: String,
                 departureTime: LocalDateTime, arrivalTime: LocalDateTime, availableSeats: Int,
                 pricePerSeat: BigDecimal, description: Option[String]): Try[Long] = {
    
    // Vérifier que le véhicule appartient au conducteur
    vehicleDAO.findById(vehicleId) match {
      case Success(Some(vehicle)) =>
        if (vehicle.userId != driverId) {
          Failure(new RuntimeException("Ce véhicule ne vous appartient pas"))
        } else if (availableSeats > vehicle.seats) {
          Failure(new RuntimeException(s"Le nombre de places disponibles ne peut pas dépasser ${vehicle.seats}"))
        } else if (departureTime.isBefore(LocalDateTime.now())) {
          Failure(new RuntimeException("L'heure de départ ne peut pas être dans le passé"))
        } else if (!arrivalTime.isAfter(departureTime)) {
          Failure(new RuntimeException("L'heure d'arrivée doit être après l'heure de départ"))
        } else {
          val trip = Trip(
            driverId = driverId,
            vehicleId = vehicleId,
            departureCity = departureCity,
            arrivalCity = arrivalCity,
            departureTime = departureTime,
            arrivalTime = arrivalTime,
            availableSeats = availableSeats,
            pricePerSeat = pricePerSeat,
            description = description
          )
          tripDAO.create(trip)
        }
      case Success(None) =>
        Failure(new RuntimeException("Véhicule non trouvé"))
      case Failure(exception) =>
        Failure(exception)
    }
  }
  
  def getDriverTrips(driverId: Long): Try[List[Trip]] = {
    tripDAO.findByDriverId(driverId)
  }
  
  def getDriverUpcomingTrips(driverId: Long): Try[List[Trip]] = {
    tripDAO.findUpcomingTripsByDriverId(driverId)
  }
  
  def getDriverPastTrips(driverId: Long): Try[List[Trip]] = {
    tripDAO.findPastTripsByDriverId(driverId)
  }
  
  def updateTrip(tripId: Long, driverId: Long, vehicleId: Long, departureCity: String,
                 arrivalCity: String, departureTime: LocalDateTime, arrivalTime: LocalDateTime,
                 availableSeats: Int, pricePerSeat: BigDecimal, description: Option[String]): Try[Boolean] = {
    
    tripDAO.findById(tripId) match {
      case Success(Some(trip)) =>
        if (trip.driverId != driverId) {
          Failure(new RuntimeException("Ce trajet ne vous appartient pas"))
        } else if (departureTime.isBefore(LocalDateTime.now())) {
          Failure(new RuntimeException("L'heure de départ ne peut pas être dans le passé"))
        } else if (!arrivalTime.isAfter(departureTime)) {
          Failure(new RuntimeException("L'heure d'arrivée doit être après l'heure de départ"))
        } else {
          // Vérifier le véhicule
          vehicleDAO.findById(vehicleId) match {
            case Success(Some(vehicle)) =>
              if (vehicle.userId != driverId) {
                Failure(new RuntimeException("Ce véhicule ne vous appartient pas"))
              } else if (availableSeats > vehicle.seats) {
                Failure(new RuntimeException(s"Le nombre de places disponibles ne peut pas dépasser ${vehicle.seats}"))
              } else {
                val updatedTrip = trip.copy(
                  vehicleId = vehicleId,
                  departureCity = departureCity,
                  arrivalCity = arrivalCity,
                  departureTime = departureTime,
                  arrivalTime = arrivalTime,
                  availableSeats = availableSeats,
                  pricePerSeat = pricePerSeat,
                  description = description
                )
                tripDAO.update(updatedTrip).map(_ > 0)
              }
            case Success(None) =>
              Failure(new RuntimeException("Véhicule non trouvé"))
            case Failure(exception) =>
              Failure(exception)
          }
        }
      case Success(None) =>
        Failure(new RuntimeException("Trajet non trouvé"))
      case Failure(exception) =>
        Failure(exception)
    }
  }
  
  def deleteTrip(tripId: Long, driverId: Long): Try[Boolean] = {
    tripDAO.findById(tripId) match {
      case Success(Some(trip)) =>
        if (trip.driverId != driverId) {
          Failure(new RuntimeException("Ce trajet ne vous appartient pas"))
        } else {
          // Vérifier s'il y a des réservations
          tripDAO.hasReservations(tripId) match {
            case Success(true) =>
              Failure(new RuntimeException("Impossible de supprimer un trajet avec des réservations"))
            case Success(false) =>
              tripDAO.delete(tripId).map(_ > 0)
            case Failure(exception) =>
              Failure(exception)
          }
        }
      case Success(None) =>
        Failure(new RuntimeException("Trajet non trouvé"))
      case Failure(exception) =>
        Failure(exception)
    }
  }
  
  def getTripById(tripId: Long): Try[Option[Trip]] = {
    tripDAO.findById(tripId)
  }
  
  def searchTrips(departureCity: String, arrivalCity: String, departureDate: LocalDateTime): Try[List[Trip]] = {
    tripDAO.searchTrips(departureCity, arrivalCity, departureDate)
  }
}