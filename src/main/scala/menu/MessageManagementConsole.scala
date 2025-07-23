package menu

import services.{MessageService, UserService, TripService, ReservationService}
import models.{User, Message}
import java.time.format.DateTimeFormatter
import scala.io.StdIn
import scala.util.{Try, Success, Failure}

class MessageManagementConsole(currentUser: User) {
  private val messageService = new MessageService()
  private val userService = new UserService()
  private val tripService = new TripService()
  private val reservationService = new ReservationService()
  private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
  
  def start(): Unit = {
    var managing = true
    
    while (managing) {
      showMessageMenu()
      val choice = StdIn.readLine("Votre choix: ")
      
      choice match {
        case "1" => viewConversations()
        case "2" => sendNewMessage()
        case "3" => viewReceivedMessages()
        case "4" => viewSentMessages()
        case "5" => searchMessages()
        case "6" => messageWithTripParticipants()
        case "7" => viewMessageStats()
        case "8" => managing = false
        case _ => println("Choix invalide")
      }
    }
  }
  
  private def showMessageMenu(): Unit = {
    messageService.getUnreadMessageCount(currentUser.id.get) match {
      case Success(unreadCount) =>
        val unreadText = if (unreadCount > 0) s" ($unreadCount non lus)" else ""
        println(s"\n=== Messagerie$unreadText ===")
      case _ =>
        println("\n=== Messagerie ===")
    }
    
    println("1. Mes conversations")
    println("2. Envoyer un nouveau message")
    println("3. Messages reçus")
    println("4. Messages envoyés")
    println("5. Rechercher dans les messages")
    println("6. Contacter participants d'un trajet")
    println("7. Statistiques de messagerie")
    println("8. Retour")
  }
  
  private def viewConversations(): Unit = {
    println("\n=== Mes conversations ===")
    
    messageService.getAllConversations(currentUser.id.get) match {
      case Success(conversations) =>
        if (conversations.isEmpty) {
          println("Aucune conversation")
        } else {
          conversations.foreach { case (userId, firstName, lastName, lastMessageTime, lastContent) =>
            val preview = if (lastContent.length > 50) lastContent.take(50) + "..." else lastContent
            println(s"👤 $firstName $lastName (ID: $userId)")
            println(s"   💬 $preview")
            println(s"   🕒 ${lastMessageTime.format(dateFormatter)}")
            println("---")
          }
          
          print("\nOuvrir une conversation (ID utilisateur) ou 0 pour retour: ")
          val userId = StdIn.readLong()
          if (userId != 0) {
            openConversation(userId)
          }
        }
      case Failure(exception) =>
        println(s"Erreur: ${exception.getMessage}")
    }
  }
  
  private def openConversation(otherUserId: Long): Unit = {
    userService.getUserById(otherUserId) match {
      case Success(Some(otherUser)) =>
        println(s"\n=== Conversation avec ${otherUser.firstName} ${otherUser.lastName} ===")
        
        // Marquer les messages comme lus
        messageService.markConversationAsRead(currentUser.id.get, otherUserId)
        
        messageService.getConversation(currentUser.id.get, otherUserId) match {
          case Success(messages) =>
            if (messages.isEmpty) {
              println("Aucun message dans cette conversation")
            } else {
              messages.foreach { message =>
                val senderName = if (message.senderId == currentUser.id.get) "Vous" else s"${otherUser.firstName}"
                val direction = if (message.senderId == currentUser.id.get) "→" else "←"
                println(s"$direction $senderName (${message.sentAt.format(dateFormatter)})")
                println(s"  ${message.content}")
                println()
              }
            }
            
            println("Actions:")
            println("1. Répondre")
            println("2. Retour")
            
            val choice = StdIn.readLine("Votre choix: ")
            if (choice == "1") {
              print("Votre message: ")
              val content = StdIn.readLine()
              
              messageService.sendMessage(currentUser.id.get, otherUserId, content) match {
                case Success(messageId) =>
                  println(s"✅ Message envoyé ! (ID: $messageId)")
                  // Rouvrir la conversation pour voir le nouveau message
                  openConversation(otherUserId)
                case Failure(exception) =>
                  println(s"❌ Erreur: ${exception.getMessage}")
              }
            }
            
          case Failure(exception) =>
            println(s"Erreur: ${exception.getMessage}")
        }
        
      case Success(None) =>
        println("Utilisateur non trouvé")
      case Failure(exception) =>
        println(s"Erreur: ${exception.getMessage}")
    }
  }
  
