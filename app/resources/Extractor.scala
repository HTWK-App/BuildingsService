package resources

import java.text.SimpleDateFormat
import java.util.Date

import scala.collection.parallel.immutable.ParMap
import scala.collection.parallel.immutable.ParSeq
import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration

import org.apache.commons.codec.binary.Base64
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements

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

  /** Returns a map, containing all known buildings */
  def getBuildings = {
    val b = buildingsMap
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

        val doc = Jsoup.connect(link).get()
        val heading = doc.select("#content h1").first().text
        val key = keyCornerCase(heading)
        val content = doc.select("#content div.csc-textpic-text p")
        val text = textCornerCase(key, content)
        val adress = adressCornerCase(key, content)
        val pos = extractPosition(doc)
        val (imgLink, img) = imageCornerCase(key, doc)
        val timestamp = extractTimestamp(doc).getTime

        (key, (heading, link, text, adress, pos, imgLink, img, timestamp))
      }.toMap

      buildingsMap = extractedBuildings
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
      case "MenAca" => extractText(5)
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

    key match {
      case "FZC" => extractAdress(2, 1, ", ")
      case "HB" => extractAdress(2, 1)
      case "M" => ""
      case "MenAca" => extractAdress(4, 3)
      case _ => content.last().text()
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
      case _ => "http://www.htwk-leipzig.de/" + (doc select ("#content img") attr ("src"))
    }

    val img = imgLink match {
      case "" => ""
      case _ => Base64 encodeBase64String (Jsoup connect (imgLink) ignoreContentType (true) execute () bodyAsBytes ())
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
  private def extractTimestamp(doc: Document): Date = {
    val extime = doc.select("#content .last_update").first().text()
      .split("E-Mail").head
      .split("am").last
      .replace(" ", "")
    new SimpleDateFormat("dd.MM.yyy").parse(extime)
  }

  /**
   * Extracts the Geo position from the provided page
   *
   * @param doc Full HTML DOM
   *
   * @return 2_Tuple[Latitude, Longitude]
   */
  private def extractPosition(doc: Document): (Float, Float) = {
    val posLine = doc.toString().lines filter { line =>
      line contains ("var latlng = new google.maps.LatLng(")
    }
    val pos = posLine.hasNext match {
      case true => posLine.next().split('(').last.split(')').head.split(", ")
      case _ => Array("")
    }
    (pos.head.toFloat, pos.last.toFloat)
  }
}