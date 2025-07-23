package dao

import models.User
import java.sql.ResultSet
import java.time.LocalDateTime
import scala.util.Try

class UserDAO extends BaseDAO {
  
  def create(user: User): Try[Long] = {
    val sql = """
      INSERT INTO users (email, password, first_name, last_name, phone, created_at, is_active)
      VALUES (?, ?, ?, ?, ?, ?, ?)
    """
    val params = Seq(
      user.email,
      user.password,
      user.firstName,
      user.lastName,
      user.phone,
      java.sql.Timestamp.valueOf(user.createdAt),
      user.isActive
    )
    executeInsert(sql, params)
  }
  
  def findByEmail(email: String): Try[Option[User]] = {
    val sql = "SELECT * FROM users WHERE email = ? AND is_active = true"
    executeQuery(sql, Seq(email))(mapResultSetToUser).map(_.headOption)
  }
  
  def findById(id: Long): Try[Option[User]] = {
    val sql = "SELECT * FROM users WHERE id = ? AND is_active = true"
    executeQuery(sql, Seq(id))(mapResultSetToUser).map(_.headOption)
  }
  
  def update(user: User): Try[Int] = {
    val sql = """
      UPDATE users 
      SET email = ?, first_name = ?, last_name = ?, phone = ?
      WHERE id = ? AND is_active = true
    """
    val params = Seq(
      user.email,
      user.firstName,
      user.lastName,
      user.phone,
      user.id.get
    )
    executeUpdate(sql, params)
  }
  
  def updatePassword(userId: Long, newPassword: String): Try[Int] = {
    val sql = "UPDATE users SET password = ? WHERE id = ? AND is_active = true"
    executeUpdate(sql, Seq(newPassword, userId))
  }
  
  def softDelete(userId: Long): Try[Int] = {
    val sql = "UPDATE users SET is_active = false WHERE id = ?"
    executeUpdate(sql, Seq(userId))
  }
  
  def updateAverageRating(userId: Long, rating: Double): Try[Int] = {
    val sql = "UPDATE users SET average_rating = ? WHERE id = ?"
    executeUpdate(sql, Seq(rating, userId))
  }
  
  private def mapResultSetToUser(rs: ResultSet): User = {
    User(
      id = Some(rs.getLong("id")),
      email = rs.getString("email"),
      password = rs.getString("password"),
      firstName = rs.getString("first_name"),
      lastName = rs.getString("last_name"),
      phone = rs.getString("phone"),
      averageRating = Option(rs.getDouble("average_rating")).filter(_ != 0),
      createdAt = rs.getTimestamp("created_at").toLocalDateTime,
      isActive = rs.getBoolean("is_active")
    )
  }
}
