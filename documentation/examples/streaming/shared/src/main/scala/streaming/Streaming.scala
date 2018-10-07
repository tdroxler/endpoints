package streaming

import scala.language.higherKinds

import endpoints.algebra
import play.api.libs.json.{Json, OFormat}

/**
  * Algebra interface for describing endpoints whose responses are streamed.
  */
trait Streaming extends algebra.Endpoints {

  /** A response that uses the Chunked transfer encoding.
    * Each individual chunk contains a value of type `A` */
  def chunkedResponse[A]: Response[Chunks[A]]

  /** Type of chunked response */
  type Chunks[A]

  /** A server-sent events endpoint: the response is a stream of `B` values */
  def sseEndpoint[A, B](url: Url[A], codec: EventCodec[B]): SseEndpoint[A, B]
  /** A server-sent events endpoint taking an `A` value as a request, and producing a stream of `B` values as a response */
  type SseEndpoint[A, B]
  /** A codec encoding and decoding events of type `A` */
  type EventCodec[A]

  /** An event source that uses JSON as the underlying transport format */
  // Ideally we should provide an extensible serialization mechanism, like we do for path segments
  def jsonEventSource[A](implicit oformat: OFormat[A]): EventCodec[A]

  // TODO bidirectional endpoints with websockets

}

// --- Example of use ---

case class Counter(value: Int)

object Counter {
  implicit val oformat: OFormat[Counter] = Json.format[Counter]
}

trait Usage extends Streaming {

  val notifications = sseEndpoint(
    path / "notifications",
    jsonEventSource[Counter]
  )

}
