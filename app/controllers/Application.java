package controllers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Locale;
import java.util.Map;

import javax.activation.MimetypesFileTypeMap;

import org.apache.commons.io.IOUtils;

import play.libs.F;
import play.libs.F.Promise;
import play.libs.F.Tuple;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Http.Request;
import play.mvc.Result;
import views.html.index;
import views.html.showimage;
import views.html.showother;
import views.html.showtext;
import biz.info_cloud.filesharer.service.FileStoreService;
import biz.info_cloud.filesharer.service.FileStoreService.StoredFile;
import biz.info_cloud.web.utils.ContentsUtils;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class Application extends Controller {
  public static final String FilePartParam = "file";
  public static final String FallbackModeParam = "fallback-mode";
  public static final String JsonPathParam = "path";
  public static final String DownloadParam = "download";
  
  private static MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
  
  public static Result index() {
    return ok(index.render());
  }
  
  public static Promise<Result> upload() {
    return Promise.promise(() -> saveFile(request()))
                  .map(path -> responseUpload(path))
                  .recover(t -> handleUploadError(t));
  }
  
  public static Promise<Result> sharer(String filename) {
    return Promise.promise(() -> getFile(filename))
                  .map(f -> responseSharer(f))
                  .recover(t -> handleErrro(t));
  }
  
  public static Promise<Result> show(String filename) {
    return Promise.promise(() -> getFile(filename))
                  .map(f -> responseShow(f))
                  .recover(t -> handleErrro(t));
  }
  
  private static StoredFile getFile(String filename) {
    FileStoreService service = new FileStoreService();
    return service.getStoredFile(filename);
  }
  
  private static Result responseSharer(StoredFile file)
      throws IOException {
    if (!file.exists()) {
      throw new FileNotFoundException(
          String.format("%s is not found", file.getKeyPath()));
    }
    
    String contentType = mimeTypesMap.getContentType(
        file.getOriginalFilename());
    
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
  
  
  private static Result responseShow(StoredFile file)
      throws IOException {
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
      return ok(showtext.render(
          file.getKeyPath(),
          file.getOriginalFilename(),
          IOUtils.toString(file.getInputStream(), encoding)));
    case IMAGE:
      return ok(showimage.render(
          file.getKeyPath(), file.getOriginalFilename()));
    case OTHER:
    default:
      return ok(showother.render(
          file.getKeyPath(), file.getOriginalFilename()));
    }
  }
  
  private static Tuple<StoredFile, Boolean> saveFile(
      final Request request) throws IOException {
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
        uploadFile.getFile(), uploadFile.getFilename());
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
  
  private static Result handleErrro(Throwable t) {
    if (t instanceof FileNotFoundException) {
      return notFound(t.toString());
    }
    return badRequest(t.toString());
  }
  
  private static Result handleUploadError(Throwable t) {
    if (t instanceof MissingFileException) {
      return redirect(controllers.routes.Application.index());
    }
    return badRequest(t.getMessage());
  }
  
  private static ShowType getShowType(String contentType) {
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

    public MissingFileException(String message, Throwable cause) {
      super(message, cause);
    }

    public MissingFileException(String message) {
      super(message);
    }

    public MissingFileException(Throwable cause) {
      super(cause);
    }
  }
  
  public enum ShowType {
    TEXT, IMAGE, OTHER
  }
}
