package utils

import java.sql.{Connection, DriverManager}
import scala.util.{Failure, Success, Try}

class DBConnection {
  private val url = "jdbc:postgresql://localhost:5432/postgres"
  private val username = "postgres"
  private val password = "postgres"
  
  def getConnection: Try[Connection] = {
    Try {
      Class.forName("org.postgresql.Driver")
      DriverManager.getConnection(url, username, password)
    }
  }
  
  def closeConnection(connection: Connection): Unit = {
    Try(connection.close())
  }
}

object DBConnection {
  def apply(): DBConnection = new DBConnection()
}