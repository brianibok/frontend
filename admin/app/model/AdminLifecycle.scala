package model

import java.util.TimeZone

import common._
import conf.Configuration
import conf.Configuration.environment
import conf.switches.Switches._
import football.feed.MatchDayRecorder
import jobs._
import play.api.inject.ApplicationLifecycle
import services.EmailService
import tools.{AssetMetricsCache, CloudWatch, LoadBalancer}

import scala.concurrent.{ExecutionContext, Future}

class AdminLifecycle(appLifecycle: ApplicationLifecycle, jobs: JobScheduler = Jobs, akkaAsync: AkkaAsync = AkkaAsync)(implicit ec: ExecutionContext) extends LifecycleComponent with Logging {

  appLifecycle.addStopHook { () => Future {
    descheduleJobs()
    CloudWatch.shutdown()
    EmailService.shutdown()
  }}

  lazy val adminPressJobStandardPushRateInMinutes: Int = Configuration.faciatool.adminPressJobStandardPushRateInMinutes
  lazy val adminPressJobHighPushRateInMinutes: Int = Configuration.faciatool.adminPressJobHighPushRateInMinutes
  lazy val adminPressJobLowPushRateInMinutes: Int = Configuration.faciatool.adminPressJobLowPushRateInMinutes
  lazy val adminRebuildIndexRateInMinutes: Int = Configuration.indexes.adminRebuildIndexRateInMinutes
  lazy val r2PagePressRateInSeconds: Int = Configuration.r2Press.pressRateInSeconds

  private def scheduleJobs(): Unit = {

    //every 0, 30 seconds past the minute
    jobs.schedule("AdminLoadJob", "0/30 * * * * ?") {
      model.abtests.AbTestJob.run()
    }

    //every 4, 19, 34, 49 minutes past the hour, on the 2nd second past the minute (e.g 13:04:02, 13:19:02)
    jobs.schedule("LoadBalancerLoadJob", "2 4/15 * * * ?") {
      LoadBalancer.refresh()
    }

    //every 2 minutes starting 5 seconds past the minute (e.g  13:02:05, 13:04:05)
    jobs.schedule("FastlyCloudwatchLoadJob", "5 0/2 * * * ?") {
      FastlyCloudwatchLoadJob.run()
    }

    jobs.scheduleEveryNSeconds("R2PagePressJob", r2PagePressRateInSeconds) {
      R2PagePressJob.run()
    }

    //every 2, 17, 32, 47 minutes past the hour, on the 12th second past the minute (e.g 13:02:12, 13:17:12)
    jobs.schedule("AnalyticsSanityCheckJob", "12 2/15 * * * ?") {
      AnalyticsSanityCheckJob.run()
    }

    jobs.scheduleEveryNMinutes("FrontPressJobHighFrequency", adminPressJobHighPushRateInMinutes) {
      if(FrontPressJobSwitch.isSwitchedOn) RefreshFrontsJob.runFrequency(HighFrequency)
      Future.successful(())
    }

    jobs.scheduleEveryNMinutes("FrontPressJobStandardFrequency", adminPressJobStandardPushRateInMinutes) {
      if(FrontPressJobSwitchStandardFrequency.isSwitchedOn) RefreshFrontsJob.runFrequency(StandardFrequency)
      Future.successful(())
    }

    jobs.scheduleEveryNMinutes("FrontPressJobLowFrequency", adminPressJobLowPushRateInMinutes) {
      if(FrontPressJobSwitch.isSwitchedOn) RefreshFrontsJob.runFrequency(LowFrequency)
      Future.successful(())
    }

    jobs.schedule("RebuildIndexJob", s"9 0/$adminRebuildIndexRateInMinutes * 1/1 * ? *") {
      RebuildIndexJob.run()
    }

    // every minute, 22 seconds past the minute (e.g 13:01:22, 13:02:22)
    jobs.schedule("MatchDayRecorderJob", "22 * * * * ?") {
      MatchDayRecorder.record()
    }

    if (environment.isProd) {
      val londonTime = TimeZone.getTimeZone("Europe/London")
      jobs.scheduleWeekdayJob("AdsStatusEmailJob", 44, 8, londonTime) {
        AdsStatusEmailJob.run()
      }
      jobs.scheduleWeekdayJob("ExpiringAdFeaturesEmailJob", 47, 8, londonTime) {
        log.info(s"Starting ExpiringAdFeaturesEmailJob")
        ExpiringAdFeaturesEmailJob.run()
      }
      jobs.scheduleWeekdayJob("ExpiringSwitchesEmailJob", 48, 8, londonTime) {
        log.info(s"Starting ExpiringSwitchesEmailJob")
        ExpiringSwitchesEmailJob.run()
      }
    }

    //every 7, 22, 37, 52 minutes past the hour, 28 seconds past the minute (e.g 13:07:28, 13:22:28)
    jobs.schedule("VideoEncodingsJob", "28 7/15 * * * ?") {
      log.info("Starting VideoEncodingsJob")
      VideoEncodingsJob.run()
    }

    jobs.scheduleEveryNMinutes("AssetMetricsCache", 60 * 6) {
      AssetMetricsCache.run()
    }

  }

  private def descheduleJobs(): Unit = {
    jobs.deschedule("AdminLoadJob")
    jobs.deschedule("LoadBalancerLoadJob")
    jobs.deschedule("FastlyCloudwatchLoadJob")
    jobs.deschedule("R2PagePressJob")
    jobs.deschedule("AnalyticsSanityCheckJob")
    jobs.deschedule("FrontPressJob")
    jobs.deschedule("RebuildIndexJob")
    jobs.deschedule("MatchDayRecorderJob")
    jobs.deschedule("SentryReportJob")
    jobs.deschedule("FrontPressJobHighFrequency")
    jobs.deschedule("FrontPressJobStandardFrequency")
    jobs.deschedule("FrontPressJobLowFrequency")
    jobs.deschedule("AdsStatusEmailJob")
    jobs.deschedule("ExpiringAdFeaturesEmailJob")
    jobs.deschedule("VideoEncodingsJob")
    jobs.deschedule("ExpiringSwitchesEmailJob")
    jobs.deschedule("AssetMetricsCache")
  }

  override def start(): Unit = {
    descheduleJobs()
    scheduleJobs()

    akkaAsync.after1s {
      RebuildIndexJob.run()
      VideoEncodingsJob.run()
      AssetMetricsCache.run()
    }
  }
}
