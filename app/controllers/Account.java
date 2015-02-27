package controllers;


import models.User;
import play.data.Form;
import play.data.format.Formats.NonEmpty;
import play.data.validation.Constraints.MinLength;
import play.data.validation.Constraints.Required;
import play.libs.F.Promise;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.account.ask_link;
import views.html.account.ask_merge;
import views.html.account.link;
import views.html.account.password_change;
import views.html.account.unverified;
import be.objectify.deadbolt.java.actions.Group;
import be.objectify.deadbolt.java.actions.Restrict;
import be.objectify.deadbolt.java.actions.SubjectPresent;
import biz.info_cloud.filesharer.providers.MyUsernamePasswordAuthProvider;
import biz.info_cloud.filesharer.providers.MyUsernamePasswordAuthUser;

import com.feth.play.module.pa.PlayAuthenticate;
import com.feth.play.module.pa.controllers.Authenticate;
import com.feth.play.module.pa.user.AuthUser;

public class Account extends Controller {

  private static final Form<Accept> ACCEPT_FORM = Form.form(Accept.class);
  private static final Form<PasswordChange> PASSWORD_CHANGE_FORM =
      Form.form(PasswordChange.class);
  
  @SubjectPresent
  public static Result link() {
    Authenticate.noCache(response());
    return ok(link.render());
  }
  
  @SubjectPresent
  public static Promise<Result> askLink() {
    return Promise.promise(() -> {
      Authenticate.noCache(response());
      final AuthUser authUser = PlayAuthenticate.getLinkUser(session());
      if (authUser == null) {
        return redirect(routes.Application.index());
      }
      return ok(ask_link.render(ACCEPT_FORM, authUser));
    });
  }
  
  @SubjectPresent
  public static Promise<Result> doLink() {
    return Promise.promise(() -> {
      Authenticate.noCache(response());
      final AuthUser authUser = PlayAuthenticate.getLinkUser(session());
      if (authUser == null) {
        return redirect(routes.Application.index());
      }
      
      final Form<Accept> filledForm = ACCEPT_FORM.bindFromRequest();
      if (filledForm.hasErrors()) {
        return badRequest(ask_link.render(filledForm, authUser));
      }
      final boolean link = filledForm.get().accept;
      if (link) {
        flash(MessageKey.FLASH_MESSAGE_KEY, "Account linked successfully");
      }
      return PlayAuthenticate.link(ctx(), link);
    });
  }
  
  @SubjectPresent
  public static Promise<Result> askMerge() {
    return Promise.promise(() -> {
      Authenticate.noCache(response());
      final AuthUser aUser = PlayAuthenticate.getLinkUser(session());
      final AuthUser bUser = PlayAuthenticate.getMergeUser(session());
      if (bUser == null) {
        return redirect(routes.Application.index());
      }
      return ok(ask_merge.render(ACCEPT_FORM, aUser, bUser));
    });
  }
  
  @SubjectPresent
  public static Promise<Result> doMerge() {
    return Promise.promise(() -> {
      Authenticate.noCache(response());
      final AuthUser aUser = PlayAuthenticate.getLinkUser(session());
      final AuthUser bUser = PlayAuthenticate.getMergeUser(session());
      if (bUser == null) {
        return redirect(routes.Application.index());
      }
      final Form<Accept> filledForm = ACCEPT_FORM.bindFromRequest();
      if (filledForm.hasGlobalErrors()) {
        return badRequest(ask_merge.render(filledForm, aUser, bUser));
      }
      final boolean merge = filledForm.get().accept;
      if (merge) {
        flash(MessageKey.FLASH_MESSAGE_KEY, "Accounts merged successfully");
      }
      return PlayAuthenticate.merge(ctx(), merge);
    });
  }
  
  @Restrict(@Group(UserRole.USER))
  public static Result verifyEmail() {
    Authenticate.noCache(response());
    final User user = Application.getLocalUser(session());
    if (user.emailValidated) {
      flash(MessageKey.FLASH_MESSAGE_KEY,
          "Your email has already been validated.");
    } else if (user.email != null && !user.email.trim().isEmpty()) {
      flash(MessageKey.FLASH_MESSAGE_KEY,
          String.format("Instructions on how to verify your e-mail address have been sent to %s", user.email.trim()));
      MyUsernamePasswordAuthProvider.getProvider()
                                    .sendVerifyEmailMailingAfterSignup(user, ctx());
    } else {
      flash(MessageKey.FLASH_MESSAGE_KEY, 
          "You need to set an e-mail address first.");
    }
    return redirect(routes.Application.profile());
  }
  
  @Restrict(@Group(UserRole.USER))
  public static Promise<Result> changePassword() {
    return Promise.promise(() -> {
      Authenticate.noCache(response());
      final User user = Application.getLocalUser(session());
      if (!user.emailValidated) {
        return ok(unverified.render());
      }
      return ok(password_change.render(PASSWORD_CHANGE_FORM));
    });
  }
  
  @Restrict(@Group(UserRole.USER))
  public static Promise<Result> doChangePassword() {
    return Promise.promise(() -> {
      Authenticate.noCache(response());
      final Form<PasswordChange> filledForm =
          PASSWORD_CHANGE_FORM.bindFromRequest();
      if (filledForm.hasErrors()) {
        return badRequest(password_change.render(filledForm));
      }
      final User user = Application.getLocalUser(session());
      final String newPassword = filledForm.get().password;
      user.changePassword(
          new MyUsernamePasswordAuthUser(newPassword), true);
      flash(MessageKey.FLASH_MESSAGE_KEY, 
          "Password has been changed successfully.");
      return redirect(routes.Application.profile());
    });
  }
  
  public static class Accept {
    @Required
    @NonEmpty
    public Boolean accept;

    public Boolean getAccept() {
      return accept;
    }

    public void setAccept(final Boolean accept) {
      this.accept = accept;
    }
  }
  
  public static class PasswordChange {
    @Required
    @MinLength(5)
    public String password;
    
    @Required
    @MinLength(5)
    public String repeatPassword;
    
    public String validate() {
      if (password == null || !password.equals(repeatPassword)) {
        return "Password do not match";
      }
      return null;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(final String password) {
      this.password = password;
    }

    public String getRepeatPassword() {
      return repeatPassword;
    }

    public void setRepeatPassword(final String repeatPassword) {
      this.repeatPassword = repeatPassword;
    }
  }
}
