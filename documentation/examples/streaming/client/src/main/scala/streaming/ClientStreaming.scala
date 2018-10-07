package streaming

import endpoints.xhr
import monix.reactive.Observable
import monix.reactive.subjects.Var
import monix.execution.Scheduler.Implicits.global
import org.scalajs.dom.EventSource
import play.api.libs.json.{Json, OFormat, Reads}

import scala.scalajs.js

/**
  * Interpreter for the [[Streaming]] algebra interface, targeting web browsers.
  */
trait ClientStreaming extends Streaming with xhr.faithful.Endpoints {

  // From web browsers we can not really use the chunked transfer encoding
  type Chunks[A] = String
  def chunkedResponse[A]: Response[Chunks[A]] = textResponse()

  // Ideally we would return an org.reactivestreams.Publisher, but
  // we are blocked by https://github.com/reactive-streams/reactive-streams-scalajs/pull/1
  /** A server-sent events endpoint is a function that takes an `A` as parameter and returns an `Observable[B]` */
  type SseEndpoint[A, B] = js.Function1[A, Observable[B]]

  type EventCodec[A] = Reads[A]

  def sseEndpoint[A, B](url: Url[A], codec: EventCodec[B]): SseEndpoint[A, B] = { a =>
    val es = new EventSource(url.encode(a))
    val lastMessage = Var(Option.empty[B])
    // TODO Report failures
    es.onmessage = message => lastMessage := Some(Json.parse(message.data.toString).as(codec))
    lastMessage.collect { case Some(b) => b }
  }

  def jsonEventSource[A](implicit oformat: OFormat[A]): EventCodec[A] = oformat

}

object Client extends Usage with ClientStreaming

object Main {
  def main(args: Array[String]): Unit = {
    // Subscribe to the notifications and print them to the console
    val _ = Client.notifications(()).foreach { counter =>
      println(counter.value)
    }
  }
}