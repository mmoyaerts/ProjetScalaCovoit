package services

import dao.{PaymentDAO, ReservationDAO, TripDAO, UserDAO}
import models.{Payment, Reservation}
import java.time.LocalDateTime
import scala.util.{Try, Success, Failure, Random}

class PaymentService {
  private val paymentDAO = new PaymentDAO()
  private val reservationDAO = new ReservationDAO()
  private val tripDAO = new TripDAO()
  private val userDAO = new UserDAO()
  
  def simulatePayment(reservationId: Long, payerId: Long): Try[Long] = {
    reservationDAO.findById(reservationId) match {
      case Success(Some(reservation)) =>
        if (reservation.passengerId != payerId) {
          Failure(new RuntimeException("Cette réservation ne vous appartient pas"))
        } else if (reservation.status != "CONFIRMED") {
          Failure(new RuntimeException("Cette réservation n'est pas confirmée"))
        } else {
          // Vérifier s'il n'y a pas déjà un paiement pour cette réservation
          paymentDAO.findByReservationId(reservationId) match {
            case Success(Some(_)) =>
              Failure(new RuntimeException("Un paiement a déjà été effectué pour cette réservation"))
            case Success(None) =>
              // Récupérer le conducteur
              tripDAO.findById(reservation.tripId) match {
                case Success(Some(trip)) =>
                  // Simuler le traitement du paiement
                  val isSuccessful = simulatePaymentProcessing()
                  
                  val payment = Payment(
                    reservationId = reservationId,
                    payerId = payerId,
                    receiverId = trip.driverId,
                    amount = reservation.totalPrice,
                    status = if (isSuccessful) "COMPLETED" else "FAILED",
                    paymentMethod = "SIMULATION"
                  )
                  
                  paymentDAO.create(payment) match {
                    case Success(paymentId) =>
                      if (isSuccessful) {
                        println(s"✅ Paiement simulé avec succès: ${reservation.totalPrice}€")
                        Success(paymentId)
                      } else {
                        println("❌ Échec de la simulation de paiement")
                        Failure(new RuntimeException("Échec du paiement simulé"))
                      }
                    case Failure(exception) => Failure(exception)
                  }
                case Success(None) =>
                  Failure(new RuntimeException("Trajet non trouvé"))
                case Failure(exception) =>
                  Failure(exception)
              }
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
  
  def getPaymentHistory(userId: Long): Try[List[Payment]] = {
    paymentDAO.findByUserId(userId)
  }
  
  def getPaymentsSent(userId: Long): Try[List[Payment]] = {
    paymentDAO.findByPayerId(userId)
  }
  
  def getPaymentsReceived(userId: Long): Try[List[Payment]] = {
    paymentDAO.findByReceiverId(userId)
  }
  
  def refundPayment(paymentId: Long, userId: Long): Try[Boolean] = {
    paymentDAO.findByUserId(userId).flatMap { payments =>
      payments.find(_.id.contains(paymentId)) match {
        case Some(payment) if payment.status == "COMPLETED" =>
          val refund = Payment(
            reservationId = payment.reservationId,
            payerId = payment.receiverId, // Le conducteur rembourse
            receiverId = payment.payerId,  // Au passager
            amount = payment.amount,
            status = "COMPLETED",
            paymentMethod = "REFUND_SIMULATION"
          )
          
          paymentDAO.create(refund).map { _ =>
            println(s"💰 Remboursement simulé: ${payment.amount}€")
            true
          }
        case Some(_) =>
          Failure(new RuntimeException("Ce paiement ne peut pas être remboursé"))
        case None =>
          Failure(new RuntimeException("Paiement non trouvé ou non autorisé"))
      }
    }
  }
  
  private def simulatePaymentProcessing(): Boolean = {
    // Simulation: 95% de chance de succès
    val random = new Random()
    Thread.sleep(1000) // Simuler le temps de traitement
    random.nextDouble() < 0.95
  }
  
  def getPaymentStats(userId: Long): Try[Map[String, BigDecimal]] = {
    for {
      sent <- getPaymentsSent(userId)
      received <- getPaymentsReceived(userId)
    } yield {
      val totalSent = sent.filter(_.status == "COMPLETED").map(_.amount).sum
      val totalReceived = received.filter(_.status == "COMPLETED").map(_.amount).sum
      val refunds = received.filter(_.paymentMethod == "REFUND_SIMULATION").map(_.amount).sum
      
      Map(
        "totalSent" -> totalSent,
        "totalReceived" -> totalReceived,
        "refunds" -> refunds,
        "balance" -> (totalReceived - totalSent + refunds)
      )
    }
  }
}