import java.io.{File, FilenameFilter}
import java.nio.file.{Files, Path}
import java.time.Instant

import scala.util.{Failure, Success, Try}


private object Restorer {

  private val FileExtension: String = ".userid-backup"

  private val fileFilter: FilenameFilter = (_: File, name: String) => name.endsWith(FileExtension)

  private val log: Logger = Logger(getClass)

}


class Restorer(directory: Path) {

  import Restorer._
  import scala.collection.JavaConverters._

  def backup(counter: BackupableCounter): Unit = {
    val directoryFile = directory.toFile

    if (!directoryFile.exists() && !directoryFile.mkdirs()) {
      log.error("Creating backup directory failed")
    } else {
      val now = Instant.now()
      val filename = (now.toString + FileExtension).replace(":", "-") // replacing colons for Windows
      val backupPath = directory.resolve(filename)
      val idList = counter.all()
      
      log.info(s"Starting back up at $now")
      Try(Files.write(backupPath, idList.asJava)) match {
        case Success(_) => log.info(s"Backed up to ${backupPath.toString}")
        case Failure(ex) => log.error("Backing up failed", ex)
      }
    }
  }


  def restore(counter: Counter): Unit = {
    val files = backups()
    val maybeFile = pickBestFile(files)

    maybeFile match {
      case Some(file) =>
        log.info(s"Chosen ${file.getName} as most suitable for restoring")
        
        scala.io.Source
          .fromFile(file)
          .getLines()
          .foreach(counter.count)

        log.info(s"State was restored from ${file.getName}")
        
      case None =>
        log.info("Cannot find any backups")
    }
  }


  def clean(): Unit = {
    val files = backups()
    files.foreach(_.delete())
  }

  // ------------------------------- Private -------------------------------


  private def backups(): Seq[File] = {
    val files = directory.toFile.listFiles(fileFilter)
    Option(files).fold(Seq.empty[File])(_.toSeq)
  }

  private def pickBestFile(files: Seq[File]): Option[File] = {
    if (files.isEmpty) {
      None
    } else {
      val largestFile = files.maxBy(_.length())
      val latestFile = files.maxBy(_.getName)

      if (largestFile != latestFile) {
        log.warn(
          s"Althoung latest backup file is ${latestFile.getName}, " +
          s"choosing ${largestFile.getName} as a larger one " +
          s"and thus more likely to contain more info")
      }

      Some(largestFile)
    }
  }

}
