package controllers;

import models.TokenAction;
import models.TokenAction.Type;
import models.User;
import biz.info_cloud.filesharer.providers.MyLoginUsernamePasswordAuthUser;
import biz.info_cloud.filesharer.providers.MyUsernamePasswordAuthProvider;
import biz.info_cloud.filesharer.providers.MyUsernamePasswordAuthProvider.MyIdentity;

import com.feth.play.module.pa.PlayAuthenticate;
import com.feth.play.module.pa.controllers.Authenticate;
import com.google.common.base.Strings;

import play.data.Form;
import play.libs.F.Promise;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.account.signup.exists;
import views.html.account.signup.no_token_or_invalid;
import views.html.account.signup.password_forgot;
import views.html.account.signup.password_reset;
import views.html.account.signup.unverified;

public class Signup extends Controller {

  public static Result exists() {
    Authenticate.noCache(response());
    return ok(exists.render());
  }
  
  private static final Form<PasswordReset> PASSWORD_RESET_FORM =
      Form.form(PasswordReset.class);
  
  public static Result resetPassword(final String token) {
    Authenticate.noCache(response());
    final TokenAction action = tokenIsValid(token, Type.PASSWORD_RESET);
    if (action == null) {
      return badRequest(no_token_or_invalid.render());
    }
    return ok(password_reset.render(
        PASSWORD_RESET_FORM.fill(new PasswordReset(token))));
  }
  
  public static Result doResetPassword() {
    Authenticate.noCache(response());
    final Form<PasswordReset> filledForm = PASSWORD_RESET_FORM.bindFromRequest();
    if (filledForm.hasErrors()) {
      return badRequest(password_reset.render(filledForm));
    }
    
    final String token = filledForm.get().token;
    final String newPassword =filledForm.get().password;
    final TokenAction action = tokenIsValid(token, Type.PASSWORD_RESET);
    if (action == null) {
      return badRequest(no_token_or_invalid.render());
    }
    final User user = action.targetUser;
    try {
      user.resetPassword(new MyLoginUsernamePasswordAuthUser(newPassword), false);
    } catch (RuntimeException e) {
      flash(MessageKey.FLASH_MESSAGE_KEY, "Your user has not yet been set up for password usage.");
    }
    final boolean login =
        MyUsernamePasswordAuthProvider.getProvider().isLoginAfterPasswordReset();
    if (login) {
      flash(MessageKey.FLASH_MESSAGE_KEY, "Your password has been reset.");
      return PlayAuthenticate.loginAndRedirect(ctx(),
          new MyLoginUsernamePasswordAuthUser(user.email));
    } else {
      flash(MessageKey.FLASH_MESSAGE_KEY,
          "Your password has been reset. Please now log in using your new password.");
      return redirect(routes.Application.index());
    }
  }
  
  public static Promise<Result> verify(final String token) {
    return Promise.promise(() -> {
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
    });
  }
  
  public static Result unverified() {
    Authenticate.noCache(response());
    return ok(unverified.render());
  }
  
  private static final Form<MyIdentity> FORGOT_PASSWORD_FORM = Form.form(MyIdentity.class) ;
  
  public static Promise<Result> forgotPassword(final String email) {
    return Promise.promise(() -> {
      Authenticate.noCache(response());
      Form<MyIdentity> form = FORGOT_PASSWORD_FORM;
      if (email != null && Strings.isNullOrEmpty(email.trim())) {
        form = FORGOT_PASSWORD_FORM.fill(new MyIdentity(email.trim()));
      }
      return ok(password_forgot.render(form));
    });
  }
  
  public static Promise<Result> doForgotPassword() {
    return Promise.promise(() -> {
      Authenticate.noCache(response());
      final Form<MyIdentity> filledForm = FORGOT_PASSWORD_FORM.bindFromRequest();
      if (filledForm.hasErrors()) {
        return badRequest(password_forgot.render(filledForm));
      }
      final String email = filledForm.get().email.trim();
      flash(MessageKey.FLASH_MESSAGE_KEY,
          String.format("Instructions on how to reset your password have been sent to %s", email));
      final User user = User.findByEmail(email);
      if (user != null) {
        final MyUsernamePasswordAuthProvider provider =
            MyUsernamePasswordAuthProvider.getProvider();
        if (user.emailValidated) {
          provider.sendPasswordResetMailing(user, ctx());
        } else {
          flash(MessageKey.FLASH_MESSAGE_KEY,
              "Your account has not been verified, yet. An e-mail including instructions on how to verify it has been sent out. Retry resetting your password afterwards.");
          provider.sendVerifyEmailMailingAfterSignup(user, ctx());
        }
      }
      return redirect(routes.Application.index());
    });
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
  
  public static class PasswordReset extends Account.PasswordChange {
    public String token;
    
    public PasswordReset() {
    }
    
    public PasswordReset(final String token) {
      this.token = token;
    }
    
    public String getToken() {
      return token;
    }

    public void setToken(final String token) {
      this.token = token;
    }

  }
}
