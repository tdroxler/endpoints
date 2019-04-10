package endpoints.akkahttp.server

import akka.http.scaladsl.model
import endpoints.algebra

/**
  * [[algebra.StatusCodes]] interpreter that decodes and encodes methods.
  *
  * @group interpreters
  */
trait StatusCodes extends algebra.StatusCodes {

  type StatusCode = model.StatusCode

  def OK = model.StatusCodes.OK

  def NotFound = model.StatusCodes.NotFound

  def BadRequest = model.StatusCodes.BadRequest

  def Forbidden = model.StatusCodes.Forbidden

  def Unauthorized = model.StatusCodes.Unauthorized

}
