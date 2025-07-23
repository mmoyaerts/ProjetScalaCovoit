package menu

import services.{RatingService, TripService, UserService, ReservationService}
import models.{User, Rating}
import java.time.format.DateTimeFormatter
import scala.io.StdIn
import scala.util.{Try, Success, Failure}

class RatingManagementConsole(currentUser: User) {
  private val ratingService = new RatingService()
  private val tripService = new TripService()
  private val userService = new UserService()
  private val reservationService = new ReservationService()
  private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
  
  def start(): Unit = {
    var managing = true
    
    while (managing) {
      showRatingMenu()
      val choice = StdIn.readLine("Votre choix: ")
      
      choice match {
        case "1" => rateParticipant()
        case "2" => viewMyRatings()
        case "3" => viewRatingsGiven()
        case "4" => viewMyRatingStats()
        case "5" => viewEligibleTrips()
        case "6" => managing = false
        case _ => println("Choix invalide")
      }
    }
  }
  
  private def showRatingMenu(): Unit = {
    println("\n=== Système de notation ===")
    println("1. Noter un participant")
    println("2. Voir mes notes reçues")
    println("3. Voir mes notes données")
    println("4. Mes statistiques de notation")
    println("5. Trajets éligibles pour notation")
    println("6. Retour")
  }
  
  private def rateParticipant(): Unit = {
    println("\n=== Noter un participant ===")
    
    ratingService.getEligibleTripsForRating(currentUser.id.get) match {
      case Success(trips) =>
        if (trips.isEmpty) {
          println("Aucun trajet éligible pour notation")
          return
        }
        
        println("Trajets éligibles pour notation:")
        trips.foreach { case (tripId, route, role, date) =>
          println(s"ID: $tripId - $route ($role) - ${date.format(dateFormatter)}")
        }
        
        print("ID du trajet: ")
        val tripId = StdIn.readLong()
        StdIn.readLine() // consume newline
        
        // Récupérer les participants du trajet
        tripService.getTripById(tripId) match {
          case Success(Some(trip)) =>
            val isDriver = trip.driverId == currentUser.id.get
            
            if (isDriver) {
              // Le conducteur note les passagers
              reservationService.getDriverReservations(currentUser.id.get).map { reservations =>
                val tripReservations = reservations.filter(_.tripId == tripId)
                if (tripReservations.nonEmpty) {
                  println("Passagers à noter:")
                  tripReservations.foreach { reservation =>
                    userService.getUserById(reservation.passengerId) match {
                      case Success(Some(passenger)) =>
                        println(s"${passenger.id.get}. ${passenger.firstName} ${passenger.lastName}")
                      case _ => ()
                    }
                  }
                  
                  print("ID du passager à noter: ")
                  val passengerId = StdIn.readLong()
                  StdIn.readLine()
                  
                  performRating(tripId, passengerId)
                } else {
                  println("Aucun passager trouvé pour ce trajet")
                }
              }
            } else {
              // Le passager note le conducteur
              userService.getUserById(trip.driverId) match {
                case Success(Some(driver)) =>
                  println(s"Conducteur: ${driver.firstName} ${driver.lastName}")
                  performRating(tripId, trip.driverId)
                case _ =>
                  println("Conducteur non trouvé")
              }
            }
            
          case Success(None) =>
            println("Trajet non trouvé")
          case Failure(exception) =>
            println(s"Erreur: ${exception.getMessage}")
        }
        
      case Failure(exception) =>
        println(s"Erreur: ${exception.getMessage}")
    }
  }
  
  private def performRating(tripId: Long, ratedUserId: Long): Unit = {
    print("Note (1-5): ")
    val rating = StdIn.readInt()
    StdIn.readLine() // consume newline
    
    if (rating < 1 || rating > 5) {
      println("La note doit être comprise entre 1 et 5")
      return
    }
    
    print("Commentaire (optionnel): ")
    val commentInput = StdIn.readLine()
    val comment = if (commentInput.trim.isEmpty) None else Some(commentInput)
    
    ratingService.rateUser(tripId, currentUser.id.get, ratedUserId, rating, comment) match {
      case Success(ratingId) =>
        println(s"✅ Note attribuée avec succès ! (ID: $ratingId)")
      case Failure(exception) =>
        println(s"❌ Erreur: ${exception.getMessage}")
    }
  }
  
