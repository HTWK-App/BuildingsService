import scala.concurrent.Future

import org.junit.runner.RunWith
import org.specs2.matcher.MatchResult
import org.specs2.matcher.ValueCheck.valueIsTypedValueCheck
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.GET
import play.api.test.Helpers.NOT_FOUND
import play.api.test.Helpers.OK
import play.api.test.Helpers.charset
import play.api.test.Helpers.contentAsJson
import play.api.test.Helpers.contentAsString
import play.api.test.Helpers.contentType
import play.api.test.Helpers.defaultAwaitTimeout
import play.api.test.Helpers.route
import play.api.test.Helpers.status
import play.api.test.Helpers.writeableOf_AnyContentAsEmpty
import play.api.test.WithApplication

@RunWith(classOf[JUnitRunner])
class RestSpec extends Specification {

  "Application" should {

    "respond with NotFound on a bad request" in new WithApplication {
      Seq("application/json", "application/rdf+xml", "text/turtle", "text/n3").map {
        ContentType =>
          val result = controllers.v1.buildingDetails("slkdnfksdhbfi")(FakeRequest().withHeaders("Accept" -> ContentType))
          status(result) must equalTo(NOT_FOUND)
      } :+ (route(FakeRequest(GET, "/boum")) must beNone)
    }

    "respond with JSON (utf-8) by default" in {
      val ContentType = "application/json"

      idList.map { id =>
        checkDefaults(ContentType, controllers.v1.buildingDetails(id)(FakeRequest()))
      } :+ checkDefaults(ContentType, controllers.v1.buildings()(FakeRequest()))
    }

    "respond with RDF/XML (utf-8) by request" in {
      val ContentType = "application/rdf+xml"

      idList.map { id =>
        checkDefaults(ContentType, controllers.v1.buildingDetails(id)(FakeRequest().withHeaders("Accept" -> ContentType)))
      } :+ checkDefaults(ContentType, controllers.v1.buildings()(FakeRequest().withHeaders("Accept" -> ContentType)))
    }

    "respond with Turtle (utf-8) by request" in {
      val ContentType = "text/turtle"

      idList.map { id =>
        checkDefaults(ContentType, controllers.v1.buildingDetails(id)(FakeRequest().withHeaders("Accept" -> ContentType)))
      } :+ checkDefaults(ContentType, controllers.v1.buildings()(FakeRequest().withHeaders("Accept" -> ContentType)))
    }

    "respond with N3 (utf-8) by request" in {
      val ContentType = "text/n3"

      idList.map { id =>
        checkDefaults(ContentType, controllers.v1.buildingDetails(id)(FakeRequest().withHeaders("Accept" -> ContentType)))
      } :+ checkDefaults(ContentType, controllers.v1.buildings()(FakeRequest().withHeaders("Accept" -> ContentType)))
    }

    "contain all elements in JSON responses" in {
      idList.map { id =>
        val result = controllers.v1.buildingDetails(id)(FakeRequest())
        checkContains(result)
      } :+ (checkContains(controllers.v1.buildings()(FakeRequest())))
    }

    /*
     * @TODO Test for right JSON structure
     */

    "respond with equal data in BuildingsList and BuildingDetails for JSON" in {
      val result = controllers.v1.buildings()(FakeRequest())

      idList.map { id =>
        val detailResult = controllers.v1.buildingDetails(id)(FakeRequest())

        contentAsString(result) must contain(contentAsString(detailResult))
      }
    }

    /*
     * @TODO Test "respond with equal data in BuildingsList and BuildingsDetail for XYZ" for RDF/XML, Turtle, N3
     */
  }

  def checkDefaults(ContentType: String, result: Future[Result]) = {
    status(result) must equalTo(OK)
    charset(result) must beSome("utf-8")
    contentType(result) must beSome(ContentType)
  }

  def checkContains(result: Future[Result]): MatchResult[String] = {
    (contentAsString(result) must
      contain(""""id":""") contain (""""fullName":""") contain (""""detailLink":""") contain (""""description":""")
      contain (""""latLng":""") contain (""""address":""") contain (""""pictureLink":""") contain (""""pictureData":""")
      contain (""""lastChange":"""))
  }

  def idList(): Seq[String] = {
    val json = contentAsJson(controllers.v1.buildings()(FakeRequest()))

    (json \\ "id").map(_.toString().replace(""""""", ""))
  }
}
