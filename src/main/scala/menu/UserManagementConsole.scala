package menu

import services.{UserService, VehicleService}
import models.{User, Vehicle}
import scala.io.StdIn
import scala.util.{Try, Success, Failure}

class UserManagementConsole {
  private val userService = new UserService()
  private val vehicleService = new VehicleService()
  private var currentUser: Option[User] = None
  
  def start(): Unit = {
    println("=== Bienvenue sur la plateforme de co-voiturage ===")
    
    var running = true
    while (running) {
      if (currentUser.isEmpty) {
        showMainMenu()
        val choice = StdIn.readLine("Votre choix: ")
        
        choice match {
          case "1" => register()
          case "2" => login()
          case "3" => 
            running = false
            println("Au revoir !")
          case _ => println("Choix invalide")
        }
      } else {
        showUserMenu()
        val choice = StdIn.readLine("Votre choix: ")
        
        choice match {
          case "1" => viewProfile()
          case "2" => updateProfile()
          case "3" => changePassword()
          case "4" => manageVehicles()
          case "5" => manageTrips()
          case "6" => manageReservations()
          case "7" => managePayments()
          case "8" => manageRatings()
          case "9" => manageMessages()
          case "10" => deleteAccount()
          case "11" => logout()
          case _ => println("Choix invalide")
        }
      }
    }
  }
  
  private def showMainMenu(): Unit = {
    println("\n=== Menu Principal ===")
    println("1. S'inscrire")
    println("2. Se connecter")
    println("3. Quitter")
  }
  
  private def showUserMenu(): Unit = {
    // Code existant mais avec le menu mis à jour
    println(s"\n=== Bienvenue ${currentUser.get.firstName} ===")
    println("1. Voir mon profil")
    println("2. Modifier mon profil")
    println("3. Changer mot de passe")
    println("4. Gérer mes véhicules")
    println("5. Gérer mes trajets")
    println("6. Gérer mes réservations")
    println("7. Gérer mes paiements")
    println("8. Système de notation")
    println("9. Messagerie")
    println("10. Supprimer mon compte")
    println("11. Se déconnecter")
  }
  
  private def manageTrips(): Unit = {
    val tripConsole = new TripManagementConsole(currentUser.get)
    tripConsole.start()
  }
  
  private def manageReservations(): Unit = {
    val reservationConsole = new ReservationManagementConsole(currentUser.get)
    reservationConsole.start()
  }
  
  private def managePayments(): Unit = {
    val paymentConsole = new PaymentManagementConsole(currentUser.get)
    paymentConsole.start()
  }
  
  private def register(): Unit = {
    println("\n=== Inscription ===")
    print("Email: ")
    val email = StdIn.readLine()
    print("Mot de passe: ")
    val password = StdIn.readLine()
    print("Prénom: ")
    val firstName = StdIn.readLine()
    print("Nom: ")
    val lastName = StdIn.readLine()
    print("Téléphone: ")
    val phone = StdIn.readLine()
    
    userService.register(email, password, firstName, lastName, phone) match {
      case Success(userId) =>
        println(s"Inscription réussie ! Votre ID utilisateur est: $userId")
      case Failure(exception) =>
        println(s"Erreur lors de l'inscription: ${exception.getMessage}")
    }
  }
  
  private def login(): Unit = {
    println("\n=== Connexion ===")
    print("Email: ")
    val email = StdIn.readLine()
    print("Mot de passe: ")
    val password = StdIn.readLine()
    
    userService.login(email, password) match {
      case Success(user) =>
        currentUser = Some(user)
        println(s"Connexion réussie ! Bienvenue ${user.firstName}")
      case Failure(exception) =>
        println(s"Erreur de connexion: ${exception.getMessage}")
    }
  }
  
  private def viewProfile(): Unit = {
    val user = currentUser.get
    println("\n=== Mon Profil ===")
    println(s"ID: ${user.id.get}")
    println(s"Email: ${user.email}")
    println(s"Nom: ${user.firstName} ${user.lastName}")
    println(s"Téléphone: ${user.phone}")
    
    // Affichage amélioré de la note avec étoiles
    user.averageRating match {
      case Some(rating) =>
        val stars = "⭐" * rating.round.toInt + "☆" * (5 - rating.round.toInt)
        println(s"Note moyenne: ${rating.formatted("%.2f")}/5 $stars")
      case None =>
        println("Note moyenne: Aucune note")
    }
    
    println(s"Membre depuis: ${user.createdAt}")
  }
  
