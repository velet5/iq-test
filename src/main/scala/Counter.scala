import akka.actor.ActorSystem
import redis.RedisClient

import scala.concurrent.Future

trait Counter {
  type UserId = String

  /** Intended to use in a "fire and forget" fashion.
    * Doesn't imply to be async or non-blocking - totally depends on the implementation */
  def count(userId: UserId): Unit
  
  def size(): Future[Long]
}


trait BackupableCounter extends Counter {
  def all(): Iterable[UserId]
}


/** Although the [[size]] method has result type Future[Long],
  * actual maximal size is about [[Int.MaxValue]]
  * because it is backed up with scala [[Set]] */
class SyncronizedImmCounter extends BackupableCounter {

  private var users = Set[UserId]()

  override def count(userId: UserId): Unit = synchronized(users += userId)

  override def size(): Future[Long] = Future.successful(users.size)

  override def all(): Iterable[UserId] = users
  
}


// for some reason size is returning bigger number than expected
//
//class ParCounter extends BackupableCounter {
//
//  private val users = scala.collection.parallel.mutable.ParSet[UserId]()
//
//  override def count(userId: UserId): Unit = users += userId
//
//  override def size(): Future[Long] = Future.successful(users.size)
//
//  override def all(): Iterable[UserId] = users.to[Set]
//
//}


/** I had no previous experience with Redis.
  * So i just run it with docker like "sudo docker run -p 3333:6379 redis"
  * The persistence between restarts is implied to work via redis fsync.
  *  */
class RedisCounter(port: Port) extends Counter {

  // is two actor systems in one procees good idea?
  // maybe i should configure one actor system for http-server and redis-client
  // and tune some settings
  // as i never worked with neither of them - this is a bit of rocket science to me right now
  implicit private val akkaSystem: ActorSystem = akka.actor.ActorSystem("redis-system")

  private val redis = RedisClient(port = port.value)

  override def count(userId: UserId): Unit = redis.set(userId, "")

  override def size(): Future[Long] = redis.dbsize()
}