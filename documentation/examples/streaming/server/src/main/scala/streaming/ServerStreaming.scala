package streaming

import akka.stream.scaladsl.Source
import controllers.{Assets, AssetsConfiguration, DefaultAssetsMetadata}
import endpoints.play.server
import endpoints.play.server.{DefaultPlayComponents, PlayComponents}
import play.api.http.ContentTypes.HTML
import play.api.http.Writeable
import play.api.libs.EventSource
import play.api.libs.json.{JsValue, OFormat}
import play.api.mvc.{Call, Handler, RequestHeader, Results}
import play.api.routing.Router
import play.core.server.{NettyServer, ServerConfig}

import scala.concurrent.duration.DurationInt

/**
  * Interpreter for the [[Streaming]] algebra interface, using Play framework
  */
trait ServerStreaming extends Streaming with server.Endpoints {

  class Chunks[A](val source: Source[A, _])(implicit val writeable: Writeable[A])

  // TODO Pull up something similar to Writeable at the algebra level
  // to enforce consistency between server encoding and client decoding
  def chunks[A : Writeable](source: Source[A, _]): Chunks[A] = new Chunks(source)

  def chunkedResponse[A]: Response[Chunks[A]] = { stream =>
    Results.Ok.chunked(stream.source)(stream.writeable)
  }

  type EventCodec[A] = Source[A, _] => Source[EventSource.Event, _]

  case class SseEndpoint[A, B](url: Url[A], codec: EventCodec[B]) {

    def call(a: A): Call = Call("GET", url.encodeUrl(a))

    def implementedBy(f: A => Source[B, _]): ToPlayHandler =
      new ToPlayHandler {
        def playHandler(header: RequestHeader): Option[Handler] =
          url.decodeUrl(header).map { a =>
            playComponents.actionBuilder {
              Results.Ok.chunked(codec(f(a)))
            }
          }
      }

  }

  def sseEndpoint[A, B](url: Url[A], source: EventCodec[B]): SseEndpoint[A, B] =
    SseEndpoint(url, source)

  def jsonEventSource[A](implicit oformat: OFormat[A]): EventCodec[A] =
    (source: Source[A, _]) => source.map(oformat.writes).via(EventSource.flow[JsValue])

}

class Server(val playComponents: PlayComponents) extends Usage with ServerStreaming {

  val routes = routesFromEndpoints(
    notifications.implementedBy { _ =>
      Source.tick(1.second, 1.second, ())
        .scan(0)((n, _) => n + 1)
        .map(Counter(_))
    }
  )

}

object Main {
  def main(args: Array[String]): Unit = {
    val playConfig = ServerConfig()
    val playComponents = new DefaultPlayComponents(playConfig)
    val server = new Server(playComponents)
    val assetsMetadata = new DefaultAssetsMetadata(playComponents.environment, AssetsConfiguration.fromConfiguration(playComponents.configuration), playComponents.fileMimeTypes)
    val assets = new Assets(playComponents.httpErrorHandler, assetsMetadata)
    import _root_.play.api.routing.sird._
    val assetsRoutes = Router.from {
      case GET(p"/assets/$file*") => assets.at("/", file)
      case GET(p"/") => playComponents.actionBuilder {
        val html =
          """
            |<!DOCTYPE html>
            |<html>
            |  <head>
            |  </head>
            |  <body>
            |    <script src="/assets/app.js"></script>
            |  </body>
            |</html>
          """.stripMargin
        Results.Ok(html).as(HTML)
      }

    }
    val _ = NettyServer.fromRouter(ServerConfig())(server.routes orElse assetsRoutes.routes)
  }
}
