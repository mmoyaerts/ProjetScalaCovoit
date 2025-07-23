package menu

import services.{ReservationService, TripService, UserService}
import models.{User, Trip}
import java.time.format.DateTimeFormatter
import scala.io.StdIn
import scala.util.{Try, Success, Failure}

class ReservationManagementConsole(currentUser: User) {
  private val reservationService = new ReservationService()
  private val tripService = new TripService()
  private val userService = new UserService()
  private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
  
  def start(): Unit = {
    var managing = true
    
    while (managing) {
      showReservationMenu()
      val choice = StdIn.readLine("Votre choix: ")
      
      choice match {
        case "1" => searchAndReserveTrip()
        case "2" => viewMyReservations()
        case "3" => cancelReservation()
        case "4" => viewDriverReservations()
        case "5" => managing = false
        case _ => println("Choix invalide")
      }
    }
  }
  
  private def showReservationMenu(): Unit = {
    println("\n=== Gestion des réservations ===")
    println("1. Rechercher et réserver un trajet")
    println("2. Voir mes réservations (passager)")
    println("3. Annuler une réservation")
    println("4. Voir les réservations de mes trajets (conducteur)")
    println("5. Retour")
  }
  
  private def searchAndReserveTrip(): Unit = {
    val tripConsole = new TripManagementConsole(currentUser)
    
    println("\n=== Rechercher un trajet ===")
    print("Ville de départ: ")
    val departureCity = StdIn.readLine()
    
    print("Ville d'arrivée: ")
    val arrivalCity = StdIn.readLine()
    
    print("Date de départ (dd/MM/yyyy): ")
    val dateStr = StdIn.readLine()
    
    try {
      val departureDate = java.time.LocalDateTime.parse(s"$dateStr 00:00", dateFormatter)
      
      tripService.searchTrips(departureCity, arrivalCity, departureDate) match {
        case Success(trips) =>
          if (trips.isEmpty) {
            println("Aucun trajet trouvé pour ces critères")
          } else {
            println(s"\n=== ${trips.length} trajet(s) trouvé(s) ===")
            trips.foreach { trip =>
              userService.getUserById(trip.driverId) match {
                case Success(Some(driver)) =>
                  println(s"ID: ${trip.id.get}")
                  println(s"Conducteur: ${driver.firstName} ${driver.lastName}")
                  println(s"Note: ${driver.averageRating.getOrElse("Aucune note")}")
                  println(s"${trip.departureCity} → ${trip.arrivalCity}")
                  println(s"Départ: ${trip.departureTime.format(dateFormatter)}")
                  println(s"Arrivée: ${trip.arrivalTime.format(dateFormatter)}")
                  println(s"Places disponibles: ${trip.availableSeats}")
                  println(s"Prix par place: ${trip.pricePerSeat}€")
                  trip.description.foreach(desc => println(s"Description: $desc"))
                  println("---")
                case _ =>
                  println(s"Erreur lors de la récupération des informations du conducteur")
              }
            }
            
            // Permettre de faire une réservation
            print("\nSouhaitez-vous réserver un trajet ? (oui/non): ")
            val wantToReserve = StdIn.readLine().toLowerCase
            
            if (wantToReserve == "oui" || wantToReserve == "o") {
              print("ID du trajet à réserver: ")
              val tripId = StdIn.readLong()
              StdIn.readLine() // consume newline
              
              print("Nombre de places à réserver: ")
              val seatsRequested = StdIn.readInt()
              StdIn.readLine() // consume newline
              
              reservationService.makeReservation(tripId, currentUser.id.get, seatsRequested) match {
                case Success(reservationId) =>
                  println(s"✅ Réservation effectuée avec succès ! ID: $reservationId")
                  println("N'oubliez pas d'effectuer le paiement dans la section paiements")
                case Failure(exception) =>
                  println(s"❌ Erreur lors de la réservation: ${exception.getMessage}")
              }
            }
          }
        case Failure(exception) =>
          println(s"Erreur: ${exception.getMessage}")
      }
    } catch {
      case _: Exception =>
        println("Format de date invalide. Utilisez le format dd/MM/yyyy")
    }
  }
  
