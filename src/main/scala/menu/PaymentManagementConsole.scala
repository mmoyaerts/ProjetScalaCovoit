package menu

import services.{PaymentService, ReservationService, TripService, UserService}
import models.{User, Payment, Reservation}
import java.time.format.DateTimeFormatter
import scala.io.StdIn
import scala.util.{Try, Success, Failure}

class PaymentManagementConsole(currentUser: User) {
  private val paymentService = new PaymentService()
  private val reservationService = new ReservationService()
  private val tripService = new TripService()
  private val userService = new UserService()
  private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
  
  def start(): Unit = {
    var managing = true
    
    while (managing) {
      showPaymentMenu()
      val choice = StdIn.readLine("Votre choix: ")
      
      choice match {
        case "1" => processPayment()
        case "2" => viewPaymentHistory()
        case "3" => viewPaymentsSent()
        case "4" => viewPaymentsReceived()
        case "5" => viewPaymentStats()
        case "6" => processRefund()
        case "7" => viewReservations()
        case "8" => managing = false
        case _ => println("Choix invalide")
      }
    }
  }
  
  private def showPaymentMenu(): Unit = {
    println("\n=== Gestion des paiements ===")
    println("1. Effectuer un paiement")
    println("2. Historique complet des paiements")
    println("3. Mes paiements envoyés")
    println("4. Mes paiements reçus")
    println("5. Statistiques de paiement")
    println("6. Effectuer un remboursement")
    println("7. Voir mes réservations")
    println("8. Retour")
  }
  
  private def processPayment(): Unit = {
    println("\n=== Effectuer un paiement ===")
    
    // Afficher les réservations non payées
    reservationService.getPassengerReservations(currentUser.id.get) match {
      case Success(reservations) =>
        val confirmedReservations = reservations.filter(_.status == "CONFIRMED")
        
        if (confirmedReservations.isEmpty) {
          println("Aucune réservation à payer")
          return
        }
        
        println("Vos réservations à payer:")
        confirmedReservations.foreach { reservation =>
          tripService.getTripById(reservation.tripId) match {
            case Success(Some(trip)) =>
              println(s"Réservation ID: ${reservation.id.get}")
              println(s"Trajet: ${trip.departureCity} → ${trip.arrivalCity}")
              println(s"Date: ${trip.departureTime.format(dateFormatter)}")
              println(s"Places: ${reservation.seatsReserved}")
              println(s"Montant: ${reservation.totalPrice}€")
              println("---")
            case _ =>
              println(s"Erreur lors de la récupération du trajet pour la réservation ${reservation.id.get}")
          }
        }
        
        print("ID de la réservation à payer: ")
        val reservationId = StdIn.readLong()
        StdIn.readLine() // consume newline
        
        // Vérifier que la réservation appartient à l'utilisateur
        if (confirmedReservations.exists(_.id.contains(reservationId))) {
          println("\n💳 Simulation du paiement en cours...")
          
          paymentService.simulatePayment(reservationId, currentUser.id.get) match {
            case Success(paymentId) =>
              println(s"✅ Paiement effectué avec succès ! ID: $paymentId")
            case Failure(exception) =>
              println(s"❌ Erreur lors du paiement: ${exception.getMessage}")
          }
        } else {
          println("Réservation invalide ou non trouvée")
        }
        
      case Failure(exception) =>
        println(s"Erreur lors de la récupération des réservations: ${exception.getMessage}")
    }
  }
  
  private def viewPaymentHistory(): Unit = {
    println("\n=== Historique complet des paiements ===")
    
    paymentService.getPaymentHistory(currentUser.id.get) match {
      case Success(payments) =>
        if (payments.isEmpty) {
          println("Aucun paiement trouvé")
        } else {
          payments.foreach { payment =>
            displayPaymentDetails(payment)
          }
        }
      case Failure(exception) =>
        println(s"Erreur: ${exception.getMessage}")
    }
  }
  
  private def viewPaymentsSent(): Unit = {
    println("\n=== Mes paiements envoyés ===")
    
    paymentService.getPaymentsSent(currentUser.id.get) match {
      case Success(payments) =>
        if (payments.isEmpty) {
          println("Aucun paiement envoyé")
        } else {
          payments.foreach { payment =>
            displayPaymentDetails(payment, showAs = "SENT")
          }
        }
      case Failure(exception) =>
        println(s"Erreur: ${exception.getMessage}")
    }
  }
  
