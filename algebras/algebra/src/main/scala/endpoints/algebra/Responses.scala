package endpoints.algebra

import scala.language.higherKinds

/**
  * @group algebras
  */
trait Responses extends StatusCodes {

  /** Information carried by a response */
  type Response[A]

  type ResponseEntity[A]

  trait WithStatusCode {
    def statusCode: StatusCode
  }

  def ok[A](entity: ResponseEntity[A]): Response[A]

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


  def wheneverValid[A,E<:WithStatusCode](response: Response[A])(errorEntity: ResponseEntity[E], notValidDocs: List[StatusCode] = List.empty): Response[Either[E, A]]

  /** Extensions for [[Response]]. */
  implicit class ResponseExtensions[A](response: Response[A]) {
    /** syntax for `wheneverFound` */
    final def orNotFound(notFoundDocs: Documentation = None): Response[Option[A]] = wheneverFound(response, notFoundDocs)
  }

}
