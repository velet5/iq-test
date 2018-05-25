import java.util.concurrent.{Executors, TimeUnit}


class Scheduler {

  private val scheduler = Executors.newScheduledThreadPool(1)

  def every(duration: Long, timeUnit: TimeUnit)(job: () => Unit): Unit =
    scheduler.scheduleAtFixedRate(() => job(), duration, duration, timeUnit)

  def shutdown(): Unit = scheduler.shutdown()

}
