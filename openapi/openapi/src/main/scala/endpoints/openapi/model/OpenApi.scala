package endpoints.openapi.model

import io.circe.syntax._
import io.circe.{Json, JsonObject, ObjectEncoder}

/**
  * @see [[https://github.com/OAI/OpenAPI-Specification/blob/OpenAPI.next/versions/3.0.md]]
  */
case class OpenApi(
  info: Info,
  paths: Map[String, PathItem]
)

object OpenApi {

  implicit val jsonEncoder: ObjectEncoder[OpenApi] =
    ObjectEncoder.instance { openApi =>
      JsonObject.fromMap(Map(
        "openapi" -> Json.fromString("3.0.0"),
        "info" -> Json.obj(
          "title" -> Json.fromString(openApi.info.title),
          "version" -> Json.fromString(openApi.info.version)
        ),
        "paths" -> Json.fromFields(openApi.paths.to[List].map { case (path, item) => (path, item.asJson) })
      ))
    }

}

case class Info(
  title: String,
  version: String
)

case class PathItem(
  operations: Map[String, Operation]
)

object PathItem {

  implicit val jsonEncoder: ObjectEncoder[PathItem] =
    ObjectEncoder.instance { item =>
      JsonObject.fromIterable(item.operations.to[List].map { case (verb, op) => (verb, op.asJson) })
    }

}

case class Operation(
  summary: Option[String],
  description: Option[String],
  parameters: List[Parameter],
  requestBody: Option[RequestBody],
  responses: Map[Int, Response],
  tags: List[String]
)

object Operation {

  implicit val jsonEncoder: ObjectEncoder[Operation] =
    ObjectEncoder.instance { op =>
      val optFields = List(
        op.summary.map(x => "summary" -> x.asJson),
        op.description.map(x => "description" -> x.asJson),
        op.requestBody.map(x => "requestBody" -> x.asJson),
        op.tags.headOption.map(_ => "tags" -> op.tags.asJson)
      ).flatten
      val fields =
        "parameters" -> Json.fromValues(op.parameters.map(_.asJson)) ::
          (
            "responses" -> Json.fromFields(
              op.responses.to[List].map { case (status, resp) =>
                status.toString -> Json.fromFields(
                  "description" -> Json.fromString(resp.description) ::
                    (if (resp.content.nonEmpty) {
                      "content" -> MediaType.jsonMediaTypes(resp.content) ::
                        Nil
                    } else Nil)
                )
              }
            )
            ) ::
          optFields

      JsonObject.fromIterable(fields)
    }

}

case class RequestBody(
  description: Option[String],
  content: Map[String, MediaType]
) {
  assert(content.nonEmpty)
}

object RequestBody {

  implicit val jsonEncoder: ObjectEncoder[RequestBody] =
    ObjectEncoder.instance { requestBody =>
      JsonObject.fromIterable({
        val requiredFields =
          "content" -> MediaType.jsonMediaTypes(requestBody.content) ::
            Nil
        requestBody.description.fold(requiredFields)(d => "description" -> Json.fromString(d) :: requiredFields)
      })
    }

}

case class Response(
  description: String,
  content: Map[String, MediaType]
)

case class Parameter(
  name: String,
  in: In,
  required: Boolean,
  description: Option[String],
  schema: Schema // not specified in openapi spec but swagger-editor breaks without it for path parameters
)

object Parameter {

  implicit val jsonEncoder: ObjectEncoder[Parameter] =
    ObjectEncoder.instance { parameter =>
      val fields =
        "name" -> Json.fromString(parameter.name) ::
          "in" -> Json.fromString(parameter.in match {
            case In.Cookie => "cookie"
            case In.Header => "header"
            case In.Path => "path"
            case In.Query => "query"
          }) ::
          "schema" -> parameter.schema.asJson ::
          List(
            parameter.description.map(s => "description" -> Json.fromString(s))
          ).flatten
      val maybeRequiredField = if (parameter.required) Some("required" -> Json.fromBoolean(true)) else None
      JsonObject.fromIterable(maybeRequiredField +?: fields)
    }

}

sealed trait In

object In {

  case object Query extends In

  case object Path extends In

  case object Header extends In

  case object Cookie extends In

}

case class MediaType(schema: Option[Schema]/*, example: Option[Json]*/)

object MediaType {

  def jsonMediaTypes(mediaTypes: Map[String, MediaType]): Json =
    Json.fromFields(mediaTypes.map { case (tpe, mediaType) =>
      val maybeSchemaField = mediaType.schema.map(schema => ("schema", schema.asJson))
      // val maybeExampleField = mediaType.example.map(example => ("example", example))
      tpe -> Json.fromFields(maybeSchemaField +?: /*maybeExampleField +?: */Nil)
    })

}

sealed trait Schema

object Schema {

  case class Object(properties: List[Property], description: Option[String]) extends Schema

  case class Array(elementType: Schema) extends Schema

  case class Property(name: String, schema: Schema, isRequired: Boolean, description: Option[String])

  case class Primitive(name: String) extends Schema

  case class OneOf(alternatives: List[Schema], description: Option[String]) extends Schema

  val simpleString = Primitive("string")
  val simpleInteger = Primitive("integer")

  implicit val jsonEncoder: ObjectEncoder[Schema] =
    ObjectEncoder.instance {
      case Primitive(name) => JsonObject.singleton("type", Json.fromString(name))
      case Array(elementType) =>
        JsonObject.fromIterable(
          "type" -> Json.fromString("array") ::
            "items" -> jsonEncoder.apply(elementType) ::
            Nil
        )
      case Object(properties, description) =>
        val fields =
          "type" -> Json.fromString("object") ::
            "properties" -> Json.fromFields(
              properties.map { property =>
                val maybeDescriptionField = property.description.map(s => ("description", Json.fromString(s)))
                property.name -> Json.fromFields(maybeDescriptionField +?: jsonEncoder.encodeObject(property.schema).toList)
              }
            ) ::
            Nil
        val maybeDescriptionField =
          description.map(s => "description" -> Json.fromString(s))
        val requiredProperties = properties.filter(_.isRequired)
        val maybeRequiredField =
          if (requiredProperties.isEmpty) None
          else Some("required" -> Json.arr(requiredProperties.map(p => Json.fromString(p.name)): _*))
        JsonObject.fromIterable(maybeRequiredField +?: maybeDescriptionField +?: fields)
      case OneOf(alternatives, description) =>
        val fields =
            "oneOf" -> Json.fromValues(alternatives.map(jsonEncoder.apply)) ::
            Nil
        val maybeDescriptionField = description.map(s => "description" -> Json.fromString(s))
        JsonObject.fromIterable(maybeDescriptionField +?: fields)
    }

}
