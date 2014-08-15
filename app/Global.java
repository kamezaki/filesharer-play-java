import java.util.concurrent.TimeUnit;

import play.Application;
import play.GlobalSettings;
import play.Logger;
import play.libs.Akka;
import scala.concurrent.duration.Duration;
import biz.info_cloud.filesharer.LocalConfig;
import biz.info_cloud.filesharer.service.FileStoreService;

public class Global extends GlobalSettings {

  @Override
  public void onStart(Application application) {
    super.onStart(application);
    Logger.info("start application");
    String folder = LocalConfig.getStorePath();
    int interval = LocalConfig.getScheduleFrequencyInHours();
    
    FileStoreService service = new FileStoreService(folder);
    Akka.system().scheduler().schedule(
        Duration.create(0, TimeUnit.MILLISECONDS),  // Initial delay 0 milliseconds.
        Duration.create(interval, TimeUnit.HOURS),  // Frequency
        () -> service.deleteFiles(),
        Akka.system().dispatcher());
  }

  @Override
  public void onStop(Application application) {
    Logger.info("shutdown application");
    super.onStop(application);
  }
  
}
