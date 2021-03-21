package org.greengen.core

// Out of 5 stars
sealed trait StarRating
case object NoStar extends StarRating
case object OneStar extends StarRating
case object TwoStar extends StarRating
case object ThreeStar extends StarRating
case object FourStar extends StarRating
case object FiveStar extends StarRating

object Rating {
  
  def apply(rating: Int): Option[StarRating] = Option(rating).collect {
    case 0 => NoStar
    case 1 => OneStar
    case 2 => TwoStar
    case 3 => ThreeStar
    case 4 => FourStar
    case 5 => FiveStar
  }

  def zero() = NoStar
  def one() = OneStar
  def two() = TwoStar
  def three() = ThreeStar
  def four() = FourStar
  def five() = FiveStar
}