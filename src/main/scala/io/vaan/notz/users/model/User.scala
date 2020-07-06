package io.vaan.notz.users.model

final case class User(
    firstName: String,
    lastName: String,
    email: String
)

final case class Users(users: Seq[User])
