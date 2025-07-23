package dao

import models.Reservation
import java.sql.ResultSet
import java.time.LocalDateTime
import scala.util.Try

class ReservationDAO extends BaseDAO {
  
  def create(reservation: Reservation): Try[Long] = {
    val sql = """
      INSERT INTO reservations (trip_id, passenger_id, seats_reserved, total_price, status, created_at)
      VALUES (?, ?, ?, ?, ?, ?)
    """
    val params = Seq(
      reservation.tripId,
      reservation.passengerId,
      reservation.seatsReserved,
      reservation.totalPrice.bigDecimal,
      reservation.status,
      java.sql.Timestamp.valueOf(reservation.createdAt)
    )
    executeInsert(sql, params)
  }
  
  def findByPassengerId(passengerId: Long): Try[List[Reservation]] = {
    val sql = "SELECT * FROM reservations WHERE passenger_id = ? ORDER BY created_at DESC"
    executeQuery(sql, Seq(passengerId))(mapResultSetToReservation)
  }
  
  def findByTripId(tripId: Long): Try[List[Reservation]] = {
    val sql = "SELECT * FROM reservations WHERE trip_id = ? AND status = 'CONFIRMED'"
    executeQuery(sql, Seq(tripId))(mapResultSetToReservation)
  }
  
  def findById(id: Long): Try[Option[Reservation]] = {
    val sql = "SELECT * FROM reservations WHERE id = ?"
    executeQuery(sql, Seq(id))(mapResultSetToReservation).map(_.headOption)
  }
  
  def updateStatus(reservationId: Long, status: String): Try[Int] = {
    val sql = "UPDATE reservations SET status = ? WHERE id = ?"
    executeUpdate(sql, Seq(status, reservationId))
  }
  
  def findConfirmedByPassengerId(passengerId: Long): Try[List[Reservation]] = {
    val sql = "SELECT * FROM reservations WHERE passenger_id = ? AND status = 'CONFIRMED' ORDER BY created_at DESC"
    executeQuery(sql, Seq(passengerId))(mapResultSetToReservation)
  }
  
  def findByDriverId(driverId: Long): Try[List[Reservation]] = {
    val sql = """
      SELECT r.* FROM reservations r
      INNER JOIN trips t ON r.trip_id = t.id
      WHERE t.driver_id = ? AND r.status = 'CONFIRMED'
      ORDER BY r.created_at DESC
    """
    executeQuery(sql, Seq(driverId))(mapResultSetToReservation)
  }
  
  private def mapResultSetToReservation(rs: ResultSet): Reservation = {
    Reservation(
      id = Some(rs.getLong("id")),
      tripId = rs.getLong("trip_id"),
      passengerId = rs.getLong("passenger_id"),
      seatsReserved = rs.getInt("seats_reserved"),
      totalPrice = BigDecimal(rs.getBigDecimal("total_price")),
      status = rs.getString("status"),
      createdAt = rs.getTimestamp("created_at").toLocalDateTime
    )
  }
}