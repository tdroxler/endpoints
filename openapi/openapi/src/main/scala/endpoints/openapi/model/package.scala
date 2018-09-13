package endpoints.openapi

package object model {

  // Convenient way to optionally add elements to a list
  implicit class OptionalOps[A](val `this`: List[A]) extends AnyVal {
    def +?: [B >: A](maybeElem: Option[B]): List[B] =
      maybeElem.fold[List[B]](`this`)(_ +: `this`)
  }

}
