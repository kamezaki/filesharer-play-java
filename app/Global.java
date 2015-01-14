import java.util.concurrent.TimeUnit;

import com.feth.play.module.pa.PlayAuthenticate;
import com.feth.play.module.pa.PlayAuthenticate.Resolver;
import com.feth.play.module.pa.exceptions.AccessDeniedException;
import com.feth.play.module.pa.exceptions.AuthException;

import controllers.routes;
import play.Application;
import play.GlobalSettings;
import play.Logger;
import play.libs.Akka;
import play.mvc.Call;
import scala.concurrent.duration.Duration;
import biz.info_cloud.filesharer.LocalConfig;
import biz.info_cloud.filesharer.service.FileStoreService;

public class Global extends GlobalSettings {

  @Override
  public void onStart(Application application) {
    super.onStart(application);
    Logger.info("start application");
    int interval = LocalConfig.getScheduleFrequencyInHours();
    
    FileStoreService service = new FileStoreService();
    Akka.system().scheduler().schedule(
        Duration.create(0, TimeUnit.MILLISECONDS),  // Initial delay 0 milliseconds.
        Duration.create(interval, TimeUnit.HOURS),  // Frequency
        () -> service.deleteFiles(),
        Akka.system().dispatcher());
    
    PlayAuthenticate.setResolver(new Resolver() {
      @Override
      public Call login() {
        return routes.Application.index();
      }

      @Override
      public Call afterAuth() {
        // The user will be redirected to this page after authentication
        // if no original URL was saved
        // TODO your page here
        return routes.Application.index();
      }

      @Override
      public Call afterLogout() {
        return routes.Application.index();
      }


      @Override
      public Call auth(final String provider) {
        // You can provide your own authentication implementation,
        // however the default should be sufficient for most cases
        return com.feth.play.module.pa.controllers.routes.Authenticate.authenticate(provider);
      }

      @Override
      public Call askLink() {
        // We don't support moderated account linking in this application.
        return null;
      }

      @Override
      public Call askMerge() {
        // We don't support moderated account merging in this application.
        return null;
      }

      @Override
      public Call onException(AuthException e) {
        Logger.error("auth failed", e);
        if (e instanceof AccessDeniedException) {
          return routes.Application.oAuthDenied(((AccessDeniedException) e).getProviderKey());
        }
        return super.onException(e);
      }
    });
  }

  @Override
  public void onStop(Application application) {
    Logger.info("shutdown application");
    super.onStop(application);
  }
  
}
