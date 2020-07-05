package io.vaan.notz.utils

import io.vaan.notz.UserRegistry.ActionPerformed
import io.vaan.notz.model.{User, Users}
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

object JsonFormats  {
  // import the default encoders for primitive types (Int, String, Lists etc)
  import DefaultJsonProtocol._

  implicit val userJsonFormat: RootJsonFormat[User] =
    jsonFormat3(User)

  implicit val usersJsonFormat: RootJsonFormat[Users] =
    jsonFormat1(Users)

  implicit val actionPerformedJsonFormat: RootJsonFormat[ActionPerformed] =
    jsonFormat1(ActionPerformed)
}
