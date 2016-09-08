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

  val appId = Map("appId" -> "NIKEPLUSGPS")

  //Sync service parameters
  val syncUrl = "https://sync.plus.nikecloud.com"

  val syncAuth = "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzY3AiOiJ2My1zeW5jLXBheWxvYWQiLCJleHAiOjc0MDk4NDczMDUsInN1YiI6InYzLWxzcyIsImF1ZCI6WyJ2My1zeW5jLXNlcnZpY2UiXSwiaXNzIjoidjMtbHNzIn0.jOGoWH5JVXiDYAgcjXv3B6lWs4BT-Hd_-tno7emY90Q"

  var httpConf = http.baseURL(syncUrl)


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
      .header("Content-Type", "application/json; charset=ISO-8859-1")
      .header("USERID", "14986456963")
      .header("appid" , "NIKEPLUSGPS")
      .header("accept" , "*/*")
      .header("Authorization", "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJ1YXR0ZXN0YXBwIiwic2NwIjpbInJlYWQiLCJ3cml0ZSJdLCJzdWIiOiJ1YXR0ZXN0YXBwIiwiYXVkIjpbInYzLXN5bmMtc2VydmljZSJdLCJleHAiOjE3ODY4NzUxMjF9.VvAJIo0TfjWa2F0WeoneR3drApB8eRhHVK41_0KnQ4f9KSvVTAOeBBSzGs0tvoujAEkE5sv-eaBt7efIJ5Ao23vw8A2n7Dtgi4njJcWD6SJpLArgPfLUGd0XHdvaN-yI1Ap5PSWrhlELKXHNSPJudfv9PQ1pJrpd5lBpHFZOTD6NLt3A6neWuV2H-9zv6P2wzERMJ2HbuE-YPnppnZYRYMGWM8SSnUV-C6IMg1ZbkDG0TSkx4vMy3s_qB3fDTkEfqM-6MnQwPBnb6cCoBJLcorxxpnjNoW1vpdctGkmIS08iNhOhDMi-vt4cP3MvAYrgUQI2lD08iQ3vTVntT4-5cg")
      .header("X-Nike-AppId", "uattestapp")
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
