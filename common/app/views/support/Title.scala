package views.support

import common.Localisation
import common.commercial.HostedPage
import model._
import play.api.mvc.RequestHeader
import play.twirl.api.Html

object Title {
  val SectionsToIgnore = Set(
    "global"
  )

  def apply(page: Page)(implicit request: RequestHeader): Html = Html{
    val title = page match {
      case pressedPage: PressedPage =>
        pressedPage.metadata.title.filter(_.nonEmpty).map(Localisation(_)).getOrElse(
          s"${Localisation(page.metadata.webTitle)}${pagination(page)}"
        )
      case c: ContentPage =>
        s"${c.metadata.webTitle}${pagination(c)}${getSectionConsideringWebtitle(c.metadata.webTitle, Option(c.item.trail.sectionName))}"
      case t: Tag     =>
        s"${Localisation(t.metadata.webTitle)}${pagination(page)}${getSectionConsideringWebtitle(t.metadata.webTitle, Option(t.properties.sectionName))}"
      case s: Section =>
        s"${Localisation(s.metadata.webTitle)}${pagination(page)}"
      case hostedPage: HostedPage =>
        s"${Localisation(hostedPage.pageTitle)}"
      case _          =>
        page.metadata.title.filter(_.nonEmpty).map(Localisation(_)).getOrElse(
          s"${Localisation(page.metadata.webTitle)}${pagination(page)}${getSectionConsideringWebtitle(page.metadata.webTitle, Option(page.metadata.sectionId))}"
        )
    }
    s"${title.trim} | The Guardian"
  }

  private def getSectionConsideringWebtitle(webTitle: String, section: Option[String]): String =
    section.filter(_.nonEmpty)
      .filterNot(_.toLowerCase == webTitle.toLowerCase)
      .filterNot(SectionsToIgnore.contains)
      .fold("") { s => s" | ${s.capitalize}"}

  private def pagination(page: Page) = page.metadata.pagination.filterNot(_.isFirstPage).map{ pagination =>
    s" | Page ${pagination.currentPage} of ${pagination.lastPage}"
  }.getOrElse("")
}
