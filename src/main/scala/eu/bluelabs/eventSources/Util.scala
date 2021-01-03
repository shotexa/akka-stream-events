package eu.bluelabs
package eventSources

import akka.NotUsed
import akka.stream.scaladsl._
import akka.util.ByteString
import scala.concurrent.duration._

import entities._
import util.Random

object Util {

  def randomize[T]: Flow[T, T, NotUsed] = Flow[T]
    .fold[List[T]](Nil)((acc, v) => v :: acc)
    .map(Random.shuffle(_))
    .mapConcat(identity)

  def splitByNewLine: Flow[ByteString, ByteString, NotUsed] =
    Framing.delimiter(ByteString("\n"), Int.MaxValue, true)

  def delayForEachBetween[T](from: Int, to: Int): Flow[T, T, NotUsed] =
    Flow[T].throttle(from, from.milliseconds, _ => Random.nextInt(to) + 1)
}
