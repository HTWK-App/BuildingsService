import controllers.Extractor.cacheIt
import play.api.Application
import play.api.GlobalSettings
import play.api.Logger
import play.api.mvc.Handler
import play.api.mvc.RequestHeader
import play.api.mvc.WithFilters
import play.filters.gzip.GzipFilter

object Global extends WithFilters(new GzipFilter()) with GlobalSettings {

  override def onStart(app: Application) {
    Logger.info("Application has started")
    cacheIt
  }

  override def onStop(app: Application) {
    Logger.info("Application shutdown...")
  }

  override def onRouteRequest(request: RequestHeader): Option[Handler] = {
    //logAccess(request.path)
    super.onRouteRequest(request)
  }
}