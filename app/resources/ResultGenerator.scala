package resources

import java.io.ByteArrayOutputStream
import java.io.StringWriter

import scala.math.BigDecimal.long2bigDecimal

import com.hp.hpl.jena.query.QueryExecutionFactory
import com.hp.hpl.jena.query.QueryFactory
import com.hp.hpl.jena.query.ResultSetFormatter
import com.hp.hpl.jena.sparql.resultset._
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

    typ match {
      case "json" =>
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
          graph.write(out, typ)
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
            out.toString()
          }
        }
      case _ => None
    }

    /* Alternate Method, Not working yet, because sub-resources are missing
    val graph = Extractor.getGraph

    val res = graph.query(new SimpleSelector(graph.getResource("http://htwk-app.imn.htwk-leipzig.de/info/building/" + key), null, null.asInstanceOf[RDFNode]))

    val out = new StringWriter()
    if (!res.isEmpty()) {
      res.write(out, Datatype)
      Option.apply(out.toString())
    } else {
      Option.apply(null)
    } */
  }

  /*
   * Query the given RDF Graph
   *
   * @param queryString query formatted as String
   *
   * @return Result String
   */
  def queryGraph(queryString: String): String = {

    val query = QueryFactory.create(queryString)
    val qe = QueryExecutionFactory.create(query, Extractor.getGraph)
    val results = qe.execSelect()
    val out = new ByteArrayOutputStream()

    RDFOutput.outputAsRDF(out, "N-Triples", results)

    qe.close()
    out.close()

    out.toString()
  }

}