  private def viewMyReservations(): Unit = {
    println("\n=== Mes réservations ===")
    
    reservationService.getPassengerReservations(currentUser.id.get) match {
      case Success(reservations) =>
        if (reservations.isEmpty) {
          println("Aucune réservation trouvée")
        } else {
          reservations.foreach { reservation =>
            tripService.getTripById(reservation.tripId) match {
              case Success(Some(trip)) =>
                userService.getUserById(trip.driverId) match {
                  case Success(Some(driver)) =>
                    println(s"Réservation ID: ${reservation.id.get}")
                    println(s"Trajet: ${trip.departureCity} → ${trip.arrivalCity}")
                    println(s"Conducteur: ${driver.firstName} ${driver.lastName}")
                    println(s"Date: ${trip.departureTime.format(dateFormatter)}")
                    println(s"Places réservées: ${reservation.seatsReserved}")
                    println(s"Prix total: ${reservation.totalPrice}€")
                    println(s"Statut: ${reservation.status}")
                    println(s"Réservé le: ${reservation.createdAt.format(dateFormatter)}")
                    println("---")
                  case _ =>
                    println("Erreur lors de la récupération du conducteur")
                }
              case _ =>
                println("Erreur lors de la récupération du trajet")
            }
          }
        }
      case Failure(exception) =>
        println(s"Erreur: ${exception.getMessage}")
    }
  }
  
  private def cancelReservation(): Unit = {
    viewMyReservations()
    
    print("ID de la réservation à annuler: ")
    val reservationId = StdIn.readLong()
    StdIn.readLine() // consume newline
    
    print("Confirmer l'annulation (oui/non): ")
    val confirmation = StdIn.readLine().toLowerCase
    
    if (confirmation == "oui" || confirmation == "o") {
      reservationService.cancelReservation(reservationId, currentUser.id.get) match {
        case Success(true) =>
          println("✅ Réservation annulée avec succès")
        case Success(false) =>
          println("❌ Échec de l'annulation")
        case Failure(exception) =>
          println(s"❌ Erreur: ${exception.getMessage}")
      }
    } else {
      println("Annulation annulée")
    }
  }
  
  private def viewDriverReservations(): Unit = {
    println("\n=== Réservations pour mes trajets ===")
    
    reservationService.getDriverReservations(currentUser.id.get) match {
      case Success(reservations) =>
        if (reservations.isEmpty) {
          println("Aucune réservation pour vos trajets")
        } else {
          reservations.foreach { reservation =>
            tripService.getTripById(reservation.tripId) match {
              case Success(Some(trip)) =>
                userService.getUserById(reservation.passengerId) match {
                  case Success(Some(passenger)) =>
                    println(s"Réservation ID: ${reservation.id.get}")
                    println(s"Trajet: ${trip.departureCity} → ${trip.arrivalCity}")
                    println(s"Passager: ${passenger.firstName} ${passenger.lastName}")
                    println(s"Téléphone: ${passenger.phone}")
                    println(s"Date trajet: ${trip.departureTime.format(dateFormatter)}")
                    println(s"Places réservées: ${reservation.seatsReserved}")
                    println(s"Prix total: ${reservation.totalPrice}€")
                    println(s"Statut: ${reservation.status}")
                    println(s"Réservé le: ${reservation.createdAt.format(dateFormatter)}")
                    println("---")
                  case _ =>
                    println("Erreur lors de la récupération du passager")
                }
              case _ =>
                println("Erreur lors de la récupération du trajet")
            }
          }
        }
      case Failure(exception) =>
        println(s"Erreur: ${exception.getMessage}")
    }
  }
}