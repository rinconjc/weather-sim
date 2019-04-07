import java.lang.Math.{max, min}

/**
  * @author julio on 7/04/19.
  */
package object weathersim {

  /**
    * Returns the value or the min and max values, if the value is outside of the range
    * @param value
    * @param minVal
    * @param maxVal
    * @return
    */
  def withinRange(value: Double, minVal: Double, maxVal: Double) = min(max(value, minVal), maxVal)
}
