package models

import java.time.LocalDateTime

case class Payment(
  id: Option[Long] = None,
  reservationId: Long,
  payerId: Long,
  receiverId: Long,
  amount: BigDecimal,
  status: String = "COMPLETED", // COMPLETED, PENDING, FAILED
  paymentMethod: String = "SIMULATION",
  transactionDate: LocalDateTime = LocalDateTime.now()
)