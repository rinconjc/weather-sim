package weathersim

import java.time.format.DateTimeFormatter.ofPattern

import akka.actor.Actor
import akka.event.EventStream

/**
  * @author julio on 6/04/19.
  */
class WeatherEventCollector(eventBus: EventStream) extends Actor {

  override def preStart(): Unit = {
    eventBus.subscribe(self, classOf[WeatherEvent])
  }

  override def receive: Receive = {
    case WeatherEvent(loc, time, condition, temp, pressure, humidity) =>
      printf("%s|%s|%s|%s|%.0f|%.2f|%.0f\n", loc.name, loc.position, time.format(ofPattern("yyyy-MM-dd HH:mm:ss"))
        , condition, temp, pressure, humidity)
  }
}
