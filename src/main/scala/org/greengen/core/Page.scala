package org.greengen.core

case class Page(n: Int, by: Int) {
  def next = Page(n+1, by)
}

object Page {
  // Hack to be able to retrieve all data in one go from paged queries
  val All = Page(1, Int.MaxValue)
}

case class PagedResult[T](result: T, page: Page, last: Boolean) {
  def next = page.next
}

object PagedResult {

  def page[T](xs: List[T], page: Page): List[T] =
    xs.drop((page.n-1) * page.by).take(page.by)

  def page[T](xs: IndexedSeq[T], page: Page): IndexedSeq[T] =
    xs.drop((page.n-1) * page.by).take(page.by)

}