import play.Application;
import play.GlobalSettings;
import play.Logger;

public class Global extends GlobalSettings {

  @Override
  public void onStart(Application application) {
    super.onStart(application);
    Logger.info("start application");
  }

  @Override
  public void onStop(Application application) {
    Logger.info("shutdown application");
    super.onStop(application);
  }
  
}
