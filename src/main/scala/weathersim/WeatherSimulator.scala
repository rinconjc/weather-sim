package weathersim

import akka.actor.{ActorSystem, Props}
import akka.event.EventStream
import org.slf4j.LoggerFactory

/**
  * @author julio on 6/04/19.
  */
object WeatherSimulator {

  private val log = LoggerFactory.getLogger("WeatherSimulator")

  def main(args: Array[String]): Unit = {
    val system = ActorSystem("WeatherSim")

    val eventBus = new EventStream(system)

    val numLocations = args.headOption.map(_.toInt).getOrElse(20)
    log.info("Deploying {} weather sensors", numLocations)
    //start weather event collector
    system.actorOf(Props(classOf[WeatherEventCollector], eventBus))
    //start weather sensors
    LocationProvider
      .randomLocations
      .take(numLocations)
      .map(location =>
        system.actorOf(Props(classOf[WeatherSensorActor], location
          , ImprovedWeatherEstimator, eventBus)))
      .foreach(_ ! Start)

  }

}
