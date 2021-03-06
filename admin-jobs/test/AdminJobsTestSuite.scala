import controllers.BreakingNews.{BreakingNewsUpdaterTest, BreakingNewsApiTest}
import controllers.HealthCheck
import org.scalatest.Suites
import test.SingleServerSuite

class AdminJobsTestSuite extends Suites (
  new BreakingNewsApiTest,
  new BreakingNewsUpdaterTest,
  new controllers.NewsAlertControllerTest
)
with SingleServerSuite {
  override lazy val port: Int = HealthCheck.testPort
}
