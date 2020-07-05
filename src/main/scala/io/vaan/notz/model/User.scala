package io.vaan.notz.model

final case class User(
    firstName: String,
    lastName: String,
    email: String
)

final case class Users(users: Seq[User])