  private def sendNewMessage(): Unit = {
    println("\n=== Envoyer un nouveau message ===")
    
    print("ID du destinataire: ")
    val receiverId = StdIn.readLong()
    StdIn.readLine() // consume newline
    
    // Vérifier que l'utilisateur existe et afficher ses informations
    userService.getUserById(receiverId) match {
      case Success(Some(receiver)) =>
        println(s"Destinataire: ${receiver.firstName} ${receiver.lastName}")
        receiver.averageRating.foreach(rating => 
          println(s"Note: ${rating.formatted("%.1f")}/5 ⭐")
        )
        
        print("Votre message: ")
        val content = StdIn.readLine()
        
        messageService.sendMessage(currentUser.id.get, receiverId, content) match {
          case Success(messageId) =>
            println(s"✅ Message envoyé avec succès ! (ID: $messageId)")
          case Failure(exception) =>
            println(s"❌ Erreur: ${exception.getMessage}")
        }
        
      case Success(None) =>
        println("❌ Destinataire non trouvé")
      case Failure(exception) =>
        println(s"❌ Erreur: ${exception.getMessage}")
    }
  }
  
  private def viewReceivedMessages(): Unit = {
    println("\n=== Messages reçus ===")
    
    messageService.getReceivedMessages(currentUser.id.get) match {
      case Success(messages) =>
        if (messages.isEmpty) {
          println("Aucun message reçu")
        } else {
          messages.foreach { message =>
            userService.getUserById(message.senderId) match {
              case Success(Some(sender)) =>
                println(s"📨 De: ${sender.firstName} ${sender.lastName}")
                println(s"🕒 ${message.sentAt.format(dateFormatter)}")
                println(s"💬 ${message.content}")
                println(s"ID: ${message.id.get}")
                println("---")
              case _ =>
                println("Erreur lors de la récupération de l'expéditeur")
            }
          }
          
          println("\nActions:")
          println("1. Répondre à un message")
          println("2. Supprimer un message")
          println("3. Retour")
          
          val choice = StdIn.readLine("Votre choix: ")
          choice match {
            case "1" => replyToMessage()
            case "2" => deleteMessage()
            case _ => ()
          }
        }
      case Failure(exception) =>
        println(s"Erreur: ${exception.getMessage}")
    }
  }
  
  private def viewSentMessages(): Unit = {
    println("\n=== Messages envoyés ===")
    
    messageService.getSentMessages(currentUser.id.get) match {
      case Success(messages) =>
        if (messages.isEmpty) {
          println("Aucun message envoyé")
        } else {
          messages.foreach { message =>
            userService.getUserById(message.receiverId) match {
              case Success(Some(receiver)) =>
                println(s"📤 À: ${receiver.firstName} ${receiver.lastName}")
                println(s"🕒 ${message.sentAt.format(dateFormatter)}")
                println(s"💬 ${message.content}")
                println(s"ID: ${message.id.get}")
                println("---")
              case _ =>
                println("Erreur lors de la récupération du destinataire")
            }
          }
        }
      case Failure(exception) =>
        println(s"Erreur: ${exception.getMessage}")
    }
  }
  
  private def searchMessages(): Unit = {
    println("\n=== Rechercher dans les messages ===")
    print("Terme de recherche: ")
    val searchTerm = StdIn.readLine()
    
    messageService.searchMessages(currentUser.id.get, searchTerm) match {
      case Success(messages) =>
        if (messages.isEmpty) {
          println("Aucun message trouvé")
        } else {
          println(s"\n${messages.length} message(s) trouvé(s):")
          messages.foreach { message =>
            val isReceived = message.receiverId == currentUser.id.get
            val otherUserId = if (isReceived) message.senderId else message.receiverId
            
            userService.getUserById(otherUserId) match {
              case Success(Some(otherUser)) =>
                val direction = if (isReceived) "📨 De" else "📤 À"
                println(s"$direction: ${otherUser.firstName} ${otherUser.lastName}")
                println(s"🕒 ${message.sentAt.format(dateFormatter)}")
                println(s"💬 ${message.content}")
                println("---")
              case _ =>
                println("Erreur lors de la récupération de l'utilisateur")
            }
          }
        }
      case Failure(exception) =>
        println(s"Erreur: ${exception.getMessage}")
    }
  }
  
