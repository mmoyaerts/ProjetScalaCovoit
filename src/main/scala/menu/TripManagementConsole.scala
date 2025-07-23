package menu

import services.{TripService, VehicleService, UserService}
import models.{User, Trip, Vehicle}
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import scala.io.StdIn
import scala.util.{Try, Success, Failure}

class TripManagementConsole(currentUser: User) {
  private val tripService = new TripService()
  private val vehicleService = new VehicleService()
  private val userService = new UserService()
  private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
  
  def start(): Unit = {
    var managing = true
    
    while (managing) {
      showTripMenu()
      val choice = StdIn.readLine("Votre choix: ")
      
      choice match {
        case "1" => createTrip()
        case "2" => viewMyTrips()
        case "3" => viewUpcomingTrips()
        case "4" => viewPastTrips()
        case "5" => updateTrip()
        case "6" => deleteTrip()
        case "7" => searchTrips()
        case "8" => managing = false
        case _ => println("Choix invalide")
      }
    }
  }
  
  private def showTripMenu(): Unit = {
    println("\n=== Gestion des trajets ===")
    println("1. Proposer un trajet")
    println("2. Voir tous mes trajets")
    println("3. Voir mes trajets à venir")
    println("4. Voir mes trajets passés")
    println("5. Modifier un trajet")
    println("6. Supprimer un trajet")
    println("7. Rechercher des trajets")
    println("8. Retour")
  }
  
  private def createTrip(): Unit = {
    println("\n=== Proposer un trajet ===")
    
    // Afficher les véhicules de l'utilisateur
    vehicleService.getUserVehicles(currentUser.id.get) match {
      case Success(vehicles) =>
        if (vehicles.isEmpty) {
          println("Vous devez d'abord ajouter un véhicule pour proposer un trajet")
          return
        }
        
        println("Vos véhicules:")
        vehicles.foreach { vehicle =>
          println(s"${vehicle.id.get}. ${vehicle.brand} ${vehicle.model} (${vehicle.seats} places)")
        }
        
        print("Choisissez un véhicule (ID): ")
        val vehicleId = StdIn.readLong()
        StdIn.readLine() // consume newline
        
        // Vérifier que le véhicule existe et appartient à l'utilisateur
        val selectedVehicle = vehicles.find(_.id.contains(vehicleId))
        if (selectedVehicle.isEmpty) {
          println("Véhicule invalide")
          return
        }
        
        print("Ville de départ: ")
        val departureCity = StdIn.readLine()
        
        print("Ville d'arrivée: ")
        val arrivalCity = StdIn.readLine()
        
        print("Date et heure de départ (dd/MM/yyyy HH:mm): ")
        val departureTimeStr = StdIn.readLine()
        
        print("Date et heure d'arrivée (dd/MM/yyyy HH:mm): ")
        val arrivalTimeStr = StdIn.readLine()
        
        print(s"Nombre de places disponibles (max ${selectedVehicle.get.seats}): ")
        val availableSeats = StdIn.readInt()
        StdIn.readLine() // consume newline
        
        print("Prix par place (€): ")
        val pricePerSeat = BigDecimal(StdIn.readDouble())
        StdIn.readLine() // consume newline
        
        print("Description (optionnel): ")
        val descriptionInput = StdIn.readLine()
        val description = if (descriptionInput.trim.isEmpty) None else Some(descriptionInput)
        
        try {
          val departureTime = LocalDateTime.parse(departureTimeStr, dateFormatter)
          val arrivalTime = LocalDateTime.parse(arrivalTimeStr, dateFormatter)
          
          tripService.createTrip(
            currentUser.id.get,
            vehicleId,
            departureCity,
            arrivalCity,
            departureTime,
            arrivalTime,
            availableSeats,
            pricePerSeat,
            description
          ) match {
            case Success(tripId) =>
              println(s"Trajet créé avec succès ! ID: $tripId")
            case Failure(exception) =>
              println(s"Erreur: ${exception.getMessage}")
          }
        } catch {
          case _: Exception =>
            println("Format de date invalide. Utilisez le format dd/MM/yyyy HH:mm")
        }
        
      case Failure(exception) =>
        println(s"Erreur lors de la récupération des véhicules: ${exception.getMessage}")
    }
  }
  
