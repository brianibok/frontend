package conf

import contentapi.SectionsLookUp
import play.api.mvc.{AnyContent, Action}
import scala.concurrent.Future

object HealthCheck extends AllGoodCachedHealthCheck(
  9002,
  "/books",
  "/books/harrypotter",
  "/travel/gallery/2012/nov/20/st-petersburg-pushkin-museum",
  "/travel/gallery/2012/nov/20/st-petersburg-pushkin-museum?index=2",
  "/world/video/2012/nov/20/australian-fake-bomber-sentenced-sydney-teenager-video"
) {
  override def healthCheck(): Action[AnyContent] = Action.async { request =>
    if (!SectionsLookUp.isLoaded()) {
      Future.successful(InternalServerError("Sections have not loaded from Content API"))
    } else {
      super.healthCheck()(request)
    }
  }
}
