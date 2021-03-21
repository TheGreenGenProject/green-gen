package org.greengen.core.like

case class Like private(value: Int) {
  def increment() =
    if(value==Int.MaxValue) this else Like(value + 1)

  def decrement() =
    Like(math.max(0,value - 1))
}

object Like {

  val NoLike = new Like(0)

  def apply() = NoLike
  def apply(value: Int): Like = value match {
    case 0          => NoLike
    case n if n > 0 => new Like(value)
    case _          => NoLike
  }
}


