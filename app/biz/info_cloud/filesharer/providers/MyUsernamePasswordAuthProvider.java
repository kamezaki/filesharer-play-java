package biz.info_cloud.filesharer.providers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

import models.LinkedAccount;
import models.TokenAction;
import models.TokenAction.Type;
import models.User;
import biz.info_cloud.filesharer.LocalConfig;

import com.amazonaws.util.StringUtils;
import com.feth.play.module.mail.Mailer.Mail.Body;
import com.feth.play.module.pa.PlayAuthenticate;
import com.feth.play.module.pa.providers.password.UsernamePasswordAuthProvider;
import com.feth.play.module.pa.providers.password.UsernamePasswordAuthUser;

import controllers.MessageKey;
import controllers.routes;
import play.Application;
import play.Logger;
import play.data.Form;
import play.data.validation.Constraints.Email;
import play.data.validation.Constraints.MinLength;
import play.data.validation.Constraints.Required;
import play.i18n.Lang;
import play.mvc.Call;
import play.mvc.Http.Context;

public class MyUsernamePasswordAuthProvider
  extends UsernamePasswordAuthProvider<String, MyLoginUsernamePasswordAuthUser, MyUsernamePasswordAuthUser, MyUsernamePasswordAuthProvider.MyLogin, MyUsernamePasswordAuthProvider.MySignup> {
  private static final int DEFAULT_HTTP_PORT = 80;
  private static final String SETTING_KEY_VERIFICATION_LINK_SECURE =
      SETTING_KEY_MAIL + "." + "verificationLink.secure";
  private static final String SETTING_KEY_PASSWORD_RESET_LINK_SECURE =
      SETTING_KEY_MAIL + "." + "passwordResetLink.secure";
  private static final String SETTING_KEY_LINK_LOGIN_AFTER_PASSWORD_RESET =
      "loginAfterPasswordReset";
  private static final String SETTING_KEY_URL_HOST="urlHost";
  private static final String SETTING_KEY_URL_PORT="urlPort";

  private static final String EMAIL_TEMPLATE_FALLBACK_LANGUAGE = "en";
  
  public static final Form<MySignup> SIGNUP_FORM = Form.form(MySignup.class);
  public static final Form<MyLogin> LOGIN_FORM = Form.form(MyLogin.class);

  public MyUsernamePasswordAuthProvider(Application app) {
    super(app);
  }
  
  @Override
  protected MyLoginUsernamePasswordAuthUser buildLoginAuthUser(
      final MyLogin login, final Context context) {
    return new MyLoginUsernamePasswordAuthUser(login);
  }

  @Override
  protected MyUsernamePasswordAuthUser buildSignupAuthUser(
      final MySignup signup, final Context context) {
    return new MyUsernamePasswordAuthUser(signup);
  }

  @Override
  protected Form<MyLogin> getLoginForm() {
    return LOGIN_FORM;
  }

  @Override
  protected Form<MySignup> getSignupForm() {
    return SIGNUP_FORM;
  }

  private String getPrefferdLangCode(final Context context) {
    final Lang lang = Lang.preferred(context.request().acceptLanguages());
    return lang.code();
  }
  
  private String getEmailName(final User user) {
    return getEmailName(user.email, user.name);
  }
  
  private boolean getSecureSetting(final String key) {
    if (LocalConfig.getForceHttps()) {
      return true;
    }
    return getConfiguration().getBoolean(key);
  }
  
  @Override
  protected String generateVerificationRecord(
      final MyUsernamePasswordAuthUser authUser) {
    return generateVerificationRecord(User.findByAuthUserIdentity(authUser));
  }
  
  protected String generateVerificationRecord(final User user) {
    final String token = generateToken();
    TokenAction.create(Type.EMAIL_VERIFICATION, token, user);
    return token;
  }
  
  private static String generateToken() {
    return UUID.randomUUID().toString();
  }
  
  public void sendPasswordResetMailing(final User user, final Context context) {
    final String token = generatePasswordResetRecord(user);
    final String subject = generatePasswordResetSubject(user, context);
    final Body body = generatePasswordResetMailingBody(token, user, context);
    sendMail(subject, body, getEmailName(user));
  }
  
  protected String generatePasswordResetSubject(final User user, final Context context) {
    return "Reset password";
  }
  
  private String getUrl(Call call, boolean isSecure, Context context) {
    final String host = getConfiguration().getString(SETTING_KEY_URL_HOST);
    if (StringUtils.isNullOrEmpty(host)) {
      return call.absoluteURL(context.request(), isSecure);
    }

    final String schema = isSecure ? "https" : "http";
    final int port = getConfiguration().getInt(SETTING_KEY_URL_PORT, DEFAULT_HTTP_PORT);
    if (port != DEFAULT_HTTP_PORT) {
      return String.format("%s://%s:%d%s", schema, host, port, call.url());
    } else {
      return String.format("%s://%s%s", schema, host, call.url());
    }
  }
  
  protected Body generatePasswordResetMailingBody(final String token, final User user, final Context context) {
    final boolean isSecure = getSecureSetting(SETTING_KEY_PASSWORD_RESET_LINK_SECURE);
    final String url = getUrl(routes.Signup.resetPassword(token), isSecure, context);
    final String langCode = getPrefferdLangCode(context);
    final String html = getEmailTemplate(
        "views.html.account.email.password_reset",
        langCode, url,
        token, user.name, user.email);
    final String text = getEmailTemplate(
        "views.txt.account.email.password_reset", langCode, url, token,
        user.name, user.email);
    return new Body(text, html);
  }

  protected String generatePasswordResetRecord(final User user) {
    final String token = generateToken();
    TokenAction.create(Type.PASSWORD_RESET, token, user);
    return token;
  }
  
  public boolean isLoginAfterPasswordReset() {
    return getConfiguration().getBoolean(SETTING_KEY_LINK_LOGIN_AFTER_PASSWORD_RESET);
  }
  
  public void sendVerifyEmailMailingAfterSignup(final User user, final Context context) {
    final String subject = getVerifyEmailMailingSubjectAfterSignup(user, context);
    final String token = generateVerificationRecord(user);
    final Body body = getVerifyEmailMailingBodyAfterSignup(token, user, context);
    sendMail(subject, body, getEmailName(user));
  }
  
  protected String getVerifyEmailMailingSubjectAfterSignup(final User user, final Context context) {
    return "verify email";
  }
  
  protected Body getVerifyEmailMailingBodyAfterSignup(
      final String token, final User user, final Context context) {
    final boolean isSecure = getSecureSetting(SETTING_KEY_VERIFICATION_LINK_SECURE);
    final String url = getUrl(routes.Signup.verify(token), isSecure, context);
    final String langCode = getPrefferdLangCode(context);
    final String html = getEmailTemplate(
        "views.html.account.email.verify_email",
        langCode, url, token, user.name, user.email);
    final String text = getEmailTemplate(
        "views.txt.account.email.verify_email",
        langCode, url, token, user.name, user.email);

    return new Body(text, html);
  }
   
  @Override
  protected Body getVerifyEmailMailingBody(
      final String token, final MyUsernamePasswordAuthUser authUser, final Context context) {
    final boolean isSecure = getSecureSetting(SETTING_KEY_VERIFICATION_LINK_SECURE);
    final String url = getUrl(routes.Signup.verify(token), isSecure, context);
    
    final String langCode = getPrefferdLangCode(context);
    final String html = getEmailTemplate(
        "views.html.account.signup.email.verify_email",
        langCode, url,
        token, authUser.getName(), authUser.getEmail());
    final String text = getEmailTemplate(
        "views.txt.account.signup.email.verify_email",
        langCode, url,
        token, authUser.getName(), authUser.getEmail());
    return new Body(text, html);
  }

  @Override
  protected String getVerifyEmailMailingSubject(
      final MyUsernamePasswordAuthUser authUser, final Context context) {
    return "Verify signup";
  }

  @Override
  protected UsernamePasswordAuthProvider.LoginResult loginUser(
      final MyLoginUsernamePasswordAuthUser authUser) {
    User user = User.findByAuthUserIdentity(authUser);
    if (user == null) {
      return LoginResult.NOT_FOUND;
    }
    
    if (!user.emailValidated) {
      return LoginResult.USER_UNVERIFIED;
    }
    
    for(final LinkedAccount account : user.linkedAccounts) {
      if (getKey().equals(account.providerKey)) {
        if (authUser.checkPassword(account.providerUserId, authUser.getPassword())) {
          return LoginResult.USER_LOGGED_IN;
        } else {
          return LoginResult.WRONG_PASSWORD;
        }
      }
    }
    return LoginResult.WRONG_PASSWORD;
  }

  @Override
  protected String onLoginUserNotFound(Context context) {
    context.flash().put(MessageKey.FLASH_ERROR_KEY, "Unknown user or password.");
    return super.onLoginUserNotFound(context);
  }

  @Override
  protected UsernamePasswordAuthProvider.SignupResult signupUser(
      final MyUsernamePasswordAuthUser authUser) {
    User user = User.findByAuthUserIdentity(authUser);
    if (user != null) {
      if (user.emailValidated) {
        return SignupResult.USER_EXISTS;
      } else {
        return SignupResult.USER_EXISTS_UNVERIFIED;
      }
    }
    User.create(authUser);
    return SignupResult.USER_CREATED_UNVERIFIED;
  }

  @Override
  protected MyLoginUsernamePasswordAuthUser transformAuthUser(
      final MyUsernamePasswordAuthUser authUser, final Context context) {
    return new MyLoginUsernamePasswordAuthUser(authUser.getEmail());
  }

  @Override
  protected Call userExists(final UsernamePasswordAuthUser authUser) {
    return routes.Signup.exists();
  }

  @Override
  protected Call userUnverified(final UsernamePasswordAuthUser authUser) {
    return routes.Signup.unverified();
  }
  
  public static MyUsernamePasswordAuthProvider getProvider() {
    return (MyUsernamePasswordAuthProvider) PlayAuthenticate.getProvider(PROVIDER_KEY);
  }
  
  protected String getEmailTemplate(
      final String template, final String langCode, final String url, final String token, final String name, final String email) {
    Class<?> clazz = null;
    try {
      clazz = Class.forName(String.format("%s_%s", template, langCode));
    } catch (ClassNotFoundException e) {
      Logger.warn(
          String.format(
              "Template '%s_%s' was not found. Try to use fallback '%s_%s' instead. ",
              template, langCode, template, EMAIL_TEMPLATE_FALLBACK_LANGUAGE)
              );
    }
    
    if (clazz == null) {
      try {
        clazz = Class.forName(String.format("%s_%s", template, EMAIL_TEMPLATE_FALLBACK_LANGUAGE));
      } catch (ClassNotFoundException e) {
        Logger.error(
            String.format(
                "Fallback template '%s_%s' was not found either",
                template, EMAIL_TEMPLATE_FALLBACK_LANGUAGE)
                );
        return null;
      }
    }
    
    try {
      Method htmlRender = clazz.getMethod("render", String.class, String.class, String.class, String.class);
      return htmlRender.invoke(null, url, token, name, email).toString();
    } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      Logger.error("class invoke error", e);
      return null;
    }
  }
  
  public static class MyIdentity {
    @Required
    @Email
    public String email;
    
    public MyIdentity() {
    }
    
    public MyIdentity(final String email) {
      this.email = email;
    }
  }
  
  public static class MyLogin extends MyIdentity
      implements UsernamePasswordAuthProvider.UsernamePassword {

    @Required
    @MinLength(5)
    public String password;
    
    @Override
    public String getEmail() {
      return email;
    }

    @Override
    public String getPassword() {
      return password;
    }
  }
  
  public static class MySignup extends MyLogin {
    
    @Required
    @MinLength(5)
    public String repeatPassword;
    
    @Required
    public String name;
    
    public String validate() {
      if (password == null || !password.equals(repeatPassword)) {
        return "password is not same";
      }
      return null;
    }
  }


}
