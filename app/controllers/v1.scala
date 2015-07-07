package controllers

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Accepting
import play.api.mvc.Action
import play.api.mvc.Controller
import resources.ResultGenerator.getBuildingDetailsResult
import resources.ResultGenerator.getBuildingsResult

object v1 extends Controller {

  val AcceptsRDFXML = Accepting("application/rdf+xml")
  val AcceptsTurtle = Accepting("text/turtle")
  val AcceptsN3 = Accepting("text/n3")

  /**
   * Get all buildings as a list
   *
   * @return Result as List or InternalServerError, if an Exception was thrown
   */
  def buildings = Action.async { implicit request =>
    scala.concurrent.Future {
      render {
        case Accepts.Json() => getBuildingsResult("json")
        case AcceptsRDFXML() => getBuildingsResult("RDF/XML").withHeaders("Content-Type" -> "application/rdf+xml; charset=utf-8")
        case AcceptsTurtle() => getBuildingsResult("Turtle").withHeaders("Content-Type" -> "text/turtle; charset=utf-8")
        case AcceptsN3() => getBuildingsResult("N-Triples").withHeaders("Content-Type" -> "text/n3; charset=utf-8")
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
        case Accepts.Json() => getBuildingDetailsResult(key, "json")
        case AcceptsRDFXML() => getBuildingDetailsResult(key, "RDF/XML").withHeaders("Content-Type" -> "application/rdf+xml; charset=utf-8")
        case AcceptsTurtle() => getBuildingDetailsResult(key, "Turtle").withHeaders("Content-Type" -> "text/turtle; charset=utf-8")
        case AcceptsN3() => getBuildingDetailsResult(key, "N-Triples").withHeaders("Content-Type" -> "text/n3; charset=utf-8")
      }
    }
  }
}