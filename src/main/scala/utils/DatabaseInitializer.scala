package utils

import utils.DatabaseSchema
import scala.util.{Try, Success, Failure}

class DatabaseInitializer {
  private val dbConnection = new DBConnection()
  
  def initializeDatabase(): Try[Unit] = {
    dbConnection.getConnection match {
      case Success(connection) =>
        Try {
          val statement = connection.createStatement()
          DatabaseSchema.createTablesSQL.foreach { sql =>
            statement.execute(sql)
            println(s"Table créée avec succès")
          }
          statement.close()
          connection.close()
          println("Base de données initialisée avec succès")
        }
      case Failure(exception) =>
        Failure(new RuntimeException(s"Erreur de connexion à la base de données: ${exception.getMessage}"))
    }
  }
}

object DatabaseInitializer {
  def apply(): DatabaseInitializer = new DatabaseInitializer()
}