  private def updateProfile(): Unit = {
    println("\n=== Modifier le profil ===")
    val user = currentUser.get
    
    print(s"Email (${user.email}): ")
    val email = StdIn.readLine() match {
      case "" => user.email
      case newEmail => newEmail
    }
    
    print(s"Prénom (${user.firstName}): ")
    val firstName = StdIn.readLine() match {
      case "" => user.firstName
      case newFirstName => newFirstName
    }
    
    print(s"Nom (${user.lastName}): ")
    val lastName = StdIn.readLine() match {
      case "" => user.lastName
      case newLastName => newLastName
    }
    
    print(s"Téléphone (${user.phone}): ")
    val phone = StdIn.readLine() match {
      case "" => user.phone
      case newPhone => newPhone
    }
    
    userService.updateProfile(user.id.get, email, firstName, lastName, phone) match {
      case Success(true) =>
        println("Profil mis à jour avec succès")
        // Recharger l'utilisateur
        userService.getUserById(user.id.get) match {
          case Success(Some(updatedUser)) => currentUser = Some(updatedUser)
          case _ => ()
        }
      case Success(false) =>
        println("Aucune modification effectuée")
      case Failure(exception) =>
        println(s"Erreur lors de la mise à jour: ${exception.getMessage}")
    }
  }
  
  private def changePassword(): Unit = {
    println("\n=== Changer le mot de passe ===")
    print("Ancien mot de passe: ")
    val oldPassword = StdIn.readLine()
    print("Nouveau mot de passe: ")
    val newPassword = StdIn.readLine()
    
    userService.changePassword(currentUser.get.id.get, oldPassword, newPassword) match {
      case Success(true) =>
        println("Mot de passe changé avec succès")
      case Success(false) =>
        println("Échec du changement de mot de passe")
      case Failure(exception) =>
        println(s"Erreur: ${exception.getMessage}")
    }
  }
  
  private def manageVehicles(): Unit = {
    var managing = true
    
    while (managing) {
      println("\n=== Gestion des véhicules ===")
      println("1. Voir mes véhicules")
      println("2. Ajouter un véhicule")
      println("3. Modifier un véhicule")
      println("4. Supprimer un véhicule")
      println("5. Retour")
      
      val choice = StdIn.readLine("Votre choix: ")
      
      choice match {
        case "1" => viewVehicles()
        case "2" => addVehicle()
        case "3" => updateVehicle()
        case "4" => deleteVehicle()
        case "5" => managing = false
        case _ => println("Choix invalide")
      }
    }
  }
  
  private def viewVehicles(): Unit = {
    vehicleService.getUserVehicles(currentUser.get.id.get) match {
      case Success(vehicles) =>
        if (vehicles.isEmpty) {
          println("Aucun véhicule enregistré")
        } else {
          println("\n=== Mes véhicules ===")
          vehicles.foreach { vehicle =>
            println(s"ID: ${vehicle.id.get}")
            println(s"${vehicle.brand} ${vehicle.model} (${vehicle.year})")
            println(s"Plaque: ${vehicle.licensePlate}")
            println(s"Places: ${vehicle.seats}, Couleur: ${vehicle.color}")
            println("---")
          }
        }
      case Failure(exception) =>
        println(s"Erreur: ${exception.getMessage}")
    }
  }
  
  private def addVehicle(): Unit = {
    println("\n=== Ajouter un véhicule ===")
    print("Marque: ")
    val brand = StdIn.readLine()
    print("Modèle: ")
    val model = StdIn.readLine()
    print("Année: ")
    val year = StdIn.readInt()
    StdIn.readLine() // consume newline
    print("Plaque d'immatriculation: ")
    val licensePlate = StdIn.readLine()
    print("Nombre de places: ")
    val seats = StdIn.readInt()
    StdIn.readLine() // consume newline
    print("Couleur: ")
    val color = StdIn.readLine()
    
    vehicleService.addVehicle(currentUser.get.id.get, brand, model, year, licensePlate, seats, color) match {
      case Success(vehicleId) =>
        println(s"Véhicule ajouté avec succès ! ID: $vehicleId")
      case Failure(exception) =>
        println(s"Erreur: ${exception.getMessage}")
    }
  }
  