  private def viewMyRatings(): Unit = {
    println("\n=== Mes notes reçues ===")
    
    ratingService.getUserRatings(currentUser.id.get) match {
      case Success(ratings) =>
        if (ratings.isEmpty) {
          println("Aucune note reçue")
        } else {
          ratings.foreach { rating =>
            userService.getUserById(rating.raterId) match {
              case Success(Some(rater)) =>
                tripService.getTripById(rating.tripId) match {
                  case Success(Some(trip)) =>
                    println(s"⭐ ${rating.rating}/5 - Par: ${rater.firstName} ${rater.lastName}")
                    println(s"   Trajet: ${trip.departureCity} → ${trip.arrivalCity}")
                    println(s"   Date: ${trip.departureTime.format(dateFormatter)}")
                    rating.comment.foreach(c => println(s"   Commentaire: $c"))
                    println(s"   Noté le: ${rating.createdAt.format(dateFormatter)}")
                    println("---")
                  case _ =>
                    println("Erreur lors de la récupération du trajet")
                }
              case _ =>
                println("Erreur lors de la récupération de l'utilisateur")
            }
          }
        }
      case Failure(exception) =>
        println(s"Erreur: ${exception.getMessage}")
    }
  }
  
  private def viewRatingsGiven(): Unit = {
    println("\n=== Mes notes données ===")
    
    ratingService.getRatingsGiven(currentUser.id.get) match {
      case Success(ratings) =>
        if (ratings.isEmpty) {
          println("Aucune note donnée")
        } else {
          ratings.foreach { rating =>
            userService.getUserById(rating.ratedId) match {
              case Success(Some(rated)) =>
                tripService.getTripById(rating.tripId) match {
                  case Success(Some(trip)) =>
                    println(s"⭐ ${rating.rating}/5 - À: ${rated.firstName} ${rated.lastName}")
                    println(s"   Trajet: ${trip.departureCity} → ${trip.arrivalCity}")
                    println(s"   Date: ${trip.departureTime.format(dateFormatter)}")
                    rating.comment.foreach(c => println(s"   Commentaire: $c"))
                    println(s"   Noté le: ${rating.createdAt.format(dateFormatter)}")
                    println("---")
                  case _ =>
                    println("Erreur lors de la récupération du trajet")
                }
              case _ =>
                println("Erreur lors de la récupération de l'utilisateur")
            }
          }
        }
      case Failure(exception) =>
        println(s"Erreur: ${exception.getMessage}")
    }
  }
  
  private def viewMyRatingStats(): Unit = {
    println("\n=== Mes statistiques de notation ===")
    
    ratingService.getUserRatingStats(currentUser.id.get) match {
      case Success(stats) =>
        val average = stats("averageRating").asInstanceOf[Option[Double]]
        val total = stats("totalRatings").asInstanceOf[Int]
        val distribution = stats("ratingDistribution").asInstanceOf[Map[Int, Int]]
        
        println(s"📊 Note moyenne: ${average.map(_.formatted("%.2f")).getOrElse("Aucune note")}/5")
        println(s"📈 Nombre total de notes: $total")
        
        if (total > 0) {
          println("\n📋 Répartition des notes:")
          for (i <- 5 to 1 by -1) {
            val count = distribution.getOrElse(i, 0)
            val percentage = if (total > 0) (count * 100.0 / total).formatted("%.1f") else "0.0"
            val stars = "⭐" * i
            println(f"$stars ($i): $count%3d notes ($percentage%%)")
          }
        }
        
      case Failure(exception) =>
        println(s"Erreur: ${exception.getMessage}")
    }
  }
  
  private def viewEligibleTrips(): Unit = {
    println("\n=== Trajets éligibles pour notation ===")
    
    ratingService.getEligibleTripsForRating(currentUser.id.get) match {
      case Success(trips) =>
        if (trips.isEmpty) {
          println("Aucun trajet éligible pour notation")
        } else {
          trips.foreach { case (tripId, route, role, date) =>
            println(s"ID: $tripId")
            println(s"Trajet: $route")
            println(s"Votre rôle: $role")
            println(s"Date: ${date.format(dateFormatter)}")
            println("---")
          }
        }
      case Failure(exception) =>
        println(s"Erreur: ${exception.getMessage}")
    }
  }
}