  private def viewMyTrips(): Unit = {
    tripService.getDriverTrips(currentUser.id.get) match {
      case Success(trips) =>
        if (trips.isEmpty) {
          println("Aucun trajet trouvé")
        } else {
          println("\n=== Tous mes trajets ===")
          displayTrips(trips)
        }
      case Failure(exception) =>
        println(s"Erreur: ${exception.getMessage}")
    }
  }
  
  private def viewUpcomingTrips(): Unit = {
    tripService.getDriverUpcomingTrips(currentUser.id.get) match {
      case Success(trips) =>
        if (trips.isEmpty) {
          println("Aucun trajet à venir")
        } else {
          println("\n=== Mes trajets à venir ===")
          displayTrips(trips)
        }
      case Failure(exception) =>
        println(s"Erreur: ${exception.getMessage}")
    }
  }
  
  private def viewPastTrips(): Unit = {
    tripService.getDriverPastTrips(currentUser.id.get) match {
      case Success(trips) =>
        if (trips.isEmpty) {
          println("Aucun trajet passé")
        } else {
          println("\n=== Mes trajets passés ===")
          displayTrips(trips)
        }
      case Failure(exception) =>
        println(s"Erreur: ${exception.getMessage}")
    }
  }
  
  private def displayTrips(trips: List[Trip]): Unit = {
    trips.foreach { trip =>
      println(s"ID: ${trip.id.get}")
      println(s"${trip.departureCity} → ${trip.arrivalCity}")
      println(s"Départ: ${trip.departureTime.format(dateFormatter)}")
      println(s"Arrivée: ${trip.arrivalTime.format(dateFormatter)}")
      println(s"Places disponibles: ${trip.availableSeats}")
      println(s"Prix par place: ${trip.pricePerSeat}€")
      trip.description.foreach(desc => println(s"Description: $desc"))
      println(s"Statut: ${if (trip.isActive) "Actif" else "Inactif"}")
      println("---")
    }
  }
  
