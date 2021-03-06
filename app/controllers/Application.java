package controllers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javax.activation.MimetypesFileTypeMap;

import models.User;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;

import play.Logger;
import play.Routes;
import play.data.Form;
import play.filters.csrf.AddCSRFToken;
import play.filters.csrf.RequireCSRFCheck;
import play.libs.F;
import play.libs.F.Promise;
import play.libs.F.Tuple;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Http.Request;
import play.mvc.Http.Session;
import play.mvc.Result;
import views.html.accesslog;
import views.html.index;
import views.html.login;
import views.html.profile;
import views.html.showimage;
import views.html.showother;
import views.html.showtext;
import views.html.signup;
import views.html.uploadlist;
import views.html.error.notfound;
import be.objectify.deadbolt.java.actions.Group;
import be.objectify.deadbolt.java.actions.Restrict;
import biz.info_cloud.filesharer.providers.MyUsernamePasswordAuthProvider;
import biz.info_cloud.filesharer.providers.MyUsernamePasswordAuthProvider.MyLogin;
import biz.info_cloud.filesharer.providers.MyUsernamePasswordAuthProvider.MySignup;
import biz.info_cloud.filesharer.service.FileStoreService;
import biz.info_cloud.filesharer.service.FileStoreService.StoredFile;
import biz.info_cloud.web.utils.ContentsUtils;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.feth.play.module.pa.PlayAuthenticate;
import com.feth.play.module.pa.controllers.Authenticate;
import com.feth.play.module.pa.providers.oauth2.google.GoogleAuthProvider;
import com.feth.play.module.pa.user.AuthUser;

public class Application extends Controller {
  public static final String FilePartParam = "file";
  public static final String FallbackModeParam = "fallback-mode";
  public static final String JsonPathParam = "path";
  public static final String DownloadParam = "download";
  public static final String DeleteParam = "deleteFiles";
  
  public static final String SPNEGO_PROVIDER_KEY = "spnego";
  
  public static Map<String, String> providerMap = initializeProviderMap();
  private static Map<String, String> initializeProviderMap() {
    Map<String, String> map = new HashMap<>();
    map.put(GoogleAuthProvider.PROVIDER_KEY, "images/sign-in-with-google.png");
    return Collections.unmodifiableMap(map);
  }
  
  private static MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
  
  public static Result index() {
    return ok(index.render());
  }
  
  public static Result jsRoutes() {
    return ok(Routes.javascriptRouter(
        "jsRoutes",
        controllers.routes.javascript.Signup.forgotPassword()))
        .as("text/javascript");
  }
  
  public static Result login() {
    return ok(login.render(MyUsernamePasswordAuthProvider.LOGIN_FORM));
  }
  
  public static Promise<Result> doLogin() {
    return Promise.promise(() -> {
      Authenticate.noCache(response());
      final Form<MyLogin> filledForm =
          MyUsernamePasswordAuthProvider.LOGIN_FORM.bindFromRequest();
      if (filledForm.hasErrors()) {
        return badRequest(login.render(filledForm));
      }
      return MyUsernamePasswordAuthProvider.handleLogin(ctx());
    });
  }
  
  public static Result signup() {
    return ok(signup.render(MyUsernamePasswordAuthProvider.SIGNUP_FORM));
  }
  
  public static Promise<Result> doSignup() {
    return Promise.promise(() -> {
      Authenticate.noCache(response());
      final Form<MySignup> filledForm = MyUsernamePasswordAuthProvider.SIGNUP_FORM.bindFromRequest();
      if (filledForm.hasErrors()) {
        return badRequest(signup.render(filledForm));
      }
      return MyUsernamePasswordAuthProvider.handleSignup(ctx());
    });
  }
  
  @Restrict(@Group(UserRole.USER))
  public static Promise<Result> profile() {
    return Promise.promise(() -> {
      final User localUser = getLocalUser(session());
      return ok(profile.render(localUser));
    });
  }
  
  public static Promise<Result> upload() {
    return Promise.promise(() -> saveFile(request()))
                  .map(path -> responseUpload(path))
                  .recover(t -> handleUploadError(t));
  }
  
