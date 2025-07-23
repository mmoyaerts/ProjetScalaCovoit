package dao

import models.Payment
import java.sql.ResultSet
import scala.util.Try

class PaymentDAO extends BaseDAO {
  
  def create(payment: Payment): Try[Long] = {
    val sql = """
      INSERT INTO payments (reservation_id, payer_id, receiver_id, amount, status, payment_method, transaction_date)
      VALUES (?, ?, ?, ?, ?, ?, ?)
    """
    val params = Seq(
      payment.reservationId,
      payment.payerId,
      payment.receiverId,
      payment.amount.bigDecimal,
      payment.status,
      payment.paymentMethod,
      java.sql.Timestamp.valueOf(payment.transactionDate)
    )
    executeInsert(sql, params)
  }
  
  def findByPayerId(payerId: Long): Try[List[Payment]] = {
    val sql = "SELECT * FROM payments WHERE payer_id = ? ORDER BY transaction_date DESC"
    executeQuery(sql, Seq(payerId))(mapResultSetToPayment)
  }
  
  def findByReceiverId(receiverId: Long): Try[List[Payment]] = {
    val sql = "SELECT * FROM payments WHERE receiver_id = ? ORDER BY transaction_date DESC"
    executeQuery(sql, Seq(receiverId))(mapResultSetToPayment)
  }
  
  def findByUserId(userId: Long): Try[List[Payment]] = {
    val sql = """
      SELECT * FROM payments 
      WHERE payer_id = ? OR receiver_id = ? 
      ORDER BY transaction_date DESC
    """
    executeQuery(sql, Seq(userId, userId))(mapResultSetToPayment)
  }
  
  def findByReservationId(reservationId: Long): Try[Option[Payment]] = {
    val sql = "SELECT * FROM payments WHERE reservation_id = ?"
    executeQuery(sql, Seq(reservationId))(mapResultSetToPayment).map(_.headOption)
  }
  
  def updateStatus(paymentId: Long, status: String): Try[Int] = {
    val sql = "UPDATE payments SET status = ? WHERE id = ?"
    executeUpdate(sql, Seq(status, paymentId))
  }
  
  private def mapResultSetToPayment(rs: ResultSet): Payment = {
    Payment(
      id = Some(rs.getLong("id")),
      reservationId = rs.getLong("reservation_id"),
      payerId = rs.getLong("payer_id"),
      receiverId = rs.getLong("receiver_id"),
      amount = BigDecimal(rs.getBigDecimal("amount")),
      status = rs.getString("status"),
      paymentMethod = rs.getString("payment_method"),
      transactionDate = rs.getTimestamp("transaction_date").toLocalDateTime
    )
  }
}