package models

import java.time.LocalDateTime


case class User(
  id: Option[Long] = None,
  email: String,
  password: String,
  firstName: String,
  lastName: String,
  phone: String,
  averageRating: Option[Double] = None,
  createdAt: LocalDateTime = LocalDateTime.now(),
  isActive: Boolean = true
)