  private def messageWithTripParticipants(): Unit = {
    println("\n=== Contacter des participants de trajet ===")
    
    // Récupérer les trajets de l'utilisateur
    tripService.getDriverTrips(currentUser.id.get) match {
      case Success(trips) =>
        if (trips.isEmpty) {
          println("Vous n'avez créé aucun trajet")
        } else {
          println("Vos trajets:")
          trips.take(10).foreach { trip => // Limiter à 10 trajets récents
            println(s"ID: ${trip.id.get} - ${trip.departureCity} → ${trip.arrivalCity}")
            println(s"   Départ: ${trip.departureTime.format(dateFormatter)}")
            println("---")
          }
          
          print("ID du trajet: ")
          val tripId = StdIn.readLong()
          StdIn.readLine() // consume newline
          
          // Récupérer les passagers de ce trajet
          reservationService.getDriverReservations(currentUser.id.get) match {
            case Success(reservations) =>
              val tripReservations = reservations.filter(_.tripId == tripId)
              
              if (tripReservations.isEmpty) {
                println("Aucun passager pour ce trajet")
              } else {
                println("Passagers du trajet:")
                tripReservations.foreach { reservation =>
                  userService.getUserById(reservation.passengerId) match {
                    case Success(Some(passenger)) =>
                      println(s"${passenger.id.get}. ${passenger.firstName} ${passenger.lastName}")
                    case _ => ()
                  }
                }
                
                print("ID du passager à contacter: ")
                val passengerId = StdIn.readLong()
                StdIn.readLine() // consume newline
                
                tripService.getTripById(tripId) match {
                  case Success(Some(trip)) =>
                    print(s"Message (concernant le trajet ${trip.departureCity} → ${trip.arrivalCity}): ")
                    val content = StdIn.readLine()
                    
                    val fullMessage = s"[Trajet ${trip.departureCity} → ${trip.arrivalCity} du ${trip.departureTime.format(dateFormatter)}] $content"
                    
                    messageService.sendMessage(currentUser.id.get, passengerId, fullMessage) match {
                      case Success(messageId) =>
                        println(s"✅ Message envoyé au passager ! (ID: $messageId)")
                      case Failure(exception) =>
                        println(s"❌ Erreur: ${exception.getMessage}")
                    }
                  case _ =>
                    println("Trajet non trouvé")
                }
              }
            case Failure(exception) =>
              println(s"Erreur: ${exception.getMessage}")
          }
        }
      case Failure(exception) =>
        println(s"Erreur: ${exception.getMessage}")
    }
  }
  
  private def viewMessageStats(): Unit = {
    println("\n=== Statistiques de messagerie ===")
    
    messageService.getMessageStats(currentUser.id.get) match {
      case Success(stats) =>
        println(s"📨 Messages reçus: ${stats("totalReceived")}")
        println(s"📤 Messages envoyés: ${stats("totalSent")}")
        println(s"🔔 Messages non lus: ${stats("unreadCount")}")
        println(s"📊 Total des messages: ${stats("totalMessages")}")
        
        messageService.getUsersWithMessages(currentUser.id.get) match {
          case Success(users) =>
            println(s"👥 Conversations avec ${users.length} personne(s)")
          case _ => ()
        }
        
      case Failure(exception) =>
        println(s"Erreur: ${exception.getMessage}")
    }
  }
  
  private def replyToMessage(): Unit = {
    print("ID du message auquel répondre: ")
    val messageId = StdIn.readLong()
    StdIn.readLine() // consume newline
    
    messageService.getReceivedMessages(currentUser.id.get) match {
      case Success(messages) =>
        messages.find(_.id.contains(messageId)) match {
          case Some(originalMessage) =>
            userService.getUserById(originalMessage.senderId) match {
              case Success(Some(sender)) =>
                println(s"Réponse à: ${sender.firstName} ${sender.lastName}")
                println(s"Message original: ${originalMessage.content}")
                
                print("Votre réponse: ")
                val response = StdIn.readLine()
                
                messageService.sendMessage(currentUser.id.get, originalMessage.senderId, response) match {
                  case Success(responseId) =>
                    println(s"✅ Réponse envoyée ! (ID: $responseId)")
                  case Failure(exception) =>
                    println(s"❌ Erreur: ${exception.getMessage}")
                }
              case _ =>
                println("Expéditeur non trouvé")
            }
          case None =>
            println("Message non trouvé")
        }
      case Failure(exception) =>
        println(s"Erreur: ${exception.getMessage}")
    }
  }
  
  private def deleteMessage(): Unit = {
    print("ID du message à supprimer: ")
    val messageId = StdIn.readLong()
    StdIn.readLine() // consume newline
    
    print("Confirmer la suppression (oui/non): ")
    val confirmation = StdIn.readLine().toLowerCase
    
    if (confirmation == "oui" || confirmation == "o") {
      messageService.deleteMessage(messageId, currentUser.id.get) match {
        case Success(true) =>
          println("✅ Message supprimé avec succès")
        case Success(false) =>
          println("❌ Message non trouvé ou non autorisé")
        case Failure(exception) =>
          println(s"❌ Erreur: ${exception.getMessage}")
      }
    } else {
      println("Suppression annulée")
    }
  }
}