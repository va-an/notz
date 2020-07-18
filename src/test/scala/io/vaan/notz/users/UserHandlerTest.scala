package io.vaan.notz.users

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}

// FIXME failed on start
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
      val user = userHandler.get(testEmail).futureValue.maybeUser.get
      user.firstName should === ("vaan")
    }
  }
}
