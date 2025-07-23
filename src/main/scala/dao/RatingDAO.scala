package dao

import models.Rating
import java.sql.ResultSet
import scala.util.Try

class RatingDAO extends BaseDAO {
  
  def create(rating: Rating): Try[Long] = {
    val sql = """
      INSERT INTO ratings (trip_id, rater_id, rated_id, rating, comment, created_at)
      VALUES (?, ?, ?, ?, ?, ?)
    """
    val params = Seq(
      rating.tripId,
      rating.raterId,
      rating.ratedId,
      rating.rating,
      rating.comment.orNull,
      java.sql.Timestamp.valueOf(rating.createdAt)
    )
    executeInsert(sql, params)
  }
  
  def findByRatedId(ratedId: Long): Try[List[Rating]] = {
    val sql = "SELECT * FROM ratings WHERE rated_id = ? ORDER BY created_at DESC"
    executeQuery(sql, Seq(ratedId))(mapResultSetToRating)
  }
  
  def findByRaterId(raterId: Long): Try[List[Rating]] = {
    val sql = "SELECT * FROM ratings WHERE rater_id = ? ORDER BY created_at DESC"
    executeQuery(sql, Seq(raterId))(mapResultSetToRating)
  }
  
  def findByTripId(tripId: Long): Try[List[Rating]] = {
    val sql = "SELECT * FROM ratings WHERE trip_id = ?"
    executeQuery(sql, Seq(tripId))(mapResultSetToRating)
  }
  
  def existsRating(tripId: Long, raterId: Long, ratedId: Long): Try[Boolean] = {
    val sql = "SELECT COUNT(*) as count FROM ratings WHERE trip_id = ? AND rater_id = ? AND rated_id = ?"
    executeQuery(sql, Seq(tripId, raterId, ratedId)) { rs =>
      rs.getInt("count") > 0
    }.map(_.headOption.getOrElse(false))
  }
  
  def getAverageRating(userId: Long): Try[Option[Double]] = {
    val sql = "SELECT AVG(rating::DECIMAL) as avg_rating FROM ratings WHERE rated_id = ?"
    executeQuery(sql, Seq(userId)) { rs =>
      val avg = rs.getDouble("avg_rating")
      if (rs.wasNull()) None else Some(avg)
    }.map(_.headOption.flatten)
  }
  
  def getRatingCount(userId: Long): Try[Int] = {
    val sql = "SELECT COUNT(*) as count FROM ratings WHERE rated_id = ?"
    executeQuery(sql, Seq(userId)) { rs =>
      rs.getInt("count")
    }.map(_.headOption.getOrElse(0))
  }
  
  private def mapResultSetToRating(rs: ResultSet): Rating = {
    Rating(
      id = Some(rs.getLong("id")),
      tripId = rs.getLong("trip_id"),
      raterId = rs.getLong("rater_id"),
      ratedId = rs.getLong("rated_id"),
      rating = rs.getInt("rating"),
      comment = Option(rs.getString("comment")),
      createdAt = rs.getTimestamp("created_at").toLocalDateTime
    )
  }
}