  private def viewPaymentsReceived(): Unit = {
    println("\n=== Mes paiements reçus ===")
    
    paymentService.getPaymentsReceived(currentUser.id.get) match {
      case Success(payments) =>
        if (payments.isEmpty) {
          println("Aucun paiement reçu")
        } else {
          payments.foreach { payment =>
            displayPaymentDetails(payment, showAs = "RECEIVED")
          }
        }
      case Failure(exception) =>
        println(s"Erreur: ${exception.getMessage}")
    }
  }
  
  private def viewPaymentStats(): Unit = {
    println("\n=== Statistiques de paiement ===")
    
    paymentService.getPaymentStats(currentUser.id.get) match {
      case Success(stats) =>
        println(s"💸 Total envoyé: ${stats("totalSent")}€")
        println(s"💰 Total reçu: ${stats("totalReceived")}€")
        println(s"🔄 Remboursements reçus: ${stats("refunds")}€")
        println(s"📊 Solde net: ${stats("balance")}€")
        
        val balance = stats("balance")
        if (balance > 0) {
          println("✅ Vous avez un solde positif")
        } else if (balance < 0) {
          println("📉 Vous avez plus dépensé que reçu")
        } else {
          println("⚖️ Votre solde est équilibré")
        }
        
      case Failure(exception) =>
        println(s"Erreur: ${exception.getMessage}")
    }
  }
  
  private def processRefund(): Unit = {
    println("\n=== Effectuer un remboursement ===")
    
    paymentService.getPaymentsReceived(currentUser.id.get) match {
      case Success(payments) =>
        val refundablePayments = payments.filter(p => p.status == "COMPLETED" && p.paymentMethod != "REFUND_SIMULATION")
        
        if (refundablePayments.isEmpty) {
          println("Aucun paiement à rembourser")
          return
        }
        
        println("Paiements pouvant être remboursés:")
        refundablePayments.foreach { payment =>
          displayPaymentDetails(payment, showAs = "REFUNDABLE")
        }
        
        print("ID du paiement à rembourser: ")
        val paymentId = StdIn.readLong()
        StdIn.readLine() // consume newline
        
        print("Confirmer le remboursement (oui/non): ")
        val confirmation = StdIn.readLine().toLowerCase
        
        if (confirmation == "oui" || confirmation == "o") {
          println("\n💰 Simulation du remboursement en cours...")
          
          paymentService.refundPayment(paymentId, currentUser.id.get) match {
            case Success(true) =>
              println("✅ Remboursement effectué avec succès !")
            case Success(false) =>
              println("❌ Échec du remboursement")
            case Failure(exception) =>
              println(s"❌ Erreur lors du remboursement: ${exception.getMessage}")
          }
        } else {
          println("Remboursement annulé")
        }
        
      case Failure(exception) =>
        println(s"Erreur: ${exception.getMessage}")
    }
  }
  
  private def viewReservations(): Unit = {
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
                    println(s"Erreur lors de la récupération du conducteur")
                }
              case _ =>
                println(s"Erreur lors de la récupération du trajet")
            }
          }
        }
      case Failure(exception) =>
        println(s"Erreur: ${exception.getMessage}")
    }
  }
  
  private def displayPaymentDetails(payment: Payment, showAs: String = "FULL"): Unit = {
    println(s"Paiement ID: ${payment.id.get}")
    println(s"Montant: ${payment.amount}€")
    println(s"Statut: ${payment.status}")
    println(s"Méthode: ${payment.paymentMethod}")
    println(s"Date: ${payment.transactionDate.format(dateFormatter)}")
    
    showAs match {
      case "SENT" =>
        userService.getUserById(payment.receiverId) match {
          case Success(Some(receiver)) =>
            println(s"Payé à: ${receiver.firstName} ${receiver.lastName}")
          case _ =>
            println("Destinataire: Non trouvé")
        }
      case "RECEIVED" | "REFUNDABLE" =>
        userService.getUserById(payment.payerId) match {
          case Success(Some(payer)) =>
            println(s"Reçu de: ${payer.firstName} ${payer.lastName}")
          case _ =>
            println("Expéditeur: Non trouvé")
        }
      case "FULL" =>
        if (payment.payerId == currentUser.id.get) {
          userService.getUserById(payment.receiverId) match {
            case Success(Some(receiver)) =>
              println(s"→ Payé à: ${receiver.firstName} ${receiver.lastName}")
            case _ =>
              println("→ Destinataire: Non trouvé")
          }
        } else {
          userService.getUserById(payment.payerId) match {
            case Success(Some(payer)) =>
              println(s"← Reçu de: ${payer.firstName} ${payer.lastName}")
            case _ =>
              println("← Expéditeur: Non trouvé")
          }
        }
    }
    
    println("---")
  }
}