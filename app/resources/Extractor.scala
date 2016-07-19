package resources

import java.text.SimpleDateFormat
import java.util.Date

import scala.collection.parallel.immutable.ParMap
import scala.collection.parallel.immutable.ParSeq
import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration
import scala.util.matching.Regex

import org.apache.commons.codec.binary.Base64
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements

import com.hp.hpl.jena.rdf.model.Model
import com.hp.hpl.jena.rdf.model.ModelFactory
import com.hp.hpl.jena.vocabulary.RDF

import play.api.Logger
import play.api.Play.current
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
 * Singleton Object that holds the data about all buildings
 *
 * @TODO Find a safe way to shutdown the actor-system. Currently it ends up with an exception.
 */
object Extractor {

  /**
   * key -> name, link, text, positon(longitude, latitude), address, imageLink, image, timestamp
   *
   * @FIXME Unsafe concurrent acess to buildingsmap through actors and this object
   */
  private var buildingsMap = ParMap.empty[String, (String, String, String, String, (Float, Float), String, String, Long)]
  private var RDFGraph = ModelFactory.createDefaultModel()

  /** Returns a map, containing all known buildings */
  def getBuildings: ParMap[String,(String, String, String, String, (Float, Float), String, String, Long)] = {
    val b = buildingsMap
    b
  }

  /** Returns a Jena RDF Model, containing all known buildings */
  def getGraph: Model = {
    val b = RDFGraph
    b
  }

  /**
   * Worker function, to renew cached buildings
   *
   * @return It was forced to return something --> useless data
   */
  def cacheIt: FiniteDuration = {
    Logger.info("Started cache renew")

    val time = try {
      val buildings = extractBuildingLinks
      val extractedBuildings = buildings.map { link =>

        val doc = Jsoup.connect(link).timeout(10*1000).get()
        val heading = doc.select("#content h1").first().text
        val key = keyCornerCase(heading)
        val content = doc.select("#content div.csc-textpic-text p")
        val text = textCornerCase(key, content)
        val adress = adressCornerCase(key, content)
        val pos = extractPosition(doc, key)
        val (imgLink, img) = imageCornerCase(key, doc)
        val timestamp = extractTimestamp(key, doc).getTime

        (key, (heading, link, text, adress, pos, imgLink, img, timestamp))
      }.toMap

      buildingsMap = extractedBuildings.par
      RDFGraph = renewGraph
      Logger.info("Cache renewed")
      24.hour
    } catch {
      case t: Throwable =>
        //logError("Failed to renew cached data")
        Logger.error("Failed to renew cached data", t)
        5.minute
    }
    Akka.system.scheduler.scheduleOnce(time) { cacheIt }
    time
  }

  /**
   * Returns a sequence containing all known links to building detail Webpages
   */
  private def extractBuildingLinks: ParSeq[String] = {
    val doc = Jsoup
      .connect("http://www.htwk-leipzig.de/de/hochschule/ueber-die-htwk-leipzig/gebaeudeuebersicht/")
      .get()
      .select("a[title=Gebäudeübersicht]").parents().first().select("ul li a")

    (1 to doc.size() - 1).par map { id =>
      "http://www.htwk-leipzig.de/" + doc.get(id).attr("href")
    }
  }

  //Corner Cases *********************************************************
  /** Corner cases for broken keys of the resulting map */
  private def keyCornerCase(heading: String): String = {

    val key = heading.split('(').last.split(')').head
    key match {
      case "Mensa Academica" => "Mensa"
      case "SH früher HS" => "SH"
      case "Forschungszentrum Life Science & Engineering" => "FZE"
      case _ => key
    }
  }

  /**
   * Corner cases for different text formatting inside the HTML Document
   *
   * @param key Key inside the resulting Map
   * @param content preextracted HTML DOM
   *
   * @return Description of the building
   */
  private def textCornerCase(key: String, content: Elements): String = {

    def extractText(end: Integer): String = {
      (0 to content.size - end)
        .map(x => content.get(x).text)
        .reduce((a, b) => a + b)
    }

    key match {
      case "FZC" => extractText(3)
      case "FZE" => extractText(6)
      case "HB" => extractText(4)
      case "Mensa" => extractText(5)
      case _ => extractText(2)
    }
  }

