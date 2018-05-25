import java.io.PrintStream
import java.time.Instant


object Logger {

  def apply(clazz: Class[_]): Logger = new Logger(clazz)

}


class Logger private(clazz: Class[_]) {

  // ------------------------------- Interface -------------------------------

  def info(message: String): Unit = log(System.out, "INFO", message)

  def warn(message: String): Unit = log(System.out, "WARN", message)

  def error(message: String): Unit = log(System.err, "ERROR", message)

  def error(message: String, exception: Throwable): Unit =
    log(System.err, "ERROR", message, exception.printStackTrace)


  // ------------------------------- Private -------------------------------

  private val name = clazz.getCanonicalName

  private def log(stream: PrintStream, level: String, message: String): Unit =
    log(stream, level, message, _ => {})

  private def log(stream: PrintStream, level: String, message: String, extra: PrintStream => Unit): Unit = {
    val now = Instant.now()

    stream.println("[" + level + "] " + now.toString + " " + name + " - " + message)
    extra(stream)
  }

}
