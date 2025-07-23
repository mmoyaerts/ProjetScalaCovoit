package dao

import models.Trip
import java.sql.ResultSet
import java.time.LocalDateTime
import scala.util.Try

class TripDAO extends BaseDAO {
  
  def create(trip: Trip): Try[Long] = {
    val sql = """
      INSERT INTO trips (driver_id, vehicle_id, departure_city, arrival_city, 
                        departure_time, arrival_time, available_seats, price_per_seat, 
                        description, is_active, created_at)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """
    val params = Seq(
      trip.driverId,
      trip.vehicleId,
      trip.departureCity,
      trip.arrivalCity,
      java.sql.Timestamp.valueOf(trip.departureTime),
      java.sql.Timestamp.valueOf(trip.arrivalTime),
      trip.availableSeats,
      trip.pricePerSeat.bigDecimal,
      trip.description.orNull,
      trip.isActive,
      java.sql.Timestamp.valueOf(trip.createdAt)
    )
    executeInsert(sql, params)
  }
  
  def findByDriverId(driverId: Long): Try[List[Trip]] = {
    val sql = "SELECT * FROM trips WHERE driver_id = ? ORDER BY departure_time ASC"
    executeQuery(sql, Seq(driverId))(mapResultSetToTrip)
  }
  
  def findUpcomingTripsByDriverId(driverId: Long): Try[List[Trip]] = {
    val sql = """
      SELECT * FROM trips 
      WHERE driver_id = ? AND departure_time > NOW() AND is_active = true 
      ORDER BY departure_time ASC
    """
    executeQuery(sql, Seq(driverId))(mapResultSetToTrip)
  }
  
  def findPastTripsByDriverId(driverId: Long): Try[List[Trip]] = {
    val sql = """
      SELECT * FROM trips 
      WHERE driver_id = ? AND departure_time <= NOW() 
      ORDER BY departure_time DESC
    """
    executeQuery(sql, Seq(driverId))(mapResultSetToTrip)
  }
  
  def findById(id: Long): Try[Option[Trip]] = {
    val sql = "SELECT * FROM trips WHERE id = ?"
    executeQuery(sql, Seq(id))(mapResultSetToTrip).map(_.headOption)
  }
  
  def update(trip: Trip): Try[Int] = {
    val sql = """
      UPDATE trips 
      SET vehicle_id = ?, departure_city = ?, arrival_city = ?, 
          departure_time = ?, arrival_time = ?, available_seats = ?, 
          price_per_seat = ?, description = ?
      WHERE id = ? AND is_active = true
    """
    val params = Seq(
      trip.vehicleId,
      trip.departureCity,
      trip.arrivalCity,
      java.sql.Timestamp.valueOf(trip.departureTime),
      java.sql.Timestamp.valueOf(trip.arrivalTime),
      trip.availableSeats,
      trip.pricePerSeat.bigDecimal,
      trip.description.orNull,
      trip.id.get
    )
    executeUpdate(sql, params)
  }
  
  def delete(tripId: Long): Try[Int] = {
    val sql = "UPDATE trips SET is_active = false WHERE id = ?"
    executeUpdate(sql, Seq(tripId))
  }
  
  def hasReservations(tripId: Long): Try[Boolean] = {
    val sql = "SELECT COUNT(*) as count FROM reservations WHERE trip_id = ? AND status = 'CONFIRMED'"
    executeQuery(sql, Seq(tripId)) { rs =>
      rs.getInt("count") > 0
    }.map(_.headOption.getOrElse(false))
  }
  
  def searchTrips(departureCity: String, arrivalCity: String, departureDate: LocalDateTime): Try[List[Trip]] = {
    val sql = """
      SELECT * FROM trips 
      WHERE departure_city ILIKE ? AND arrival_city ILIKE ? 
      AND DATE(departure_time) = DATE(?) AND is_active = true 
      AND departure_time > NOW() AND available_seats > 0
      ORDER BY departure_time ASC
    """
    val params = Seq(
      s"%$departureCity%",
      s"%$arrivalCity%",
      java.sql.Timestamp.valueOf(departureDate)
    )
    executeQuery(sql, params)(mapResultSetToTrip)
  }
  
  private def mapResultSetToTrip(rs: ResultSet): Trip = {
    Trip(
      id = Some(rs.getLong("id")),
      driverId = rs.getLong("driver_id"),
      vehicleId = rs.getLong("vehicle_id"),
      departureCity = rs.getString("departure_city"),
      arrivalCity = rs.getString("arrival_city"),
      departureTime = rs.getTimestamp("departure_time").toLocalDateTime,
      arrivalTime = rs.getTimestamp("arrival_time").toLocalDateTime,
      availableSeats = rs.getInt("available_seats"),
      pricePerSeat = BigDecimal(rs.getBigDecimal("price_per_seat")),
      description = Option(rs.getString("description")),
      isActive = rs.getBoolean("is_active"),
      createdAt = rs.getTimestamp("created_at").toLocalDateTime
    )
  }
}