package controllers.restricted;

import com.feth.play.module.pa.PlayAuthenticate;
import com.feth.play.module.pa.user.AuthUser;

import controllers.MessageKey;
import controllers.routes;
import play.mvc.Http.Context;
import play.mvc.Result;
import play.mvc.Security.Authenticator;

public class Secured extends Authenticator {

  @Override
  public String getUsername(Context context) {
    AuthUser user = PlayAuthenticate.getUser(context);
    if (user != null) {
      return user.getId();
    } else {
      return null;
    }
  }

  @Override
  public Result onUnauthorized(Context context) {
    context.flash()
           .put(MessageKey.FLASH_MESSAGE_KEY, "Nice try, but you need to log in first");
    return redirect(routes.Application.index());
  }

}
