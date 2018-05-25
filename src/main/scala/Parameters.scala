import java.nio.file.{Path, Paths}

import scala.util.Try

case class Parameters(
    port: Port,
    backupDirectory: Path,
    redisPort: Option[Port] = None,
    cleanBackups: Boolean = false,
    help: Boolean = false)


/** Naive command line arguments parser */
object Parameters {

  val defaultPort = new Port(8080)

  val defaultBackupDirectory: Path = Paths.get("/tmp/userIds")

  val defaultParameters = Parameters(defaultPort, defaultBackupDirectory, redisPort = None)

  def parse(strings: Array[String]): Either[String, Parameters] = {
    val firstUnrecognizedArgument: Option[String] =
      strings
        .filter(_.startsWith("--"))
        .find(string => !arguments.exists("--" + _.name == string))

    firstUnrecognizedArgument match {
      case Some(argument) =>
        Left(s"Urecognized argument $argument")

      case None =>
        for {
          port <- port.find(strings)
          directory <- directory.find(strings)
          redis <- redis.find(strings)
          clean <- cleanBackups.find(strings)
          help <- help.find(strings)
        } yield Parameters(port, directory, redis, clean, help)
    }
  }

  def printHelp(): Unit = {
    arguments.foreach { argument =>
     println("--" + argument.name + "\t" + argument.description)
    }
  }


  // ------------------------------- Arguments -------------------------------

  private val EmptyConverter: String => Either[String, Nothing] = _ => Left("No value expected here")
  private val TrueConverter: String => Either[String, Boolean] = _ => Right(true)

  case class Argument[A](
    name: String,
    description: String,
    needValue: Boolean,
    defaultValue: A,
    converter: String => Either[String, A] = EmptyConverter) {

    def find(args: Array[String]): Either[String, A] = {
      val prefixed = "--" + name
      val index = args.indexOf(prefixed)

      if (index < 0) {
        Right(defaultValue)
      } else if (needValue) {
        parseValue(args, index)
      } else {
        converter("")
      }
    }

    private def parseValue(args: Array[String], index: Int): Either[String, A] = {
      val nextIndex = index + 1

      if (!args.isDefinedAt(nextIndex)) {
        Left(s"Argument --$name needs value")
      } else {
        val raw = args(nextIndex)
        converter(raw)
      }
    }
  }

  private val port = Argument[Port](
    name = "port",
    description = s"HTTP port to serve requests on, default is $defaultPort",
    needValue = true,
    converter = Port.parse,
    defaultValue = defaultPort)

  private val directory = Argument[Path](
    name = "backup-dir",
    description = s"Directory to store backups, default is ${defaultBackupDirectory.toString}",
    needValue = true,
    converter = value => Try(Paths.get(value)).toEither.left.map(_.getMessage),
    defaultValue = defaultBackupDirectory)

  private val cleanBackups = Argument[Boolean](
    name = "clean",
    description = "Removes all existing backups in specified backup directory",
    needValue = false,
    converter = TrueConverter,
    defaultValue = false)

  private val redis = Argument[Option[Port]](
    name = "redis",
    description = s"Uses redis-server for persistance. Needs a port",
    needValue = true,
    converter = Port.parse(_).map(Option(_)),
    defaultValue = None)

  private val help = Argument[Boolean](
    name = "help",
    description = "Print this message and exit",
    needValue = false,
    converter = TrueConverter,
    defaultValue = false)

  private val arguments = Seq(port, directory, cleanBackups, redis, help)

}
