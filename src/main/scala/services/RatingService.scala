package services

import dao.{RatingDAO, TripDAO, ReservationDAO, UserDAO}
import models.Rating
import java.time.LocalDateTime
import scala.util.{Try, Success, Failure}

class RatingService {
  private val ratingDAO = new RatingDAO()
  private val tripDAO = new TripDAO()
  private val reservationDAO = new ReservationDAO()
  private val userDAO = new UserDAO()
  
  def rateUser(tripId: Long, raterId: Long, ratedId: Long, rating: Int, comment: Option[String]): Try[Long] = {
    // Validations
    if (rating < 1 || rating > 5) {
      return Failure(new RuntimeException("La note doit être comprise entre 1 et 5"))
    }
    
    if (raterId == ratedId) {
      return Failure(new RuntimeException("Vous ne pouvez pas vous noter vous-même"))
    }
    
    // Vérifier que le trajet existe
    tripDAO.findById(tripId) match {
      case Success(Some(trip)) =>
        // Vérifier que le trajet est passé
        if (trip.departureTime.isAfter(LocalDateTime.now())) {
          return Failure(new RuntimeException("Vous ne pouvez noter qu'après la fin du trajet"))
        }
        
        // Vérifier que l'utilisateur a participé au trajet
        val isDriverRatingPassenger = trip.driverId == raterId
        val isPassengerRatingDriver = trip.driverId == ratedId
        
        if (!isDriverRatingPassenger && !isPassengerRatingDriver) {
          // Vérifier si l'utilisateur était passager
          reservationDAO.findByTripId(tripId) match {
            case Success(reservations) =>
              val hasParticipated = reservations.exists(r => 
                (r.passengerId == raterId && trip.driverId == ratedId) ||
                (r.passengerId == ratedId && trip.driverId == raterId)
              )
              if (!hasParticipated) {
                return Failure(new RuntimeException("Vous ne pouvez noter que les personnes avec qui vous avez voyagé"))
              }
            case Failure(exception) => return Failure(exception)
          }
        }
        
        // Vérifier qu'une note n'existe pas déjà
        ratingDAO.existsRating(tripId, raterId, ratedId) match {
          case Success(true) =>
            Failure(new RuntimeException("Vous avez déjà noté cette personne pour ce trajet"))
          case Success(false) =>
            val newRating = Rating(
              tripId = tripId,
              raterId = raterId,
              ratedId = ratedId,
              rating = rating,
              comment = comment
            )
            
            ratingDAO.create(newRating) match {
              case Success(ratingId) =>
                // Mettre à jour la moyenne de l'utilisateur noté
                updateUserAverageRating(ratedId)
                Success(ratingId)
              case Failure(exception) => Failure(exception)
            }
          case Failure(exception) => Failure(exception)
        }
        
      case Success(None) =>
        Failure(new RuntimeException("Trajet non trouvé"))
      case Failure(exception) =>
        Failure(exception)
    }
  }
  
  def getUserRatings(userId: Long): Try[List[Rating]] = {
    ratingDAO.findByRatedId(userId)
  }
  
  def getRatingsGiven(userId: Long): Try[List[Rating]] = {
    ratingDAO.findByRaterId(userId)
  }
  
  def getTripRatings(tripId: Long): Try[List[Rating]] = {
    ratingDAO.findByTripId(tripId)
  }
  
  def getUserRatingStats(userId: Long): Try[Map[String, Any]] = {
    for {
      ratings <- getUserRatings(userId)
      average <- ratingDAO.getAverageRating(userId)
      count <- ratingDAO.getRatingCount(userId)
    } yield {
      val ratingDistribution = ratings.groupBy(_.rating).map { case (rating, list) => rating -> list.length }
      
      Map(
        "averageRating" -> average,
        "totalRatings" -> count,
        "ratingDistribution" -> ratingDistribution,
        "recentRatings" -> ratings.take(5)
      )
    }
  }
  
  def getEligibleTripsForRating(userId: Long): Try[List[(Long, String, String, LocalDateTime)]] = {
    // Récupérer les trajets passés où l'utilisateur était conducteur ou passager
    tripDAO.findPastTripsByDriverId(userId) match {
      case Success(driverTrips) =>
        val driverTripInfo = driverTrips.map(trip => 
          (trip.id.get, s"${trip.departureCity} → ${trip.arrivalCity}", "conducteur", trip.departureTime)
        )
        
        // Récupérer les trajets où l'utilisateur était passager
        reservationDAO.findByPassengerId(userId) match {
          case Success(reservations) =>
            val passengerTripsFuture = reservations.map { reservation =>
              tripDAO.findById(reservation.tripId) match {
                case Success(Some(trip)) if trip.departureTime.isBefore(LocalDateTime.now()) =>
                  Some((trip.id.get, s"${trip.departureCity} → ${trip.arrivalCity}", "passager", trip.departureTime))
                case _ => None
              }
            }.flatten
            
            Success((driverTripInfo ++ passengerTripsFuture).sortBy(_._4).reverse)
          case Failure(exception) => Failure(exception)
        }
      case Failure(exception) => Failure(exception)
    }
  }
  
  private def updateUserAverageRating(userId: Long): Try[Unit] = {
    ratingDAO.getAverageRating(userId) match {
      case Success(Some(average)) =>
        userDAO.updateAverageRating(userId, average).map(_ => ())
      case Success(None) =>
        userDAO.updateAverageRating(userId, 0.0).map(_ => ())
      case Failure(exception) => Failure(exception)
    }
  }
}