  /**
   * Corner cases for different adress formatting inside the HTML Document
   *
   * @param key Key inside the resulting Map
   * @param content preextracted HTML DOM
   *
   * @return Adress of the building
   */
  private def adressCornerCase(key: String, content: Elements): String = {

    def extractAdress(start: Integer, end: Integer, add: String = ""): String = {
      (content.size - start to content.size - end)
        .map(x => content.get(x).text)
        .reduce((a, b) => a + add + b)
    }

    val address = key match {
      case "FZC" => extractAdress(2, 1, ", ")
      case "HB" => extractAdress(2, 1)
      case "M" => ""
      case "Mensa" => extractAdress(4, 3)
      case "MN" => "Koburger Straße 62, 04416 Markkleeberg"
      case _ => content.last().text()
    }
    val postal = new Regex("\\d{5}") findFirstIn address
    postal match {
      case Some(p) if key == "MZ" => address.splitAt(address.indexOf(p)-1) match{case (a,b) => a.dropRight(1) + "," + b}
      case Some(p) => address.splitAt(address.indexOf(p)-1) match{case (a,b) => a.replace(",", "") + "," + b}
      case None => address
    }
  }

  /**
   * Corner cases to rebuild broken image links
   *
   * @param key Key inside the resulting Map
   * @param doc Full HTML DOM
   *
   * @return 2_Tuple[Image-Link, Base64 encoded Image]
   */
  private def imageCornerCase(key: String, doc: Document): (String, String) = {

    val imgLink = key match {
      case "FZC" => ""
      case "N" => ""
      case "Sportplatz" => ""
      case _ => "http://www.htwk-leipzig.de/" + (doc select ("#content img") attr ("src"))
    }

    val img = imgLink match {
      case "" => ""
      case _ => "data:image/jpg;base64," + Base64.encodeBase64String(Jsoup.connect(imgLink).ignoreContentType(true).execute().bodyAsBytes())
    }
    (imgLink, img)
  }

  /**
   * Extracts the Timestamp from the provided page
   *
   * @param doc Full HTML DOM
   *
   * @return Extracted Timestamp as Date Object
   *
   */
  private def extractTimestamp(key: String, doc: Document): Date = {
    try {
      val extime = doc.select("#content .tx-tslastupdate-pi1").text().split(":").last.replace(" ", "")
      new SimpleDateFormat("dd.MM.yyy").parse(extime)
    } catch {
      case e: Exception => new SimpleDateFormat("dd.MM.yyy").parse("01.01.2016")
    }
  }

  /**
   * Extracts the Geo position from the provided page
   *
   * @param doc Full HTML DOM
   *
   * @return 2_Tuple[Latitude, Longitude]
   */
  private def extractPosition(doc: Document, key: String): (Float, Float) = {
    val posLine = doc.toString().lines filter { line =>
      line contains ("var latlng = new google.maps.LatLng(")
    }
    val pos = posLine.hasNext match {
      case true => posLine.next().split('(').last.split(')').head.split(", ")
      case _ => Array("")
    }
    key match {
      case "MN" => (51.2824333.toFloat, 12.360206.toFloat)
      case _ => (pos.head.toFloat, pos.last.toFloat)
    }
  }

  def renewGraph: Model = {

    val buildings = getBuildings.seq
    val model = ModelFactory.createDefaultModel()
    val baseURI = "http://htwk-app.imn.htwk-leipzig.de/info/building"
    model.setNsPrefixes((new schema()).Prefix)

    buildings.foreach {
      case (key, (name, link, text, address, (long, lat), imglink, img, timestamp)) => {

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
            .addProperty(ImageObject.contentUrl, img)
            .addProperty(ImageObject.url, imglink))
          .addProperty(Place.alternateName, key)
          .addProperty(Place.name, model.createLiteral(name, "de"))
          .addProperty(Place.description, model.createLiteral(text, "de"))
      }
    }

    model
  }
}
