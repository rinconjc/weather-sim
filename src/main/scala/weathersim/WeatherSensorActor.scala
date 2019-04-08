package weathersim

import java.time.{Duration, LocalDateTime}
import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.event.EventStream

import scala.concurrent.duration.FiniteDuration
import scala.util.Random

/**
  * @author julio on 6/04/19.
  */

class WeatherSensorActor(location: Location, estimator: WeatherEstimator, eventBus: EventStream) extends Actor with ActorLogging {

  import context.dispatcher

  val NEARBY_THRESHOLD_KMS = 100
  val startTime = LocalDateTime.now()
  var neighbors: List[ActorRef] = List()
  var lastEvent: Option[WeatherEvent] = None

  eventBus.subscribe(self, classOf[Started])

  override def receive: Receive = {
    case Start =>
      log.info("starting sensor for {}", location)
      eventBus.publish(Started(self, location))
      self ! ReportWeather

    case ReportWeather =>
      val nextEvent = estimator.generateNewEvent(location, currentTime, lastEvent)
      lastEvent = Some(nextEvent)
      eventBus.publish(nextEvent)
      neighbors.foreach(_ ! NearbyEvent(nextEvent))
      context.system.scheduler.scheduleOnce(FiniteDuration(Random.nextInt(5) + 1
        , TimeUnit.SECONDS), self, ReportWeather)

    case NearbyEvent(event) =>
      estimator.fromNearbyEvent(event, currentTime , lastEvent)
        .foreach(nextEvent =>{
          lastEvent = Some(nextEvent)
          log.info("reacted to nearby event in {}: {}", event, nextEvent)
          eventBus.publish(nextEvent)
        })

    case Started(other, otherLocation) =>
      if (other != self && location.distance(otherLocation) < NEARBY_THRESHOLD_KMS) {
        neighbors = other :: neighbors
        log.info("neighbor sensor detected: {} and {}", location, otherLocation)
        //subscribe to events from neighbor sensor
      }

    case Stop => log.info("stopping sensor for {}", location)
  }

  def currentTime = {
    val time = LocalDateTime.now()
    val elapsed = Duration.between(startTime, time)
    time.plusSeconds(elapsed.toSeconds * Config.timeScale)
  }

}

// Actor Messages
case object Start

case object Stop

case class Started(sensor: ActorRef, location: Location)

case object ReportWeather

case class NearbyEvent(event: WeatherEvent)

