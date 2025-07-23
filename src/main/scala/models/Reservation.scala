package models

import java.time.LocalDateTime

case class Reservation(
  id: Option[Long] = None,
  tripId: Long,
  passengerId: Long,
  seatsReserved: Int,
  totalPrice: BigDecimal,
  status: String = "CONFIRMED", // CONFIRMED, CANCELLED
  createdAt: LocalDateTime = LocalDateTime.now()
)