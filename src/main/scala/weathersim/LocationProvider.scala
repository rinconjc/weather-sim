package weathersim

import java.time.LocalDateTime

import weathersim.Config.locationsFile

import scala.io.Source
import scala.util.Random

/**
  * @author julio on 6/04/19.
  */
object LocationProvider {

  val EARTH_RADIOUS = 6371 //https://en.wikipedia.org/wiki/Earth_radius

/*
  def longToX(long: Float, width: Int): Int = ((long + 180.0) * width / 360.0).toInt

  def latToY(lat: Float, height: Int): Int = ((90.0 - lat) * height / 180.0).toInt

  def locations(topoFile: String): Stream[Location] = {
    val img = ImageIO.read(new File(topoFile))
    val (maxX, maxY) = (img.getWidth(), img.getHeight())
    Stream()
  }
*/

  def randomLocations: Iterator[Location] = {
    val lines = Source.fromFile(locationsFile).getLines()
    Random
      .shuffle(lines)
      .map(_.split(';'))
      .map(cols => Location(cols(1) + "/" + cols(2), Position(cols(3).toDouble, cols(4).toDouble, cols(5).toDouble), cols(6).toInt))
  }

}

case class Position(lat: Double, lon: Double, elev: Double) {
  private def toRads(degrees: Double) = degrees * Math.PI / 180

  def distance(other: Position): Double = {
    //Derived from https://en.wikipedia.org/wiki/Haversine_formula
    val latDistInRads = toRads(other.lat - lat)
    val longDistInRads = toRads(other.lon - lon)
    val harv = Math.pow(Math.sin(latDistInRads / 2), 2) +
      Math.cos(toRads(lat)) * Math.cos(toRads(other.lat)) *
        Math.pow(Math.sin(longDistInRads / 2), 2)

    2 * LocationProvider.EARTH_RADIOUS * Math.asin(Math.sqrt(harv))
  }

  override def toString: String = s"$lat,$lon,$elev"
}

case class Location(name: String, position: Position, tzOffset: Int) {

  def distance(other: Location):Double = position.distance(other.position)

  def currentTime(): LocalDateTime = LocalDateTime.now().plusSeconds(tzOffset)
}
