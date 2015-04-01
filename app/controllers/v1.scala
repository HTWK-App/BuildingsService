package controllers

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

object v1 extends Controller {

  /**
   * Returns all buildings as an JSON list or an InternalServerError, if an Exception is thrown
   */
  def buildings = Cached.status(_ => "buildings", 200, 20) {
    Action {
      val buildings = getBuildings.toSeq.seq sortBy { case (x, _) => x }

      if (buildings.nonEmpty) {
        val list = buildings
          .map { case (key, _) => Json.arr(getBuildingsDetailsAsJSON(key).get) }
          .reduce(_ ++ _)

        Ok(list).withHeaders("Cache-Control" -> "public, max-age=604800")
      } else {
        //logError("No Buildings found")
        InternalServerError
      }
    }
  }

  /**
   * Returns a specific building as an JSON Object or NotFound
   */
  def buildingDetails(key: String) = Cached.status(_ => "building/" + key, 200, 20) {
    Action {
      val building = getBuildingsDetailsAsJSON(key)

      if (building.isDefined)
        Ok(building.get).withHeaders("Cache-Control" -> "public, max-age=604800")
      else
        NotFound
    }
  }

  /**
   * Returns a specific building as an Option. 
   * Some -> a JSON Object to this building. 
   * None -> building not found or Exception
   */
  private def getBuildingsDetailsAsJSON(key: String): Option[JsObject] = {
    val buildings = getBuildings

    buildings.nonEmpty match {
      case true =>
        buildings.get(key).map {
          case (name, link, text, address, (long, lat), imglink, img, timestamp) => Json.obj(
            "id" -> JsString(key), "fullName" -> JsString(name), "detailLink" -> JsString(link),
            "description" -> Json.arr(JsString(text)), "latLng" -> JsString(long.toString() + ", " + lat.toString()),
            "address" -> JsString(address), "pictureLink" -> JsString(imglink),
            "pictureData" -> JsString("data:image/jpg;base64," + img), "lastChange" -> JsNumber(timestamp))
        }
      case _ => None
    }
  }
}