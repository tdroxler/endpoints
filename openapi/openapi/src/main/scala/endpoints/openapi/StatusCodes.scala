package endpoints
package openapi

/**
  * Interpreter for [[endpoints.algebra.StatusCodes]]
  *
  * @group interpreters
  */
trait StatusCodes extends endpoints.algebra.StatusCodes {

  type StatusCode = Int

  def OK: StatusCode = 200

  def BadRequest: StatusCode = 400

  def Unauthorized: StatusCode = 401

  def Forbidden: StatusCode = 403

  def NotFound: StatusCode = 404

}
