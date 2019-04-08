package weathersim

import java.lang.Math._
import java.time.LocalDateTime

import weathersim.Condition.Condition

import scala.util.Random

/**
  * Interface for weather variables estimator implementations
  *
  */
trait WeatherEstimator {

  /**
    * Generates weather data for the given location
    *
    * @param lastEvent
    * @param location
    * @return
    */
  def generateNewEvent(location: Location, lastEvent: Option[WeatherEvent]): WeatherEvent

  /**
    * Generates an event as a reaction to an event nearby
    *
    * @param event
    * @param lastEvent
    * @return
    */
  def fromNearbyEvent(event: WeatherEvent, lastEvent: Option[WeatherEvent]): Option[WeatherEvent] =
    lastEvent.flatMap(last => {
      if (last.condition == event.condition) //same condition already
        None
      else if (Random.nextFloat() > 0.5) //50% chance that it should in the same condition
        Some(last.copy(condition = event.condition,
          temperature = (event.temperature + last.temperature) / 2))
      else None
    })

}

/**
  * Weather conditions
  */
object Condition extends Enumeration {
  type Condition = Value
  val SUNNY, RAIN, SNOW = Value
}

/**
  * Weather event
  *
  * @param location
  * @param time
  * @param condition
  * @param temperature
  * @param pressure
  * @param humidity
  */
case class WeatherEvent(location: Location, time: LocalDateTime, condition: Condition
                        , temperature: Double, pressure: Double, humidity: Double)


trait SimpleTemperatureEstimator {
  private val MAX_DAY_NIGHT_TEMP_CHANGE = 10 //max difference of temperature between day and night
  private val MAX_LATITUDE_TEMP_CHANGE = 50 //max difference in temperatures between latitude 0 and latitude 90
  private val MAX_ALTITUDE_TEMP_CHANGE = 32 //https://www.engineeringtoolbox.com/air-altitude-temperature-d_461.html

  def estimateTemperature(position: Position, time: LocalDateTime, lastEvent: Option[WeatherEvent]): Double = {
    val temp = Random.nextFloat() * 15 + 20 //initial temperature at sea-level latitude 0 midday [20 to 35]
    //effect of time of day, assuming a peak at 16hr and lowest at 4hr
    val timeEffect = Math.sin(7 * PI / 6 + (time.getHour + time.getMinute / 60.0) * PI / 12) * MAX_DAY_NIGHT_TEMP_CHANGE
    //effect of the latitude, max temp is reached at latitude 0, and it decreases with latitude increases
    val latitudeEffect = -pow(position.lat / 90f, 2) * MAX_LATITUDE_TEMP_CHANGE //assume a quadratic effect
    //effect of the altitude (assuming a linear relationship here)
    val altitudeEffect = -MAX_ALTITUDE_TEMP_CHANGE * position.elev / 4877f
    //final temperature
    withinRange(temp + timeEffect + latitudeEffect + altitudeEffect, -45, 58) //ensure in normal range
  }
}

trait SimplePressureEstimator {
  private val SEA_LEVEL_PRESSURE = 101325 * 1e-2f //hPa
  private val PRESSURE_ALTITUDE_FACTOR = 2.25577e-5 //magic factor (see https://www.engineeringtoolbox.com/air-altitude-pressure-d_462.html)

  def estimatePressure(position: Position, time: LocalDateTime, temperature: Double) = {
    //Relationship between pressure and elevation https://www.engineeringtoolbox.com/air-altitude-pressure-d_462.html
    SEA_LEVEL_PRESSURE * pow(1 - PRESSURE_ALTITUDE_FACTOR * position.elev, 5.25588)
  }
}

trait SimpleHumidityEstimator {
  def estimateHumidity(position: Position, time: LocalDateTime, pressure: Double, temp: Double): Double = {
    //relative humidity ranges from 0% to 100%, there's inverse relationship with temperature, as well as the altitude
    //we start with a a random humidity around 60% and apply effects of temperature and altitude
    val humidity = Random.nextFloat() * 30 + 30f
    //temperature effect
    val tempEffect = 20 - 30 * (withinRange(temp, -45, 58) + 45) / (58 + 45) //assume min temp -45 and max 58
    val altitudeEffect = 10 - 20 * withinRange(position.elev, 0, 6000) / 6000
    withinRange(humidity + tempEffect + altitudeEffect, 0, 100) //ensure in range of 0 to 100%
  }

}

trait SimpleConditionEstimator {
  def estimateCondition(position: Position, temp: Double, pressure: Double, humidity: Double): Condition = {
    //probability of snow should increase inversely with the temperature starting at 0C
    //assume a linear relationship of the prob. of snow
    val probSnowLowTemp = withinRange(0.2 - 60 * temp / 45.0, 0, 0.6)

    if (Random.nextFloat() <= probSnowLowTemp)
      Condition.SNOW
    else {
      //rain is probably more likely within temp range of 0 to 25C, and humidity is above 50%
      if (humidity > 50.0 && temp > 0 && temp < 25 && Random.nextFloat() > 0.5)
        Condition.RAIN
      else Condition.SUNNY
    }
  }
}

/**
  * Very simple and gross weather variable estimator
  */
object SuperSimpleWeatherEstimator extends WeatherEstimator with SimpleTemperatureEstimator with SimplePressureEstimator
  with SimpleHumidityEstimator with SimpleConditionEstimator {
  /**
    * Generates weather info for the given location
    *
    * @param lastEvent
    * @param location
    * @return
    */
  override def generateNewEvent(location: Location, lastEvent: Option[WeatherEvent]): WeatherEvent = {
    val time = location.currentTime()
    val temp = estimateTemperature(location.position, time, lastEvent)
    val pressure = estimatePressure(location.position, time, temp)
    val humidity = estimateHumidity(location.position, time, pressure, temp)
    val condition = estimateCondition(location.position, temp, pressure, humidity)
    WeatherEvent(location, time, condition, temp, pressure, humidity)
  }
}

/**
  * Improvement to weathersim.SuperSimpleWeatherEstimator to smooth the estimations with the previous event
  */
object ImprovedWeatherEstimator extends WeatherEstimator with SimpleTemperatureEstimator with SimplePressureEstimator
  with SimpleHumidityEstimator with SimpleConditionEstimator {

  override def estimateTemperature(position: Position, time: LocalDateTime, lastEvent: Option[WeatherEvent]): Double = {
    val newTemp = super.estimateTemperature(position, time, lastEvent)
    lastEvent.map(prevEvent => (prevEvent.temperature + newTemp) / 2).getOrElse(newTemp)
  }

  override def generateNewEvent(location: Location, lastEvent: Option[WeatherEvent]): WeatherEvent = {
    val time = location.currentTime()
    val temp = estimateTemperature(location.position, time, lastEvent)
    val pressure = estimatePressure(location.position, time, temp)
    val humidity = estimateHumidity(location.position, time, pressure, temp)
    val condition = estimateCondition(location.position, temp, pressure, humidity)
    WeatherEvent(location, time, condition, temp, pressure, humidity)
  }
}

