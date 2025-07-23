package services

import dao.{MessageDAO, UserDAO}
import models.Message
import java.time.LocalDateTime
import scala.util.{Try, Success, Failure}

class MessageService {
  private val messageDAO = new MessageDAO()
  private val userDAO = new UserDAO()
  
  def sendMessage(senderId: Long, receiverId: Long, content: String): Try[Long] = {
    // Validation du contenu
    if (content.trim.isEmpty) {
      return Failure(new RuntimeException("Le message ne peut pas être vide"))
    }
    
    if (content.length > 1000) {
      return Failure(new RuntimeException("Le message ne peut pas dépasser 1000 caractères"))
    }
    
    if (senderId == receiverId) {
      return Failure(new RuntimeException("Vous ne pouvez pas vous envoyer un message à vous-même"))
    }
    
    // Vérifier que le destinataire existe
    userDAO.findById(receiverId) match {
      case Success(Some(_)) =>
        val message = Message(
          senderId = senderId,
          receiverId = receiverId,
          content = content.trim
        )
        messageDAO.create(message)
      case Success(None) =>
        Failure(new RuntimeException("Destinataire non trouvé"))
      case Failure(exception) =>
        Failure(exception)
    }
  }
  
  def getReceivedMessages(userId: Long): Try[List[Message]] = {
    messageDAO.findByReceiverId(userId)
  }
  
  def getSentMessages(userId: Long): Try[List[Message]] = {
    messageDAO.findBySenderId(userId)
  }
  
  def getConversation(userId1: Long, userId2: Long): Try[List[Message]] = {
    messageDAO.findConversation(userId1, userId2)
  }
  
  def getAllConversations(userId: Long): Try[List[(Long, String, String, LocalDateTime, String)]] = {
    messageDAO.findAllConversations(userId)
  }
  
  def getUnreadMessageCount(userId: Long): Try[Int] = {
    messageDAO.getUnreadMessageCount(userId)
  }
  
  def markConversationAsRead(currentUserId: Long, otherUserId: Long): Try[Int] = {
    messageDAO.markAsRead(otherUserId, currentUserId)
  }
  
  def deleteMessage(messageId: Long, userId: Long): Try[Boolean] = {
    messageDAO.deleteMessage(messageId, userId).map(_ > 0)
  }
  
  def searchMessages(userId: Long, searchTerm: String): Try[List[Message]] = {
    if (searchTerm.trim.isEmpty) {
      Failure(new RuntimeException("Le terme de recherche ne peut pas être vide"))
    } else {
      messageDAO.searchMessages(userId, searchTerm.trim)
    }
  }
  
  def getMessageStats(userId: Long): Try[Map[String, Any]] = {
    for {
      received <- getReceivedMessages(userId)
      sent <- getSentMessages(userId)
      unreadCount <- getUnreadMessageCount(userId)
    } yield {
      Map(
        "totalReceived" -> received.length,
        "totalSent" -> sent.length,
        "unreadCount" -> unreadCount,
        "totalMessages" -> (received.length + sent.length)
      )
    }
  }
  
  def getUsersWithMessages(userId: Long): Try[List[(Long, String, String)]] = {
    for {
      received <- getReceivedMessages(userId)
      sent <- getSentMessages(userId)
    } yield {
      val senderIds = received.map(_.senderId).distinct
      val receiverIds = sent.map(_.receiverId).distinct
      val allUserIds = (senderIds ++ receiverIds).distinct
      
      allUserIds.map { otherUserId =>
        userDAO.findById(otherUserId) match {
          case Success(Some(user)) =>
            Some((otherUserId, user.firstName, user.lastName))
          case _ => None
        }
      }.flatten
    }
  }
}