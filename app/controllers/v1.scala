package controllers

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Accepting
import play.api.mvc.Action
import play.api.mvc.Controller
import resources.ResultGenerator.calcDistancesAs
import resources.ResultGenerator.getBuildingDetailsResult
import resources.ResultGenerator.getBuildingsResult

object v1 extends Controller {

  val JSON = "application/json"
  val RDF = "application/rdf+xml"
  val Turtle = "text/turtle"
  val N3 = "text/n3"
  val ContentTypeEnding = "; charset=utf-8"

  val AcceptsRDFXML = Accepting(RDF)
  val AcceptsTurtle = Accepting(Turtle)
  val AcceptsN3 = Accepting(N3)

  /**
   * Get all buildings as a list
   *
   * @return Result as List or InternalServerError, if an Exception was thrown
   */
  def buildings = Action.async { implicit request =>
    scala.concurrent.Future {
      render {
        case Accepts.Json() => getBuildingsResult(JSON)
        case AcceptsRDFXML() => getBuildingsResult(RDF).withHeaders("Content-Type" -> (RDF + ContentTypeEnding))
        case AcceptsTurtle() => getBuildingsResult(Turtle).withHeaders("Content-Type" -> (Turtle + ContentTypeEnding))
        case AcceptsN3() => getBuildingsResult(N3).withHeaders("Content-Type" -> (N3 + ContentTypeEnding))
      }
    }
  }

  /**
   * Get details for a specific building
   *
   * @param key The key inside the BuildingsMap
   *
   * @return Result or NotFound
   */
  def buildingDetails(key: String) = Action.async { implicit request =>
    scala.concurrent.Future {
      render {
        case Accepts.Json() => getBuildingDetailsResult(key, JSON)
        case AcceptsRDFXML() => getBuildingDetailsResult(key, RDF).withHeaders("Content-Type" -> (RDF + ContentTypeEnding))
        case AcceptsTurtle() => getBuildingDetailsResult(key, Turtle).withHeaders("Content-Type" -> (Turtle + ContentTypeEnding))
        case AcceptsN3() => getBuildingDetailsResult(key, N3).withHeaders("Content-Type" -> (N3 + ContentTypeEnding))
      }
    }
  }

  /**
   * Get distances to all buildings by position
   *
   * @param poition Geolocation, ex: 51.4,62.65
   *
   * @return Result or InternalServerError
   */
  def DistanceList(position: String) = Action.async { implicit request =>
    scala.concurrent.Future {
      render {
        case Accepts.Json() => calcDistancesAs(position, JSON)
        case AcceptsRDFXML() => calcDistancesAs(position, RDF).withHeaders("Content-Type" -> (RDF + ContentTypeEnding))
        case AcceptsTurtle() => calcDistancesAs(position, Turtle).withHeaders("Content-Type" -> (Turtle + ContentTypeEnding))
        case AcceptsN3() => calcDistancesAs(position, N3).withHeaders("Content-Type" -> (N3 + ContentTypeEnding))
      }
    }
  }
}