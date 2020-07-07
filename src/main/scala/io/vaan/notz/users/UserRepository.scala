package io.vaan.notz.users

import io.vaan.notz.users.model.{ User, Users }

trait UserRepository {
  def create(user: User): Unit
  def findAll: Users
  def findByEmail(email: String): Option[User]
  def deleteByEmail(email: String): Unit
}

object InMemoryUserRepo extends UserRepository {
  private var users: Seq[User] = Seq.empty

  override def create(user: User): Unit =
    users = users :+ user

  override def findAll: Users = Users(users)

  override def findByEmail(email: String): Option[User] =
    users.find(_.email == email)

  override def deleteByEmail(email: String): Unit =
    users = users.filterNot(_.email == email)
}