  private def updateTrip(): Unit = {
    viewUpcomingTrips()
    print("ID du trajet à modifier: ")
    val tripId = StdIn.readLong()
    StdIn.readLine() // consume newline
    
    tripService.getTripById(tripId) match {
      case Success(Some(trip)) =>
        if (trip.driverId == currentUser.id.get) {
          println(s"Modification du trajet: ${trip.departureCity} → ${trip.arrivalCity}")
          
          // Afficher les véhicules
          vehicleService.getUserVehicles(currentUser.id.get) match {
            case Success(vehicles) =>
              if (vehicles.isEmpty) {
                println("Aucun véhicule disponible")
                return
              }
              
              println("Vos véhicules:")
              vehicles.foreach { vehicle =>
                println(s"${vehicle.id.get}. ${vehicle.brand} ${vehicle.model}")
              }
              
              print(s"Véhicule (${trip.vehicleId}): ")
              val vehicleInput = StdIn.readLine()
              val vehicleId = if (vehicleInput.isEmpty) trip.vehicleId else vehicleInput.toLong
              
              print(s"Ville de départ (${trip.departureCity}): ")
              val departureCity = StdIn.readLine() match {
                case "" => trip.departureCity
                case newCity => newCity
              }
              
              print(s"Ville d'arrivée (${trip.arrivalCity}): ")
              val arrivalCity = StdIn.readLine() match {
                case "" => trip.arrivalCity
                case newCity => newCity
              }
              
              print(s"Date et heure de départ (${trip.departureTime.format(dateFormatter)}): ")
              val departureTimeInput = StdIn.readLine()
              val departureTime = if (departureTimeInput.isEmpty) {
                trip.departureTime
              } else {
                LocalDateTime.parse(departureTimeInput, dateFormatter)
              }
              
              print(s"Date et heure d'arrivée (${trip.arrivalTime.format(dateFormatter)}): ")
              val arrivalTimeInput = StdIn.readLine()
              val arrivalTime = if (arrivalTimeInput.isEmpty) {
                trip.arrivalTime
              } else {
                LocalDateTime.parse(arrivalTimeInput, dateFormatter)
              }
              
              print(s"Places disponibles (${trip.availableSeats}): ")
              val seatsInput = StdIn.readLine()
              val availableSeats = if (seatsInput.isEmpty) trip.availableSeats else seatsInput.toInt
              
              print(s"Prix par place (${trip.pricePerSeat}€): ")
              val priceInput = StdIn.readLine()
              val pricePerSeat = if (priceInput.isEmpty) trip.pricePerSeat else BigDecimal(priceInput.toDouble)
              
              print(s"Description (${trip.description.getOrElse("Aucune")}): ")
              val descriptionInput = StdIn.readLine()
              val description = if (descriptionInput.isEmpty) trip.description else Some(descriptionInput)
              
              try {
                tripService.updateTrip(
                  tripId,
                  currentUser.id.get,
                  vehicleId,
                  departureCity,
                  arrivalCity,
                  departureTime,
                  arrivalTime,
                  availableSeats,
                  pricePerSeat,
                  description
                ) match {
                  case Success(true) =>
                    println("Trajet mis à jour avec succès")
                  case Success(false) =>
                    println("Aucune modification effectuée")
                  case Failure(exception) =>
                    println(s"Erreur: ${exception.getMessage}")
                }
              } catch {
                case _: Exception =>
                  println("Format de date invalide")
              }
              
            case Failure(exception) =>
              println(s"Erreur: ${exception.getMessage}")
          }
        } else {
          println("Ce trajet ne vous appartient pas")
        }
      case Success(None) =>
        println("Trajet non trouvé")
      case Failure(exception) =>
        println(s"Erreur: ${exception.getMessage}")
    }
  }
  
  private def deleteTrip(): Unit = {
    viewUpcomingTrips()
    print("ID du trajet à supprimer: ")
    val tripId = StdIn.readLong()
    StdIn.readLine() // consume newline
    
    tripService.getTripById(tripId) match {
      case Success(Some(trip)) =>
        if (trip.driverId == currentUser.id.get) {
          print("Confirmer la suppression (oui/non): ")
          val confirmation = StdIn.readLine().toLowerCase
          
          if (confirmation == "oui" || confirmation == "o") {
            tripService.deleteTrip(tripId, currentUser.id.get) match {
              case Success(true) =>
                println("Trajet supprimé avec succès")
              case Success(false) =>
                println("Échec de la suppression")
              case Failure(exception) =>
                println(s"Erreur: ${exception.getMessage}")
            }
          } else {
            println("Suppression annulée")
          }
        } else {
          println("Ce trajet ne vous appartient pas")
        }
      case Success(None) =>
        println("Trajet non trouvé")
      case Failure(exception) =>
        println(s"Erreur: ${exception.getMessage}")
    }
  }
  
  private def searchTrips(): Unit = {
    println("\n=== Rechercher des trajets ===")
    print("Ville de départ: ")
    val departureCity = StdIn.readLine()
    
    print("Ville d'arrivée: ")
    val arrivalCity = StdIn.readLine()
    
    print("Date de départ (dd/MM/yyyy): ")
    val dateStr = StdIn.readLine()
    
    try {
      val departureDate = LocalDateTime.parse(s"$dateStr 00:00", dateFormatter)
      
      tripService.searchTrips(departureCity, arrivalCity, departureDate) match {
        case Success(trips) =>
          if (trips.isEmpty) {
            println("Aucun trajet trouvé pour ces critères")
          } else {
            println(s"\n=== ${trips.length} trajet(s) trouvé(s) ===")
            trips.foreach { trip =>
              // Récupérer les informations du conducteur
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
                  println(s"Erreur lors de la récupération des informations du conducteur pour le trajet ${trip.id.get}")
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
}