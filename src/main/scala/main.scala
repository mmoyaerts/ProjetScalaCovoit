import menu.UserManagementConsole
import utils.DatabaseInitializer
import scala.util.{Success, Failure}

object Main extends App {
  println("Initialisation de la base de données...")
  
  // Initialiser la base de données
  val dbInitializer = DatabaseInitializer()
  dbInitializer.initializeDatabase() match {
    case Success(_) =>
      println("Base de données initialisée avec succès")
      
      // Démarrer l'application console
      val console = new UserManagementConsole()
      console.start()
      
    case Failure(exception) =>
      println(s"Erreur lors de l'initialisation de la base de données: ${exception.getMessage}")
      println("L'application ne peut pas démarrer")
  }
}