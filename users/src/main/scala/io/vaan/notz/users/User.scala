package io.vaan.notz.users

import io.vaan.notz.users.UserActor.UserState

final case class User(
    firstName: String,
    lastName: String,
    email: String
)

object User {
  def apply(state: UserState): User = User(
    email = state.email,
    firstName = state.firstName,
    lastName = state.lastName
  )
}

final case class Users(users: Seq[User])
