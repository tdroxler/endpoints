package endpoints
package openapi

import endpoints.algebra.Documentation
import endpoints.openapi.model.MediaType

/**
  * Interpreter for [[algebra.Responses]]
  *
  * @group interpreters
  */
trait Responses
  extends algebra.Responses with StatusCodes {

  type Response[A] = List[DocumentedResponse]
  type ResponseEntity[A] = DocumentedResponseEntity

  /**
    * @param status Response status code (e.g. 200)
    * @param documentation Human readable documentation. Not optional because its required by openapi
    * @param content Map that associates each possible content-type (e.g. “text/html”) with a [[MediaType]] description
    */
  case class DocumentedResponse(status: Int, entity: DocumentedResponseEntity) {
    def documentation = entity.documentation
    def content = entity.content
  }
  object DocumentedResponse {
    def apply(status: Int, documentation: String, content: Map[String, MediaType]): DocumentedResponse =
      DocumentedResponse(status, DocumentedResponseEntity(documentation, content))

  }

  case class DocumentedResponseEntity(documentation: String, content: Map[String, MediaType])

  def response[A](statusCode: StatusCode, entity: ResponseEntity[A]): Response[A] = DocumentedResponse(statusCode, entity) :: Nil

  def emptyResponse(docs: Documentation): ResponseEntity[Unit] = DocumentedResponseEntity(docs.getOrElse(""), Map.empty)

  def textResponse(docs: Documentation): ResponseEntity[String] = DocumentedResponseEntity(docs.getOrElse(""), Map("text/plain" -> MediaType(None)))

  def wheneverFound[A](response: Response[A], notFoundDocs: Documentation): Response[Option[A]] =
    DocumentedResponse(NotFound, notFoundDocs.getOrElse(""), content = Map.empty) :: response

  def choiceResponse[A, B](respA: Response[A], respB: Response[B]): Response[Either[B, A]] = respA ++ respB

   override def wheneverValid[A,E<:WithStatusCode](response: Response[A])(errorEntity: ResponseEntity[E], notValidDocs: List[StatusCode]): Response[Either[E, A]] =
     notValidDocs.map(statusCode => DocumentedResponse(statusCode, errorEntity)) ++ response

  override def enumResponse[A <: WithStatusCode](responseEntity: ResponseEntity[A], enum: enumeratum.Enum[A]): Response[A] ={
    enum.values.toList.flatMap(a =>response(a.statusCode, responseEntity))
  }


}
