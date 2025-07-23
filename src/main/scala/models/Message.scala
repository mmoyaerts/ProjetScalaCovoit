package models

import java.time.LocalDateTime

case class Message(
  id: Option[Long] = None,
  senderId: Long,
  receiverId: Long,
  content: String,
  sentAt: LocalDateTime = LocalDateTime.now()
)