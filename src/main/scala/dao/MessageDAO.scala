package dao

import models.Message
import java.sql.ResultSet
import scala.util.Try

class MessageDAO extends BaseDAO {
  
  def create(message: Message): Try[Long] = {
    val sql = """
      INSERT INTO messages (sender_id, receiver_id, content, sent_at)
      VALUES (?, ?, ?, ?)
    """
    val params = Seq(
      message.senderId,
      message.receiverId,
      message.content,
      java.sql.Timestamp.valueOf(message.sentAt)
    )
    executeInsert(sql, params)
  }
  
  def findByReceiverId(receiverId: Long): Try[List[Message]] = {
    val sql = "SELECT * FROM messages WHERE receiver_id = ? ORDER BY sent_at DESC"
    executeQuery(sql, Seq(receiverId))(mapResultSetToMessage)
  }
  
  def findBySenderId(senderId: Long): Try[List[Message]] = {
    val sql = "SELECT * FROM messages WHERE sender_id = ? ORDER BY sent_at DESC"
    executeQuery(sql, Seq(senderId))(mapResultSetToMessage)
  }
  
  def findConversation(userId1: Long, userId2: Long): Try[List[Message]] = {
    val sql = """
      SELECT * FROM messages 
      WHERE (sender_id = ? AND receiver_id = ?) 
         OR (sender_id = ? AND receiver_id = ?)
      ORDER BY sent_at ASC
    """
    executeQuery(sql, Seq(userId1, userId2, userId2, userId1))(mapResultSetToMessage)
  }
  
  def findAllConversations(userId: Long): Try[List[(Long, String, String, java.time.LocalDateTime, String)]] = {
    val sql = """
      SELECT DISTINCT 
        CASE 
          WHEN m.sender_id = ? THEN m.receiver_id 
          ELSE m.sender_id 
        END as other_user_id,
        u.first_name,
        u.last_name,
        m.sent_at,
        m.content
      FROM messages m
      JOIN users u ON (
        CASE 
          WHEN m.sender_id = ? THEN m.receiver_id = u.id
          ELSE m.sender_id = u.id
        END
      )
      WHERE m.sender_id = ? OR m.receiver_id = ?
      AND m.sent_at = (
        SELECT MAX(m2.sent_at)
        FROM messages m2
        WHERE (m2.sender_id = ? AND m2.receiver_id = (
          CASE 
            WHEN m.sender_id = ? THEN m.receiver_id 
            ELSE m.sender_id 
          END
        )) OR (m2.receiver_id = ? AND m2.sender_id = (
          CASE 
            WHEN m.sender_id = ? THEN m.receiver_id 
            ELSE m.sender_id 
          END
        ))
      )
      ORDER BY m.sent_at DESC
    """
    executeQuery(sql, Seq(userId, userId, userId, userId, userId, userId, userId, userId)) { rs =>
      (
        rs.getLong("other_user_id"),
        rs.getString("first_name"),
        rs.getString("last_name"),
        rs.getTimestamp("sent_at").toLocalDateTime,
        rs.getString("content")
      )
    }
  }
  
  def getUnreadMessageCount(userId: Long): Try[Int] = {
    val sql = "SELECT COUNT(*) as count FROM messages WHERE receiver_id = ? AND is_read = false"
    executeQuery(sql, Seq(userId)) { rs =>
      rs.getInt("count")
    }.map(_.headOption.getOrElse(0))
  }
  
  def markAsRead(senderId: Long, receiverId: Long): Try[Int] = {
    val sql = "UPDATE messages SET is_read = true WHERE sender_id = ? AND receiver_id = ? AND is_read = false"
    executeUpdate(sql, Seq(senderId, receiverId))
  }
  
  def deleteMessage(messageId: Long, userId: Long): Try[Int] = {
    val sql = "DELETE FROM messages WHERE id = ? AND (sender_id = ? OR receiver_id = ?)"
    executeUpdate(sql, Seq(messageId, userId, userId))
  }
  
  def searchMessages(userId: Long, searchTerm: String): Try[List[Message]] = {
    val sql = """
      SELECT * FROM messages 
      WHERE (sender_id = ? OR receiver_id = ?) 
        AND LOWER(content) LIKE LOWER(?)
      ORDER BY sent_at DESC
    """
    executeQuery(sql, Seq(userId, userId, s"%$searchTerm%"))(mapResultSetToMessage)
  }
  
  private def mapResultSetToMessage(rs: ResultSet): Message = {
    Message(
      id = Some(rs.getLong("id")),
      senderId = rs.getLong("sender_id"),
      receiverId = rs.getLong("receiver_id"),
      content = rs.getString("content"),
      sentAt = rs.getTimestamp("sent_at").toLocalDateTime
    )
  }
}