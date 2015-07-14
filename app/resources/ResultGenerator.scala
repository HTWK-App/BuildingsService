package resources

import java.io.StringWriter

import scala.collection.Seq
import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.math.BigDecimal.int2bigDecimal
import scala.math.BigDecimal.long2bigDecimal

import com.hp.hpl.jena.rdf.model.ModelFactory
import com.hp.hpl.jena.vocabulary.RDF

import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.JsNumber
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.libs.ws.WS
import play.api.libs.ws.WSRequestHolder
import play.api.mvc.Result
import play.api.mvc.Results.InternalServerError
import play.api.mvc.Results.NotFound
import play.api.mvc.Results.Ok
import resources.Extractor.getBuildings

object ResultGenerator {

  /**
   * Mapping HTTP contentTypes to Apache Jena types
   *
   * @param ContentType HTTP contentType
   *
   * @return Jena type
   */
  def ContentTypeMapperForJena(ContentType: String): String = ContentType match {
    case controllers.v1.JSON => ""
    case controllers.v1.RDF => "RDF/XML"
    case controllers.v1.Turtle => "Turtle"
    case controllers.v1.N3 => "N-Triples"
  }

  /**
   * Get details for all buildings as a HTTP Result
   *
   * @param Datatype Content-Type of the request
   *
   * @return HTTP Result
   */
  def getBuildingsResult(typ: String): Result = {

    typ match {
      case controllers.v1.JSON =>
        val buildings = getBuildings.toSeq.seq sortBy { case (x, _) => x }

        if (buildings.nonEmpty) {
          val list = buildings
            .map { case (key, _) => Json.arr(getBuildingDetailsAsJSON(key).get) }
            .reduce(_ ++ _)

          Ok(list).withHeaders("Cache-Control" -> "public, max-age=604800")
        } else
          InternalServerError
      case _ =>
        val graph = Extractor.getGraph

        if (!graph.isEmpty()) {
          val out = new StringWriter()
          graph.write(out, ContentTypeMapperForJena(typ))
          out.close()
          Ok(out.toString()).withHeaders("Cache-Control" -> "public, max-age=604800")
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
      case controllers.v1.JSON =>
        val result = getBuildingDetailsAsJSON(key)
        if (result.isDefined)
          Ok(result.get).withHeaders("Cache-Control" -> "public, max-age=604800")
        else
          NotFound
      case _ =>
        val result = getBuildingDetailsAsLinkedData(key, ContentTypeMapperForJena(typ))
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

    getBuildings.get(key) match {
      case Some((name, link, text, address, (long, lat), imglink, img, timestamp)) => Some(Json.obj(
        "id" -> JsString(key), "fullName" -> JsString(name), "detailLink" -> JsString(link),
        "description" -> Json.arr(JsString(text)), "latLng" -> JsString(long.toString() + ", " + lat.toString()),
        "address" -> JsString(address), "pictureLink" -> JsString(imglink),
        "pictureData" -> JsString("data:image/jpg;base64," + img), "lastChange" -> JsNumber(timestamp)))
      case None => None
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

    getBuildings.get(key) match {
      case Some((name, link, text, address, (long, lat), imglink, img, timestamp)) => {

        val baseURI = "http://htwk-app.imn.htwk-leipzig.de/info/building"
        val model = ModelFactory.createDefaultModel()
        val out = new StringWriter()
        model.setNsPrefixes((new schema()).Prefix)

        model.createResource(baseURI + "/" + key)
          .addProperty(RDF.`type`, Place.Place)
          .addProperty(Place.geo, model.createResource(baseURI + "/" + key + "/GeoCoordinates")
            .addProperty(RDF.`type`, GeoCoordinates.GeoCoordinates)
            .addProperty(GeoCoordinates.latitude, model.createTypedLiteral((lat.floatValue().asInstanceOf[java.lang.Float])))
            .addProperty(GeoCoordinates.longitude, model.createTypedLiteral((long.floatValue().asInstanceOf[java.lang.Float]))))
          .addProperty(Place.address, model.createResource(baseURI + "/" + key + "/PostalAddress")
            .addProperty(RDF.`type`, PostalAddress.PostalAddress)
            .addProperty(PostalAddress.addressCountry, "DE")
            .addProperty(PostalAddress.addressLocality, model.createLiteral("Leipzig", "de"))
            .addProperty(PostalAddress.streetAddress, model.createLiteral(address, "de")))
          .addProperty(Place.photo, model.createResource(baseURI + "/" + key + "/photo")
            .addProperty(RDF.`type`, ImageObject.ImageObject)
            .addProperty(ImageObject.contentUrl, "data:image/jpg;base64," + img)
            .addProperty(ImageObject.url, imglink))
          .addProperty(Place.alternateName, key)
          .addProperty(Place.name, model.createLiteral(name, "de"))
          .addProperty(Place.description, model.createLiteral(text, "de"))

        model.write(out, Datatype)
        out.close()
        Some(out.toString())
      }
      case _ => None
    }
  }
  
  /**
   * Get all building distances as the preferred ContentType
   *
   * @param position 
   * @param ContentType HTTP content-type
   *
   * @return A Result, containing all informations about distances and buildings
   */
  def calcDistancesAs(position: String, ContentType: String): Result = {

    def getDestinationsAndNames: (String, Seq[(String, String)]) = {

      val buildings: Seq[(String, String, Float, Float)] = getBuildings.toSeq
        .map { case (id, (name, _, _, _, (lat, long), _, _, _)) => (id, name, lat, long) }.seq
      val destinations: String = buildings.map {
        case (_, _, lat, long) => lat.toString() + "," + long.toString() + "|"
      }.reduce((left, right) => left + right).init

      (destinations, buildings.map { case (key, name, _, _) => (key, name) })
    }

    def uniteDistancesAndNames(distances: Seq[Int], buildings: Seq[(String, String)]): Seq[(String, String, Int)] = distances match {
      case x :: xs => return uniteDistancesAndNames(xs, buildings.tail) :+ (buildings.head._1, buildings.head._2, x)
      case nil => return Seq()
    }

    val destinationsAndNames: (String, Seq[(String, String)]) = getDestinationsAndNames
    val GoogleDistanceService: WSRequestHolder = WS
      .url("https://maps.googleapis.com/maps/api/distancematrix/json")
      .withQueryString("origins" -> position, "destinations" -> destinationsAndNames._1, "mode" -> "walking")
      .withRequestTimeout(3000).withFollowRedirects(true)

    val futureResult = GoogleDistanceService.get().map { response =>
      if (response.status == 200) {
        val distances = response.json.\\("distance").map(x => x.\("value").toString().toInt)
        if (distances.length == destinationsAndNames._2.length) {
          Some(uniteDistancesAndNames(distances, destinationsAndNames._2).sortBy(_._3))
        } else
          None
      } else
        None
    }

    ContentType match {
      case controllers.v1.JSON => getDistancesAsJson(futureResult)
      case _ => InternalServerError
    }
  }

  /**
   * Get all building distances as a JSON Result
   *
   * @param futureResult A mapped result of the GoogleDistanceService 
   *
   * @return A JSON Result, containing all informations about distances and buildings or InternalServerError
   */
  def getDistancesAsJson(futureResult: Future[Option[Seq[(String, String, Int)]]]): Result = {
    try {
      Await.result(futureResult, 10.second) match {
        case Some(namesAndDistances) => {
          val ResultAsJson = namesAndDistances.map {
            case (key, name, distance) =>
              Json.arr(Json.obj("id" -> JsString(key), "fullName" -> JsString(name), "distance" -> JsNumber(distance)))
          }.reduce((a, b) => a ++ b)

          Ok(ResultAsJson)
        }
        case None => InternalServerError
      }
    } catch {
      case t: Throwable => InternalServerError
    }
  }
}
