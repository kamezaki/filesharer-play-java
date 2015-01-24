package controllers;

import models.TokenAction;
import models.TokenAction.Type;
import models.User;

import com.feth.play.module.pa.controllers.Authenticate;

import play.mvc.Controller;
import play.mvc.Result;
import views.html.account.signup.exists;
import views.html.account.signup.no_token_or_invalid;
import views.html.account.signup.unverified;

public class Signup extends Controller {

  public static Result exists() {
    Authenticate.noCache(response());
    return null;
  }
  
  public static Result resetPassword(final String token) {
    Authenticate.noCache(response());
    return ok(exists.render());
  }
  
  public static Result verify(final String token) {
    Authenticate.noCache(response());
    final TokenAction ta = tokenIsValid(token, Type.EMAIL_VERIFICATION);
    if (ta == null) {
      return badRequest(no_token_or_invalid.render());
    }
    
    final String email = ta.targetUser.email;
    User.verify(ta.targetUser);
    flash(MessageKey.FLASH_MESSAGE_KEY, String.format("email address %s successfully verified.", email));
    if (Application.getLocalUser(session()) != null) {
      return redirect(routes.Application.index());
    } else {
      return redirect(routes.Application.login());
    }
  }
  
  public static Result unverified() {
    Authenticate.noCache(response());
    return ok(unverified.render());
  }
  
  private static TokenAction tokenIsValid(final String token, final Type type) {
    if (token != null && !token.trim().isEmpty()) {
      final TokenAction ta = TokenAction.findByToken(token, type);
      if (ta != null && ta.isValid()) {
        return ta;
      }
    }
    return null;
  }
}
