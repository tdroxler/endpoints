package endpoints.algebra

/**
  * @group algebras
  */
trait StatusCodes {
  /** HTTP Satus Code */
  type StatusCode

  def OK: StatusCode

  def NotFound: StatusCode

  def BadRequest: StatusCode

  def Forbidden: StatusCode

  def Unauthorized: StatusCode

}
