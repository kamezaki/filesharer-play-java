import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import models.SecurityRole;

import com.feth.play.module.pa.PlayAuthenticate;
import com.feth.play.module.pa.PlayAuthenticate.Resolver;
import com.feth.play.module.pa.exceptions.AccessDeniedException;
import com.feth.play.module.pa.exceptions.AuthException;

import controllers.UserRole;
import controllers.routes;
import play.Application;
import play.GlobalSettings;
import play.Logger;
import play.libs.Akka;
import play.libs.F;
import play.libs.F.Promise;
import play.mvc.Action;
import play.mvc.Call;
import play.mvc.Http.Context;
import play.mvc.Http.Request;
import play.mvc.Http.RequestHeader;
import play.mvc.Result;
import play.mvc.Results;
import scala.concurrent.duration.Duration;
import biz.info_cloud.filesharer.LocalConfig;
import biz.info_cloud.filesharer.service.FileStoreService;
import views.html.error.notfound;

public class Global extends GlobalSettings {
  public static final String X_FORWARDED_PROTO = "X-Forwarded-Proto";

  @Override
  public void onStart(final Application application) {
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
        return routes.Application.login();
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
        return routes.Account.askLink();
      }

      @Override
      public Call askMerge() {
        return routes.Account.askMerge();
      }

      @Override
      public Call onException(final AuthException e) {
        Logger.error("auth failed", e);
        if (e instanceof AccessDeniedException) {
          return routes.Application.oAuthDenied(((AccessDeniedException) e).getProviderKey());
        }
        return super.onException(e);
      }
    });
    
    initilizeData();
  }

  @Override
  public void onStop(final Application application) {
    Logger.info("shutdown application");
    super.onStop(application);
  }

  @Override
  public Action<?> onRequest(final Request request, final Method actionMethod) {
    boolean forceHttps = LocalConfig.getForceHttps();
    if (forceHttps) {
      String schema = request.getHeader(X_FORWARDED_PROTO);
      if (schema != null && schema.equalsIgnoreCase("http")) {
        return new Action.Simple() {
          @Override
          public Promise<Result> call(final Context ctx) throws Throwable {
            String httpsUrl = String.format("https://%s%s", request.host(), request.path());
            return F.Promise.pure(redirect(httpsUrl));
          }
          
        };
      }
    }
    return super.onRequest(request, actionMethod);
  }
  
  private void initilizeData() {
    List<String> roleList = Arrays.asList(UserRole.USER);
    roleList.stream()
            .filter(roleName -> SecurityRole.findByRoleName(roleName) == null)
            .forEach(roleName -> {
              final SecurityRole role = new SecurityRole();
              role.roleName = roleName;
              role.save();
            });
  }

  @Override
  public Promise<Result> onHandlerNotFound(final RequestHeader request) {
    Logger.debug(String.format("not found on %s", request.path()));
    return Promise.promise(() -> {
      return Results.notFound(notfound.render(request));
    });
  }
}
