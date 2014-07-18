import java.util.concurrent.TimeUnit;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import biz.info_cloud.filesharer.service.FileStoreService;
import play.Application;
import play.GlobalSettings;
import play.Logger;
import play.libs.Akka;
import scala.concurrent.duration.Duration;

public class Global extends GlobalSettings {
  private static Config config = ConfigFactory.load();

  @Override
  public void onStart(Application application) {
    super.onStart(application);
    Logger.info("start application");
    String folder = config.getString(controllers.Application.ConfigStorePath);
    FileStoreService service = new FileStoreService(folder);
    Akka.system().scheduler().schedule(
        Duration.create(0, TimeUnit.MILLISECONDS),  // Initial delay 0 milliseconds.
        Duration.create(7, TimeUnit.HOURS),         // Frequency 7 hours
        () -> service.deleteFiles(),
        Akka.system().dispatcher());
  }

  @Override
  public void onStop(Application application) {
    Logger.info("shutdown application");
    super.onStop(application);
  }
  
}
