package rself

object TestList {
  import scala.collection.mutable.ListBuffer
  def apply(): ListBuffer[Long] = {
    val ref: ListBuffer[Long] = ListBuffer()
    ref
  }
}

trait TestUtils {
  val rnd = new scala.util.Random(System.currentTimeMillis)
  def random32() = {rnd.nextLong() & ((1L<<32)-1)}
}