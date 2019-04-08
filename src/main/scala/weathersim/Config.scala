package weathersim

import com.typesafe.config.ConfigFactory

/**
  * @author julio on 7/04/19.
  */
object Config {

  private lazy val config = ConfigFactory.load()

  /**
    * The locations file
    * @return
    */
  def locationsFile = config.getString("locations-file")

  /**
    *
    * @return
    */
  lazy val timeScale = config.getInt("time-scale")

}
