package models

import java.time.LocalDateTime

case class Rating(
  id: Option[Long] = None,
  tripId: Long,
  raterId: Long,
  ratedId: Long,
  rating: Int, // 1 to 5
  comment: Option[String] = None,
  createdAt: LocalDateTime = LocalDateTime.now()
)