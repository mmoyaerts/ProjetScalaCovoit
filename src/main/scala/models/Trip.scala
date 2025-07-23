package models

import java.time.LocalDateTime

case class Trip(
  id: Option[Long] = None,
  driverId: Long,
  vehicleId: Long,
  departureCity: String,
  arrivalCity: String,
  departureTime: LocalDateTime,
  arrivalTime: LocalDateTime,
  availableSeats: Int,
  pricePerSeat: BigDecimal, // Prix evolutif en fonction de l'utilisateur
  description: Option[String] = None,
  isActive: Boolean = true,
  createdAt: LocalDateTime = LocalDateTime.now()
)