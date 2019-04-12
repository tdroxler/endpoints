package endpoints.algebra

import scala.language.higherKinds

/**
  * @group algebras
  */
trait Responses extends StatusCodes {

  /** Information carried by a response */
  type Response[A]

  type ResponseEntity[A]

  trait EnumEntry {
    def response[A]: Response[A]
  }
  trait Enum[A] {
    val responses: List[A]
  }
  sealed trait MyError extends EnumEntry
  object MyError extends Enum[MyError] {
    //If user forgot one error in the list it's his fault!!!
    val responses = List(Error1.response, Error2.response)

    case class Error1(message: String)
    object Error1 extends MyError {
      def response = response(BadRequest, jsonResponse[Error1])
    }
    case class Error2(message: String, nb: Int)
    object Error2 extends MyError {
      def statusCode = response(NotFound, jsonResponse[Error2])
    }
  }

  def response[A](statusCode: StatusCode, entity: ResponseEntity[A]): Response[A]

  def ok[A](entity: ResponseEntity[A]): Response[A] = response[A](OK, entity)

  def enumResponse[A <: Enum](responseEntity: ResponseEntity[A]): Response[A]
  /**
    * Empty response.
    */
  def emptyResponse(docs: Documentation = None): ResponseEntity[Unit]

  /**
    * Text response.
    */
  def textResponse(docs: Documentation = None): ResponseEntity[String]

  /**
    * Turns a `ResponseEntity[A]` into a `ResponseEntity[Option[A]]`.
    *
    * Concrete interpreters should represent `None` with
    * an empty HTTP response whose status code is 404 (Not Found).
    */
  def wheneverFound[A](response: Response[A], notFoundDocs: Documentation = None): Response[Option[A]]

  def choiceResponse[A, B](respA: Response[A], respB: Response[B]): Response[Either[A, B]]

  def wheneverValid[A,E<:WithStatusCode](response: Response[A])(errorEntity: ResponseEntity[E], notValidDocs: List[StatusCode] = List.empty): Response[Either[E, A]]

  /** Extensions for [[Response]]. */
  implicit class ResponseExtensions[A](response: Response[A]) {
    /** syntax for `wheneverFound` */
    final def orNotFound(notFoundDocs: Documentation = None): Response[Option[A]] = wheneverFound(response, notFoundDocs)
  }

}
