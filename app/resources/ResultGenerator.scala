package resources

import java.io.StringWriter

import scala.math.BigDecimal.long2bigDecimal

import com.hp.hpl.jena.rdf.model.ModelFactory
import com.hp.hpl.jena.vocabulary.RDF

import play.api.libs.json.JsNumber
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.mvc.Result
import play.api.mvc.Results.InternalServerError
import play.api.mvc.Results.NotFound
import play.api.mvc.Results.Ok
import resources.Extractor.getBuildings

object ResultGenerator {

  /**
   * Get details for all buildings as a HTTP Result
   *
   * @param Datatype Content-Type of the request
   *
   * @return HTTP Result
   */
  def getBuildingsResult(typ: String): Result = {
    val buildings = getBuildings.toSeq.seq sortBy { case (x, _) => x }
    typ match {
      case "json" =>
        if (buildings.nonEmpty) {
          val list = buildings
            .map { case (key, _) => Json.arr(getBuildingDetailsAsJSON(key).get) }
            .reduce(_ ++ _)

          Ok(list).withHeaders("Cache-Control" -> "public, max-age=604800")
        } else
          InternalServerError
      case _ =>
        if (buildings.nonEmpty) {
          val list = buildings
            .map { case (key, _) => getBuildingDetailsAsLinkedData(key, typ).get }
            .reduce(_ ++ _)

          Ok(list).withHeaders("Cache-Control" -> "public, max-age=604800")
        } else
          InternalServerError
    }
  }

  /**
   * Get details for a specific building as a HTTP Result
   *
   * @param key Key of the building in Buildingsmap
   * @param Datatype Content-Type of the request
   *
   * @return HTTP Result
   */
  def getBuildingDetailsResult(key: String, typ: String): Result = {
    typ match {
      case "json" =>
        val result = getBuildingDetailsAsJSON(key)
        if (result.isDefined)
          Ok(result.get).withHeaders("Cache-Control" -> "public, max-age=604800")
        else
          NotFound
      case _ =>
        val result = getBuildingDetailsAsLinkedData(key, typ)
        if (result.isDefined)
          Ok(result.get).withHeaders("Cache-Control" -> "public, max-age=604800")
        else
          NotFound
    }
  }

  /**
   * Get details for a specific building as JSON
   *
   * @param key Key of the building in Buildingsmap
   *
   * @return Some -> a JSON Object, containing all informations about the specified building, None -> building not found or an Exception
   */
  private def getBuildingDetailsAsJSON(key: String): Option[JsObject] = {
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

  /**
   * Get details for a specific building as Linked Data
   *
   * @param key Key of the building in Buildingsmap
   * @param Datatype Type in which Apache Jena will print the output
   *
   * @return Some -> a String, containing all informations about the specified building, None -> building not found or an Exception
   */
  def getBuildingDetailsAsLinkedData(key: String, Datatype: String): Option[String] = {

    val buildings = getBuildings

    buildings.nonEmpty match {
      case true =>
        buildings.get(key).map {
          case (name, link, text, address, (long, lat), imglink, img, timestamp) => {

            val baseURI = "http://htwk-app.imn.htwk-leipzig.de/info/building"
            val model = ModelFactory.createDefaultModel()
            val out = new StringWriter()

            model.createResource(baseURI + "/" + key)
              .addProperty(RDF.`type`, Place.Place)
              .addProperty(Place.geo, model.createResource(baseURI + "/" + key + "/GeoCoordinates")
                .addProperty(RDF.`type`, GeoCoordinates.GeoCoordinates)
                .addProperty(GeoCoordinates.latitude, lat.toString())
                .addProperty(GeoCoordinates.longitude, long.toString()))
              .addProperty(Place.address, model.createResource(baseURI + "/" + key + "/PostalAddress")
                .addProperty(RDF.`type`, PostalAddress.PostalAddress)
                .addProperty(PostalAddress.addressCountry, "DE")
                .addProperty(PostalAddress.addressLocality, "Leipzig")
                .addProperty(PostalAddress.streetAddress, address))
              .addProperty(Place.photo, model.createResource(baseURI + "/" + key + "/photo")
                .addProperty(RDF.`type`, ImageObject.ImageObject)
                .addProperty(ImageObject.contentUrl, "data:image/jpg;base64," + img)
                .addProperty(ImageObject.url, imglink))
              .addProperty(Place.alternateName, key)
              .addProperty(Place.name, name)
              .addProperty(Place.description, text)

            model.write(out, Datatype)
            out.toString()
          }
        }
      case _ => None
    }
  }

}