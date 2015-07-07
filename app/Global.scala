import akka.actor.ActorSelection._
import akka.actor.PoisonPill
import play.api.Application
import play.api.GlobalSettings
import play.api.Logger
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc.WithFilters
import play.filters.gzip.GzipFilter
import play.libs.Akka
import resources.Extractor.cacheIt

object Global extends WithFilters(new GzipFilter() /*, new CorsFilter()*/ ) with GlobalSettings {

  override def onStart(app: Application) {
    Logger.info("Application has started")
    cacheIt
  }

  override def onStop(app: Application) {
    Logger.info("Application shutdown...")
    Akka.system.actorSelection("*").tell(PoisonPill, null)
  }
}