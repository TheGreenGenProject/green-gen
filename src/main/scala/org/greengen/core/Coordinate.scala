package org.greengen.core

object Coordinate {

  // Coordinates
  case class Latitude(value: Double)
  case class Longitude(value: Double)
  case class Altitude(value: Double)

  case class LatLong(latitude: Latitude, longitude: Longitude) {
    def asPair = (latitude, longitude)
  }

  case class LatLongAlt(latitude: Latitude, longitude: Longitude, altitude: Altitude) {
    def asTriplet = (latitude, longitude, altitude)
  }

  // Cartesian coordinates
  case class XYZ(x: Double, y: Double, z: Double)


  // Angles

  case class Degree(value: Double) extends AnyVal
  case class Radian(value: Double) extends AnyVal

  implicit def d2r(deg: Degree) = Radian(deg.value * math.Pi/180.0)

  implicit def r2d(rad: Radian) = Degree(180.0 * rad.value / math.Pi)

  implicit def latToDeg(lat: Latitude) = Degree(lat.value)

  implicit def longToDeg(long: Longitude) = Degree(long.value)

}
