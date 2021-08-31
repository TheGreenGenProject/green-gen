package org.greengen.core

import org.greengen.core.Coordinate.LatLong


sealed trait Location
case class Online(url: Url) extends Location
case class MapUrl(url: Url) extends Location
case class GeoLocation(coordinates: LatLong) extends Location
case class Address(
  address: Option[String],
  zipCode: Option[String],
  country: Country,
) extends Location


case class Country(name: String)
object Country {
  val World = Country("World")
  val UnitedKingdom = Country("United Kingdom")
}