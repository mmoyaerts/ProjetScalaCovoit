package models

case class Vehicle(
  id: Option[Long] = None,
  userId: Long,
  brand: String,
  model: String,
  year: Int,
  licensePlate: String,
  seats: Int,
  color: String
)