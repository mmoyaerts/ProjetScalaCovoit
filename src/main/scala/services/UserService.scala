package services

import dao.UserDAO
import models.User
import org.mindrot.jbcrypt.BCrypt
import scala.util.{Try, Success, Failure}

class UserService {
  private val userDAO = new UserDAO()
  
  def register(email: String, password: String, firstName: String, lastName: String, phone: String): Try[Long] = {
    // Vérifier si l'utilisateur existe déjà
    userDAO.findByEmail(email) match {
      case Success(Some(_)) =>
        Failure(new RuntimeException("Un utilisateur avec cet email existe déjà"))
      case Success(None) =>
        // Hasher le mot de passe
        val hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt())
        val user = User(
          email = email,
          password = hashedPassword,
          firstName = firstName,
          lastName = lastName,
          phone = phone
        )
        userDAO.create(user)
      case Failure(exception) =>
        Failure(exception)
    }
  }
  
  def login(email: String, password: String): Try[User] = {
    userDAO.findByEmail(email) match {
      case Success(Some(user)) =>
        if (BCrypt.checkpw(password, user.password)) {
          Success(user)
        } else {
          Failure(new RuntimeException("Mot de passe incorrect"))
        }
      case Success(None) =>
        Failure(new RuntimeException("Utilisateur non trouvé"))
      case Failure(exception) =>
        Failure(exception)
    }
  }
  
  def updateProfile(userId: Long, email: String, firstName: String, lastName: String, phone: String): Try[Boolean] = {
    userDAO.findById(userId) match {
      case Success(Some(user)) =>
        val updatedUser = user.copy(
          email = email,
          firstName = firstName,
          lastName = lastName,
          phone = phone
        )
        userDAO.update(updatedUser).map(_ > 0)
      case Success(None) =>
        Failure(new RuntimeException("Utilisateur non trouvé"))
      case Failure(exception) =>
        Failure(exception)
    }
  }
  
  def changePassword(userId: Long, oldPassword: String, newPassword: String): Try[Boolean] = {
    userDAO.findById(userId) match {
      case Success(Some(user)) =>
        if (BCrypt.checkpw(oldPassword, user.password)) {
          val hashedNewPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt())
          userDAO.updatePassword(userId, hashedNewPassword).map(_ > 0)
        } else {
          Failure(new RuntimeException("Ancien mot de passe incorrect"))
        }
      case Success(None) =>
        Failure(new RuntimeException("Utilisateur non trouvé"))
      case Failure(exception) =>
        Failure(exception)
    }
  }
  
  def deleteAccount(userId: Long): Try[Boolean] = {
    userDAO.softDelete(userId).map(_ > 0)
  }
  
  def getUserById(userId: Long): Try[Option[User]] = {
    userDAO.findById(userId)
  }
}