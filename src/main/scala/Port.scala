import scala.util.Try

object Port {
  def parse(raw: String): Either[String, Port] = {
    Try(raw.toInt)
      .toEither
      .left.map(_.getMessage)
      .filterOrElse(number => number > 0 && number < 65535, "Port must be between 0 and 65535")
      .map(new Port(_))
  }
}

class Port(val value: Int) extends AnyVal {
  override def toString: String = value.toString
}
