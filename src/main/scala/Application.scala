import java.util.concurrent.TimeUnit._


object Application {

  private val log = Logger(getClass)

  def main(args: Array[String]): Unit = {
    Parameters.parse(args) match {
      case Left(message) =>
        println(message)

      case Right(parameters) if parameters.help =>
        Parameters.printHelp()

      case Right(parameters) =>
        runWith(parameters)
    }
  }


  private def runWith(parameters: Parameters): Unit = {
    log.info("Application started")

    val counter = parameters.redisPort.fold(new SyncronizedImmCounter: Counter)(new RedisCounter(_))

    setupBackup(counter, parameters)

    val runningServer = new Server(parameters.port, counter).start()

    // i realized that i have no idea how to properly add a shutdown hook
    Runtime.getRuntime.addShutdownHook(new Thread() {
      override def run(): Unit = {
        log.info("Server is shutting down")
        runningServer.stop()
      }
    })
  }


  private def setupBackup(counter: Counter, parameters: Parameters): Unit = {
    counter match {
      case backupable: BackupableCounter =>
        val restorer = new Restorer(parameters.backupDirectory)
        val scheduler = new Scheduler

        if (parameters.cleanBackups) {
          restorer.clean()
        } else {
          restorer.restore(counter)
        }

        scheduler.every(30, SECONDS)(() => restorer.backup(backupable))

        Runtime.getRuntime.addShutdownHook(new Thread() {
          override def run(): Unit = {
            log.info("Backup mechanism is shutting down")
            scheduler.shutdown()
            restorer.backup(backupable)
          }
        })

      case _ =>
    }
  }
}
