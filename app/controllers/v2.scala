package controllers

import scala.math.BigDecimal.double2bigDecimal
import scala.math.BigDecimal.long2bigDecimal

import Extractor.getBuildings
import play.api.Play.current
import play.api.cache.Cached
import play.api.libs.json.JsNumber
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.mvc.Action
import play.api.mvc.Controller

object v2 extends Controller {

  def buildings = Cached.status(_ => "buildings", 200, 20) {
    Action {
      val buildings = getBuildings.toSeq.seq sortBy { case (x, _) => x }

      if (buildings.nonEmpty) {
        val list = buildings
          .map {
            case (key, _) => Json.arr(getBuildingsDetailsAsJSON(key).get)
          }
          .reduce(_ ++ _)
        Ok(list)
      } else {
        //logError("No Buildings found")
        InternalServerError
      }
    }
  }

  def buildingDetails(key: String) = Cached.status(_ => "building/" + key, 200, 20) {
    Action {
      val building = getBuildingsDetailsAsJSON(key)

      if (building.isDefined)
        Ok(building.get)
      else
        NotFound
    }
  }

  private def getBuildingsDetailsAsJSON(key: String): Option[JsObject] = {
    val buildings = getBuildings

    buildings.nonEmpty match {
      case true =>
        buildings.get(key).map {
          case (name, link, text, address, (long, lat), imglink, img, timestamp) => Json.obj(
            "id" -> JsString(key), "name" -> JsString(name), "description" -> JsString(text),
            "position" -> Json.obj(
              "longitude" -> JsNumber(long), "latitude" -> JsNumber(lat), "address" -> JsString(address)),
            "picture" -> Json.obj(
              "pictureLink" -> JsString(imglink), "format" -> JsString("data:image/jpg;base64,"), "data" -> JsString(img)),
            "detailLink" -> JsString(link), "lastChange" -> JsNumber(timestamp))
        }
      case _ => None
    }
  }
}