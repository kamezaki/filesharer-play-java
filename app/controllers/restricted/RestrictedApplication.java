package controllers.restricted;

import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security.Authenticated;
import views.html.index;

@Authenticated(Secured.class)
public class RestrictedApplication extends Controller {
  public static Result index() {
    return ok(index.render());
  }
}
