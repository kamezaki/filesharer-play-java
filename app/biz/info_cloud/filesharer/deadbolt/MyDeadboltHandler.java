package biz.info_cloud.filesharer.deadbolt;

import models.User;

import com.feth.play.module.pa.PlayAuthenticate;
import com.feth.play.module.pa.user.AuthUserIdentity;

import controllers.MessageKey;
import play.libs.F;
import play.libs.F.Promise;
import play.mvc.Http.Context;
import play.mvc.Result;
import be.objectify.deadbolt.core.models.Subject;
import be.objectify.deadbolt.java.AbstractDeadboltHandler;
import be.objectify.deadbolt.java.DynamicResourceHandler;

public class MyDeadboltHandler extends AbstractDeadboltHandler {

  @Override
  public Promise<Result> beforeAuthCheck(final Context context) {
    if (PlayAuthenticate.isLoggedIn(context.session())) {
      return F.Promise.pure(null);
    } else {
      final String originalUrl = PlayAuthenticate.storeOriginalUrl(context);
      context.flash().put(MessageKey.FLASH_ERROR_KEY,
                         "You need to login first, to view" + originalUrl);
      return F.Promise.promise(new F.Function0<Result>() {

        @Override
        public Result apply() throws Throwable {
          return redirect(PlayAuthenticate.getResolver().login());
        }
      });
    }
  }

  @Override
  public DynamicResourceHandler getDynamicResourceHandler(final Context context) {
    return null;
  }

  @Override
  public Promise<Subject> getSubject(final Context context) {
    if (!PlayAuthenticate.isLoggedIn(context.session())) {
      return F.Promise.pure(null);
    }
    final AuthUserIdentity u = PlayAuthenticate.getUser(context);
    // Caching might be a good idea here
    return F.Promise.pure((Subject)User.findByAuthUserIdentity(u));  }

  @Override
  public Promise<Result> onAuthFailure(final Context context, final String content) {
    return F.Promise.promise(new F.Function0<Result>() {

      @Override
      public Result apply() throws Throwable {
        return forbidden("Forbidden");
      }
    });
  }

}
