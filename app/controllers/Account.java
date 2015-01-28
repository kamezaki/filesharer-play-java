package controllers;

import play.data.validation.Constraints.MinLength;
import play.data.validation.Constraints.Required;
import play.mvc.Controller;

public class Account extends Controller {

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
