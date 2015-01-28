package controllers;


import be.objectify.deadbolt.java.actions.SubjectPresent;

import com.feth.play.module.pa.PlayAuthenticate;
import com.feth.play.module.pa.controllers.Authenticate;
import com.feth.play.module.pa.user.AuthUser;

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

public class Account extends Controller {

  private static final Form<Accept> ACCEPT_FORM = Form.form(Accept.class);
  
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