  @AddCSRFToken
  @Restrict(@Group(UserRole.USER))
  public static Promise<Result> uploadList() {
    final User user = getLocalUser(session());
    return Promise.promise(() -> new FileStoreService().getUploadList(user))
                  .map(list -> {
                    Authenticate.noCache(response());
                    return ok(uploadlist.render(list));
                  });
  }
  
  @Restrict(@Group(UserRole.USER))
  public static Promise<Result> accessLog() {
    final User user = getLocalUser(session());
    return Promise.promise(() -> new FileStoreService().getAccessLog(user))
                  .map(list -> {
                    Authenticate.noCache(response());
                    return ok(accesslog.render(list));
                  });
  }

  @RequireCSRFCheck
  @Restrict(@Group(UserRole.USER))
  public static Promise<Result> delete() {
    return Promise.promise(() -> {
      deleteOwnedFiles(request(), session());
      return redirect(routes.Application.uploadList());
    });
  }
  
  public static Result oAuthDenied(final String session) {
    Authenticate.noCache(response());
    flash(MessageKey.FLASH_ERROR_KEY,
          "You need to accept the OAuth connection in order to use this website!");
    return redirect(routes.Application.index());
  }
  
  public static Promise<Result> sharer(final String filename) {
    return Promise.promise(() -> getFile(filename))
                  .map(f -> updateAccess(f, session()))
                  .map(f -> responseSharer(f))
                  .recover(t -> handleError(t));
  }
  
  @AddCSRFToken
  public static Promise<Result> show(final String filename) {
    return Promise.promise(() -> getFile(filename))
                  .map(f -> updateAccess(f, session()))
                  .map(f -> responseShow(f))
                  .recover(t -> handleError(t));
  }
  
  public static User getLocalUser(final Session session) {
    final AuthUser authUser = PlayAuthenticate.getUser(session);
    return User.findByAuthUserIdentity(authUser);
  }
  
  public static boolean isMyOwn(final String relativePath, final Session session) {
    User user = getLocalUser(session);
    if (user == null) {
      return false;
    }
    final FileStoreService service = new FileStoreService();
    StoredFile storedFile = service.getStoredFile(relativePath);
    if (!storedFile.exists()) {
      return false;
    }
    return service.isOwndFile(storedFile, user);
  }
  
  private static StoredFile getFile(final String filename) {
    final FileStoreService service = new FileStoreService();
    return service.getStoredFile(filename);
  }
  
  private static StoredFile updateAccess(final StoredFile storedFile, final Session session) {
    User user = getLocalUser(session);
    if (user != null) {
      storedFile.updateAccessLog(getLocalUser(session));
    }
    return storedFile;
  }

  private static void deleteOwnedFiles(final Request request, final Session session) {
    Map<String, String[]> params = request.body().asFormUrlEncoded();
    if (params == null
        || params.isEmpty()
        || !params.containsKey(DeleteParam)) {
      Logger.debug("could not find " + DeleteParam);
      return;
    }
    
    final FileStoreService service = new FileStoreService();
    Arrays.asList(params.get(DeleteParam))
          .stream()
          .map(filename -> service.getStoredFile(filename))
          .filter(storedFile -> storedFile.exists())
          .filter(storedFile -> service.isOwndFile(storedFile, getLocalUser(session)))
          .forEach(storedFile -> service.delete(storedFile));
  }

