package controllers

import common.ExecutionContexts
import model.{NoCache, Cached}
import model.Cached.RevalidatableResult
import play.api.libs.ws.WS
import play.api.mvc.{Action, Controller}
import staticpages.StaticPages


object SurveyPageController extends Controller with ExecutionContexts {

  import play.api.Play.current

  def renderSimpleSurveyPage() = Action { implicit request =>
      Cached(60)(RevalidatableResult.Ok(views.html.survey.simpleSurveyPage(StaticPages.simpleSurveyStaticPageForId(request.path))))
   }

  def renderFormStackSurvey(formName: String) = Action.async { implicit request =>
      WS.url(s"https://guardiannewsampampmedia.formstack.com/forms/$formName")
        .head
        .map { headResponse =>
          headResponse.status match {
            case 200 =>
              Cached(60)(RevalidatableResult.Ok(
                views.html.survey.formstackSurvey(formName, StaticPages.simpleSurveyStaticPageForId(request.path))))
            case _ =>
              NoCache(NotFound)}}}

  def thankYou() = Action { implicit request =>
    Ok(views.html.survey.thankyou(StaticPages.simpleSurveyStaticPageForId(request.path)))
  }

  def quickSurvey() = Action { implicit request =>
    Ok(views.html.survey.quickSurvey(StaticPages.simpleSurveyStaticPageForId(request.path)))
  }
}