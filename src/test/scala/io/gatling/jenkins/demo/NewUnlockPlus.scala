package simulations.syncservice

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import scala.concurrent.forkjoin.ThreadLocalRandom
import scala.language.postfixOps
import scala.math._

class NewUnlockPlus extends Simulation {

  var startEpoch = 0L;
  var stopEpoch = 0L;

  def startGenerator() :Long = {
    var startNum = 1396024675000L + ThreadLocalRandom.current().nextLong(90000L)
    startEpoch = startNum;
    return startNum
  }
  def stopGenerator() :Long = {
    var stopNum = 1396335695000L + ThreadLocalRandom.current().nextLong(90000L)
    stopEpoch = stopNum;
    return stopNum
  }


  val postActivity = scenario("POST Activity")
    .exec(http("POST activity post")
      .post("/activity/")
      .body(StringBody(session =>
        s"""
           |{
           |    "id": "activityId",
           |    "type": "run",
           |    "start_epoch_ms": "${startGenerator()}",
           |    "end_epoch_ms": "${stopGenerator()}",
           |    "metrics": [
           |        {
           |            "type": "distance",
           |            "unit": "KM",
           |            "source": "nike.running.ios",
           |            "values": [
           |                {
           |                    "start_epoch_ms": "${startEpoch}",
           |                    "end_epoch_ms": "${stopEpoch}",
           |                    "value": 2.0
           |                }
           |
            |            ]
           |        }
           |    ]
           |}
          """.stripMargin)).asJSON
      .check(status.is(202))
      .check(
        jsonPath("$.activityId").saveAs("activityId")
      )
      .check(bodyString.transform(_.split("\"")(3)).saveAs("changeToken"))
    )
    .exec(
      session => {
        val activityId = session.get("activityId").asOption[String]
        println(activityId)
        session
      }
    )

  setUp(
    postActivity.inject
    (nothingFor(2 seconds),
      atOnceUsers(30),
      rampUsers(200) over(1000 seconds))
      .protocols(httpConf))
}