  private static Result responseSharer(final StoredFile file)
      throws IOException {
    if (!file.exists()) {
      throw new FileNotFoundException(
          String.format("%s is not found", file.getKeyPath()));
    }
    
    String contentType =
        mimeTypesMap.getContentType(file.getOriginalFilename());
    
    if (getShowType(contentType) == ShowType.TEXT) {
      String encoding = ContentsUtils.detectEncoding(file.getInputStream());
      if (encoding != null && encoding.length() > 0) {
        contentType = String.format("%s; charset=%s", contentType, encoding);
      }
    }
    
    String mode = "inline";
    if (request().getQueryString(DownloadParam) != null) {
      mode = "attachment";
    }
    String filename = URLEncoder.encode(file.getOriginalFilename(), "UTF-8");
    response().setHeader("Content-Disposition",
        String.format("%s; filename*=UTF-8\'\'%s", mode, filename));
    return ok(file.getInputStream()).as(contentType);
  }
  
  
  private static Result responseShow(final StoredFile file)
      throws IOException {
    Authenticate.noCache(response());
    if (!file.exists()) {
      throw new FileNotFoundException(
          String.format("%s is not found", file.getKeyPath()));
    }
    
    String contentType = mimeTypesMap.getContentType(
        file.getOriginalFilename());
    switch (getShowType(contentType)) {
    case TEXT:
      String encoding = ContentsUtils.detectEncoding(
          file.getInputStream());
      String body = StringEscapeUtils.escapeXml(
          IOUtils.toString(file.getInputStream(), encoding));
      return ok(showtext.render(
          file.getKeyPath(),
          file.getOriginalFilename(),
          body));
    case IMAGE:
      return ok(showimage.render(
          file.getKeyPath(), file.getOriginalFilename()));
    case OTHER:
    default:
      return ok(showother.render(
          file.getKeyPath(), file.getOriginalFilename()));
    }
  }
  
  private static Tuple<StoredFile, Boolean> saveFile(final Request request)
      throws IOException {
    MultipartFormData body = request().body().asMultipartFormData();
    if (body == null) {
      throw new MissingFileException("missing mutipart body");
    }
    FilePart uploadFile = body.getFile(FilePartParam);
    if (uploadFile == null) {
      throw new MissingFileException("missing upload file");
    }
    Map<String, String[]> queryMap = body.asFormUrlEncoded();
    boolean isFallback = false;
    if (queryMap != null &&
        queryMap.containsKey(FallbackModeParam)) {
      isFallback = true;
    }
    
    FileStoreService service = new FileStoreService();
    StoredFile storedFile = service.saveFile(
        Optional.ofNullable(getLocalUser(session())),
        uploadFile.getFile(),
        uploadFile.getFilename());
    return new F.Tuple<StoredFile, Boolean>(
        storedFile, Boolean.valueOf(isFallback));
  }
  
  private static Result responseUpload(final Tuple<StoredFile, Boolean> tuple) {
    StoredFile storedFile = tuple._1;
    boolean isFallback = tuple._2.booleanValue();
    if (isFallback) {
      return redirect(
          controllers.routes.Application.show(storedFile.getKeyPath()));
    } else {
      ObjectNode result = Json.newObject();
      String sharedPath = controllers.routes.Application.show(
          storedFile.getKeyPath()).toString();
      result.put(JsonPathParam, sharedPath);
      return ok(result);
    }
  }
  
  private static Result handleError(final Throwable t) {
    Logger.debug("handleError", t);
    if (t instanceof FileNotFoundException) {
      return notFound(notfound.render(request()));
    }
    return badRequest(t.toString());
  }
  
  private static Result handleUploadError(final Throwable t) {
    Logger.debug("handleUploadedError", t);
    if (t instanceof MissingFileException) {
      return redirect(controllers.routes.Application.index());
    }
    return badRequest(t.getMessage());
  }
  
  private static ShowType getShowType(final String contentType) {
    String lowerContentType = contentType.toLowerCase(Locale.US);
    if (lowerContentType.startsWith("text/")) {
      return ShowType.TEXT;
    } else if (lowerContentType.startsWith("image/")) {
      return ShowType.IMAGE;
    } else {
      return ShowType.OTHER;
    }
  }
  
  public static class MissingFileException extends IOException {
    private static final long serialVersionUID = -6916009604118436445L;

    public MissingFileException(final String message, final Throwable cause) {
      super(message, cause);
    }

    public MissingFileException(final String message) {
      super(message);
    }

    public MissingFileException(final Throwable cause) {
      super(cause);
    }
  }
  
  public enum ShowType {
    TEXT, IMAGE, OTHER
  }
}