  private def updateVehicle(): Unit = {
    viewVehicles()
    print("ID du véhicule à modifier: ")
    val vehicleId = StdIn.readLong()
    StdIn.readLine() // consume newline
    
    vehicleService.getVehicleById(vehicleId) match {
      case Success(Some(vehicle)) =>
        if (vehicle.userId == currentUser.get.id.get) {
          println(s"Modification du véhicule: ${vehicle.brand} ${vehicle.model}")
          
          print(s"Marque (${vehicle.brand}): ")
          val brand = StdIn.readLine() match {
            case "" => vehicle.brand
            case newBrand => newBrand
          }
          
          print(s"Modèle (${vehicle.model}): ")
          val model = StdIn.readLine() match {
            case "" => vehicle.model
            case newModel => newModel
          }
          
          print(s"Année (${vehicle.year}): ")
          val yearInput = StdIn.readLine()
          val year = if (yearInput.isEmpty) vehicle.year else yearInput.toInt
          
          print(s"Plaque (${vehicle.licensePlate}): ")
          val licensePlate = StdIn.readLine() match {
            case "" => vehicle.licensePlate
            case newPlate => newPlate
          }
          
          print(s"Places (${vehicle.seats}): ")
          val seatsInput = StdIn.readLine()
          val seats = if (seatsInput.isEmpty) vehicle.seats else seatsInput.toInt
          
          print(s"Couleur (${vehicle.color}): ")
          val color = StdIn.readLine() match {
            case "" => vehicle.color
            case newColor => newColor
          }
          
          vehicleService.updateVehicle(vehicleId, brand, model, year, licensePlate, seats, color) match {
            case Success(true) =>
              println("Véhicule mis à jour avec succès")
            case Success(false) =>
              println("Aucune modification effectuée")
            case Failure(exception) =>
              println(s"Erreur: ${exception.getMessage}")
          }
        } else {
          println("Ce véhicule ne vous appartient pas")
        }
      case Success(None) =>
        println("Véhicule non trouvé")
      case Failure(exception) =>
        println(s"Erreur: ${exception.getMessage}")
    }
  }
  
  private def deleteVehicle(): Unit = {
    viewVehicles()
    print("ID du véhicule à supprimer: ")
    val vehicleId = StdIn.readLong()
    StdIn.readLine() // consume newline
    
    vehicleService.getVehicleById(vehicleId) match {
      case Success(Some(vehicle)) =>
        if (vehicle.userId == currentUser.get.id.get) {
          print("Confirmer la suppression (oui/non): ")
          val confirmation = StdIn.readLine().toLowerCase
          
          if (confirmation == "oui" || confirmation == "o") {
            vehicleService.deleteVehicle(vehicleId) match {
              case Success(true) =>
                println("Véhicule supprimé avec succès")
              case Success(false) =>
                println("Échec de la suppression")
              case Failure(exception) =>
                println(s"Erreur: ${exception.getMessage}")
            }
          } else {
            println("Suppression annulée")
          }
        } else {
          println("Ce véhicule ne vous appartient pas")
        }
      case Success(None) =>
        println("Véhicule non trouvé")
      case Failure(exception) =>
        println(s"Erreur: ${exception.getMessage}")
    }
  }
  
  private def deleteAccount(): Unit = {
    println("\n=== Suppression du compte ===")
    println("ATTENTION: Cette action est irréversible !")
    print("Tapez 'SUPPRIMER' pour confirmer: ")
    val confirmation = StdIn.readLine()
    
    if (confirmation == "SUPPRIMER") {
      userService.deleteAccount(currentUser.get.id.get) match {
        case Success(true) =>
          println("Compte supprimé avec succès")
          currentUser = None
        case Success(false) =>
          println("Échec de la suppression")
        case Failure(exception) =>
          println(s"Erreur: ${exception.getMessage}")
      }
    } else {
      println("Suppression annulée")
    }
  }
  
  private def logout(): Unit = {
    currentUser = None
    println("Déconnexion réussie")
  }
  
  private def manageRatings(): Unit = {
    val ratingConsole = new RatingManagementConsole(currentUser.get)
    ratingConsole.start()
  }
  
  private def manageMessages(): Unit = {
    val messageConsole = new MessageManagementConsole(currentUser.get)
    messageConsole.start()
  }
}