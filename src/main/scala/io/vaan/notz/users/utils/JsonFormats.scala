package io.vaan.notz.users.utils

import io.vaan.notz.users.{User, Users}
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

object JsonFormats  {
  // import the default encoders for primitive types (Int, String, Lists etc)
  import DefaultJsonProtocol._

  implicit val userJsonFormat: RootJsonFormat[User] =
    jsonFormat3(User.apply)

  implicit val usersJsonFormat: RootJsonFormat[Users] =
    jsonFormat1(Users)
}
