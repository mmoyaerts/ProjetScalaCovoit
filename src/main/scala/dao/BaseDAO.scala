package dao

import utils.DBConnection

import java.sql.{Connection, PreparedStatement, ResultSet, Statement}
import scala.util.{Try, Using}

trait BaseDAO {
  protected val dbConnection = new DBConnection()
  
  protected def executeQuery[T](sql: String, params: Seq[Any] = Seq.empty)(mapper: ResultSet => T): Try[List[T]] = {
    Using.Manager { use =>
      val connection = use(dbConnection.getConnection.get)
      val statement = use(connection.prepareStatement(sql))
      
      params.zipWithIndex.foreach { case (param, index) =>
        statement.setObject(index + 1, param)
      }
      
      val resultSet = use(statement.executeQuery())
      var results = List.empty[T]
      
      while (resultSet.next()) {
        results = mapper(resultSet) :: results
      }
      
      results.reverse
    }
  }
  
  protected def executeUpdate(sql: String, params: Seq[Any] = Seq.empty): Try[Int] = {
    Using.Manager { use =>
      val connection = use(dbConnection.getConnection.get)
      val statement = use(connection.prepareStatement(sql))
      
      params.zipWithIndex.foreach { case (param, index) =>
        statement.setObject(index + 1, param)
      }
      
      statement.executeUpdate()
    }
  }
  
  protected def executeInsert(sql: String, params: Seq[Any] = Seq.empty): Try[Long] = {
    Using.Manager { use =>
      val connection = use(dbConnection.getConnection.get)
      val statement = use(connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS))
      
      params.zipWithIndex.foreach { case (param, index) =>
        statement.setObject(index + 1, param)
      }
      
      statement.executeUpdate()
      val generatedKeys = use(statement.getGeneratedKeys)
      
      if (generatedKeys.next()) {
        generatedKeys.getLong(1)
      } else {
        throw new RuntimeException("No generated key returned")
      }
    }
  }
}