package services

import dao.VehicleDAO
import models.Vehicle
import scala.util.{Try, Success, Failure}

class VehicleService {
  private val vehicleDAO = new VehicleDAO()
  
  def addVehicle(userId: Long, brand: String, model: String, year: Int, licensePlate: String, seats: Int, color: String): Try[Long] = {
    // Vérifier si la plaque d'immatriculation existe déjà
    vehicleDAO.findByLicensePlate(licensePlate) match {
      case Success(Some(_)) =>
        Failure(new RuntimeException("Un véhicule avec cette plaque d'immatriculation existe déjà"))
      case Success(None) =>
        val vehicle = Vehicle(
          userId = userId,
          brand = brand,
          model = model,
          year = year,
          licensePlate = licensePlate,
          seats = seats,
          color = color
        )
        vehicleDAO.create(vehicle)
      case Failure(exception) =>
        Failure(exception)
    }
  }
  
  def getUserVehicles(userId: Long): Try[List[Vehicle]] = {
    vehicleDAO.findByUserId(userId)
  }
  
  def updateVehicle(vehicleId: Long, brand: String, model: String, year: Int, licensePlate: String, seats: Int, color: String): Try[Boolean] = {
    vehicleDAO.findById(vehicleId) match {
      case Success(Some(vehicle)) =>
        val updatedVehicle = vehicle.copy(
          brand = brand,
          model = model,
          year = year,
          licensePlate = licensePlate,
          seats = seats,
          color = color
        )
        vehicleDAO.update(updatedVehicle).map(_ > 0)
      case Success(None) =>
        Failure(new RuntimeException("Véhicule non trouvé"))
      case Failure(exception) =>
        Failure(exception)
    }
  }
  
  def deleteVehicle(vehicleId: Long): Try[Boolean] = {
    vehicleDAO.delete(vehicleId).map(_ > 0)
  }
  
  def getVehicleById(vehicleId: Long): Try[Option[Vehicle]] = {
    vehicleDAO.findById(vehicleId)
  }
}