package controllers

import play.api.mvc.Accepting
import play.api.mvc.Action
import play.api.mvc.Controller
import resources.ResultGenerator.getBuildingDetailsResult
import resources.ResultGenerator.getBuildingsResult

object v1 extends Controller {

  val AcceptsRDFXML = Accepting("application/rdf+xml")
  val AcceptsTurtle = Accepting("text/turtle")
  val AcceptsN3 = Accepting("text/n3")
  val AcceptsJSONLD = Accepting("application/ld+json")

  /**
   * Get all buildings as a list
   *
   * @return Result as List or InternalServerError, if an Exception was thrown
   */
  def buildings = Action { implicit request =>
    render {
      case Accepts.Json() => getBuildingsResult("json")
      case AcceptsRDFXML() => getBuildingsResult("RDF/XML")
      case AcceptsTurtle() => getBuildingsResult("Turtle")
      case AcceptsN3() => getBuildingsResult("N-Triples")
      //case AcceptsJSONLD() => getBuildingsResult("JSON-LD")  --> No Writer found Exception
    }
  }

  /**
   * Get details for a specific building
   *
   * @param key The key inside the BuildingsMap
   *
   * @return Result or NotFound
   */
  def buildingDetails(key: String) = Action { implicit request =>
    render {
      case Accepts.Json() => getBuildingDetailsResult(key, "json")
      case AcceptsRDFXML() => getBuildingDetailsResult(key, "RDF/XML")
      case AcceptsTurtle() => getBuildingDetailsResult(key, "Turtle")
      case AcceptsN3() => getBuildingDetailsResult(key, "N-Triples")
      //case AcceptsJSONLD() => getBuildingDetailsResult(key, "JSON-LD") --> No Writer found Exception
    }
  }

}