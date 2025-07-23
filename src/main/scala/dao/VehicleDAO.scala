package dao

import models.Vehicle
import java.sql.ResultSet
import scala.util.Try

class VehicleDAO extends BaseDAO {
  
  def create(vehicle: Vehicle): Try[Long] = {
    val sql = """
      INSERT INTO vehicles (user_id, brand, model, year, license_plate, seats, color)
      VALUES (?, ?, ?, ?, ?, ?, ?)
    """
    val params = Seq(
      vehicle.userId,
      vehicle.brand,
      vehicle.model,
      vehicle.year,
      vehicle.licensePlate,
      vehicle.seats,
      vehicle.color
    )
    executeInsert(sql, params)
  }
  
  def findByUserId(userId: Long): Try[List[Vehicle]] = {
    val sql = "SELECT * FROM vehicles WHERE user_id = ?"
    executeQuery(sql, Seq(userId))(mapResultSetToVehicle)
  }
  
  def findById(id: Long): Try[Option[Vehicle]] = {
    val sql = "SELECT * FROM vehicles WHERE id = ?"
    executeQuery(sql, Seq(id))(mapResultSetToVehicle).map(_.headOption)
  }
  
  def update(vehicle: Vehicle): Try[Int] = {
    val sql = """
      UPDATE vehicles 
      SET brand = ?, model = ?, year = ?, license_plate = ?, seats = ?, color = ?
      WHERE id = ?
    """
    val params = Seq(
      vehicle.brand,
      vehicle.model,
      vehicle.year,
      vehicle.licensePlate,
      vehicle.seats,
      vehicle.color,
      vehicle.id.get
    )
    executeUpdate(sql, params)
  }
  
  def delete(vehicleId: Long): Try[Int] = {
    val sql = "DELETE FROM vehicles WHERE id = ?"
    executeUpdate(sql, Seq(vehicleId))
  }
  
  def findByLicensePlate(licensePlate: String): Try[Option[Vehicle]] = {
    val sql = "SELECT * FROM vehicles WHERE license_plate = ?"
    executeQuery(sql, Seq(licensePlate))(mapResultSetToVehicle).map(_.headOption)
  }
  
  private def mapResultSetToVehicle(rs: ResultSet): Vehicle = {
    Vehicle(
      id = Some(rs.getLong("id")),
      userId = rs.getLong("user_id"),
      brand = rs.getString("brand"),
      model = rs.getString("model"),
      year = rs.getInt("year"),
      licensePlate = rs.getString("license_plate"),
      seats = rs.getInt("seats"),
      color = rs.getString("color")
    )
  }
}