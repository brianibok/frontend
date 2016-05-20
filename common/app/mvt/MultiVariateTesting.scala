package mvt

import MultiVariateTesting._
import common.InternationalEditionVariant
import conf.switches.{SwitchGroup, Switch}
import org.joda.time.LocalDate
import play.api.mvc.RequestHeader
import views.support.CamelCase
import conf.switches.Switches.ServerSideTests

// To add a test, do the following:
// 1. Create an object that extends TestDefinition
// 2. Add the object to ActiveTests.tests
//
// object ExampleTest extends TestDefinition(...)
//
// object ActiveTests extends Tests {
//    val tests = List(ExampleTest)
// }

object ABOpenGraphOverlay extends TestDefinition(
  variants = Nil,
  name = "ab-open-graph-overlay",
  description = "Add a branded overlay to images cached by the Facebook crawler",
  sellByDate = new LocalDate(2016, 6, 29)
) {
  override def isParticipating(implicit request: RequestHeader): Boolean = {
    request.queryString.get("page").exists(_.contains("facebookOverlayVariant")) && switch.isSwitchedOn && ServerSideTests.isSwitchedOn
  }
}


object ABHeadlinesTestVariant extends TestDefinition(
  Nil,
  "headlines-ab-variant",
  "To test how much of a difference changing a headline makes (variant group)",
  new LocalDate(2016, 6, 10)
  ) {
  override def isParticipating(implicit request: RequestHeader): Boolean = {
    request.headers.get("X-GU-hlt").contains("hlt-V") && switch.isSwitchedOn && ServerSideTests.isSwitchedOn
    }
}

object ABNewHeaderVariant extends TestDefinition(
  variants = Nil,
  name = "ab-new-header-variant",
  description = "Feature switch (0% test) for the new header",
  sellByDate = new LocalDate(2016, 6, 14)
) {
  override def isParticipating(implicit request: RequestHeader): Boolean = {
    request.headers.get("X-GU-ab-new-header").contains("variant") && switch.isSwitchedOn && ServerSideTests.isSwitchedOn
  }
}

object ABHeadlinesTestControl extends TestDefinition(
  Nil,
  "headlines-ab-control",
  "To test how much of a difference changing a headline makes (control group)",
  new LocalDate(2016, 6, 10)
  ) {
  override def isParticipating(implicit request: RequestHeader): Boolean = {
      request.headers.get("X-GU-hlt").contains("hlt-C") && switch.isSwitchedOn && ServerSideTests.isSwitchedOn
    }
}

object ActiveTests extends Tests {
  val tests: Seq[TestDefinition] = List(
    ABOpenGraphOverlay,
    ABNewHeaderVariant,
    ABHeadlinesTestControl,
    ABHeadlinesTestVariant
  )

  def getJavascriptConfig(implicit request: RequestHeader): String = {

    val headlineTests = List(ABHeadlinesTestControl, ABHeadlinesTestVariant).filter(_.isParticipating)
                          .map{ test => s""""${CamelCase.fromHyphenated(test.name)}" : ${test.switch.isSwitchedOn}"""}
    val newHeaderTests = List(ABNewHeaderVariant).filter(_.isParticipating)
                          .map{ test => s""""${CamelCase.fromHyphenated(test.name)}" : ${test.switch.isSwitchedOn}"""}
    val internationalEditionTests = List(InternationalEditionVariant(request)
                                      .map{ international => s""""internationalEditionVariant" : "$international" """}).flatten

    val activeTest = List(ActiveTests.getParticipatingTest(request)
                        .map{ test => s""""${CamelCase.fromHyphenated(test.name)}" : ${test.switch.isSwitchedOn}"""}).flatten

    val configEntries = activeTest ++ internationalEditionTests ++ headlineTests ++ newHeaderTests

    configEntries.mkString(",")
  }
}

case class TestDefinition (
  variants: Seq[Variant],
  name: String,
  description: String,
  sellByDate: LocalDate
) {
  val switch: Switch = Switch(
    SwitchGroup.ServerSideABTests,
    name,
    description,
    conf.switches.Off,
    sellByDate,
    exposeClientSide = true
  )

  def isParticipating(implicit request: RequestHeader): Boolean = {
    ActiveTests.getParticipatingTest(request).contains(this)
  }
}

trait Tests {

  protected def tests: Seq[TestDefinition]

  def getParticipatingTest(request: RequestHeader): Option[TestDefinition] = {
    getVariant(request).flatMap { variant =>
      tests.find { test =>
        test.variants.contains(variant) &&
        test.switch.isSwitchedOn &&
        ServerSideTests.isSwitchedOn
      }
    }
  }

  def isParticipatingInATest(request: RequestHeader): Boolean = getParticipatingTest(request).isDefined
}

object MultiVariateTesting {

  sealed case class Variant(name: String)

  private[mvt] val allVariants = List[Variant]()

  def getVariant(request: RequestHeader): Option[Variant] = {
    val cdnVariant: Option[String] = request.headers.get("X-GU-mvt-variant")

    cdnVariant.flatMap( variantName => {
      allVariants.find(_.name == variantName)
    })
  }
}
