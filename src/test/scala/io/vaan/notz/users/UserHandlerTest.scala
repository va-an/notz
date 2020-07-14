package io.vaan.notz.users

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.http.scaladsl.testkit.ScalatestRouteTest
import io.vaan.notz.users.model.User
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}

class UserHandlerTest
  extends WordSpec
    with Matchers
    with ScalaFutures
    with ScalatestRouteTest {

  lazy val testKit: ActorTestKit = ActorTestKit()
  val userHandler = new UserHandler(testKit.system)
  val testEmail = "mailbox@vaan.io"

  "UserHandler" should {
    "create user" in {
      val user = userHandler.createOrUpdate(testEmail, User(
        email = testEmail,
        firstName = "vaan",
        lastName = "vy"
      )).futureValue

      user.firstName should === ("vaan")
    }

    "read user" in {
      val user = userHandler.read(testEmail).futureValue.get
      user.firstName should === ("vaan")
    }
  }
}
