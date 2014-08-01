package controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;
import java.util.Map;

import javax.activation.MimetypesFileTypeMap;

import org.apache.commons.io.FileUtils;
import org.mozilla.universalchardet.UniversalDetector;

import play.Logger;
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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class Application extends Controller {
  public static final String ConfigStorePath = "filesharer.store.path";
  public static final String FilePartParam = "file";
  public static final String FallbackModeParam = "fallback-mode";
  public static final String JsonPathParam = "path";
  public static final String DownloadParam = "download";
  
  private static Config config = ConfigFactory.load();
  private static MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
  
  public static Result index() {
    return ok(index.render());
  }
  
  private static StoredFile getFile(String filename) {
    String folder = config.getString(ConfigStorePath);
    FileStoreService service = new FileStoreService(folder);
    return service.getStoredFile(filename);
  }
  
  private static Result returnSharer(StoredFile file)
      throws FileNotFoundException, UnsupportedEncodingException {
    if (!file.exists()) {
      throw new FileNotFoundException(
          String.format("%s is not found", file.getRelativePath()));
    }
    
    String contentType = mimeTypesMap.getContentType(
        file.getAbsolutePath());
    
    SupportContentType type = getSupportContentType(contentType);
    if (type == SupportContentType.TEXT) {
      String encoding = detectFileEncoding(file.getAbsolutePath());
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
    return ok(new FileInputStream(file.getAbsolutePath())).as(contentType);
  }
  
  private static String detectFileEncoding(String path) {
    String encoding = null;
    UniversalDetector detector = null;
    try (FileInputStream fis = new java.io.FileInputStream(path)) {

      byte[] buf = new byte[4096];
      detector = new UniversalDetector(null);
      int nread;
      while ((nread = fis.read(buf)) > 0 && !detector.isDone()) {
        detector.handleData(buf, 0, nread);
      }
      detector.dataEnd();

      encoding = detector.getDetectedCharset();
    } catch (IOException e) {
      Logger.error("error on detect encoding", e);
    } finally {
      if (detector != null) {
        detector.reset();
      }
    }
    return encoding;
  }
  
  private static Result returnSharerWithError(Throwable t) {
    if (t instanceof FileNotFoundException) {
      return notFound(t.toString());
    }
    return badRequest(t.toString());
  }
  
  public static Promise<Result> sharer(String filename) {
    return Promise.promise(() -> getFile(filename))
                  .map(f -> returnSharer(f))
                  .recover(t -> returnSharerWithError(t));
  }
  
  private static Result returnShow(StoredFile file)
      throws IOException {
    if (!file.exists()) {
      throw new FileNotFoundException(
          String.format("%s is not found", file.getRelativePath()));
    }
    
    String contentType = mimeTypesMap.getContentType(
        file.getAbsolutePath());
    SupportContentType type = getSupportContentType(contentType);
    switch (type) {
    case TEXT:
      String encoding = detectFileEncoding(file.getAbsolutePath());
//      if (encoding != null && encoding.length() > 0) {
//        contentType = String.format("%s; charset=%s", contentType, encoding);
//      }
      File textFile = new File(file.getAbsolutePath());
      String textBody = FileUtils.readFileToString(textFile, encoding);
      return ok(showtext.render(
          file.getRelativePath(), file.getOriginalFilename(), textBody));
    case IMAGE:
      return ok(showimage.render(
          file.getRelativePath(), file.getOriginalFilename()));
    case OTHER:
    default:
      return ok(showother.render(
          file.getRelativePath(), file.getOriginalFilename()));
    }
  }
  
  private static SupportContentType getSupportContentType(String contentType) {
    String lowerContentType = contentType.toLowerCase(Locale.US);
    if (lowerContentType.startsWith("text/") ||
        lowerContentType.endsWith("/json")) {
      return SupportContentType.TEXT;
    } else if (lowerContentType.startsWith("image/")) {
      return SupportContentType.IMAGE;
    } else {
      return SupportContentType.OTHER;
    }
    
  }
  
  public static Promise<Result> show(String filename) {
    return Promise.promise(() -> getFile(filename))
        .map(f -> returnShow(f))
        .recover(t -> returnSharerWithError(t));
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
    
    String folder = config.getString("filesharer.store.path");
    FileStoreService service = new FileStoreService(folder);
    StoredFile storedFile = service.saveFile(
        uploadFile.getFile(), uploadFile.getFilename());
    return new F.Tuple<StoredFile, Boolean>(storedFile, Boolean.valueOf(isFallback));
  }
  
  private static Result returnSaveFile(final Tuple<StoredFile, Boolean> tuple) {
    StoredFile storedFile = tuple._1;
    boolean isFallback = tuple._2.booleanValue();
    if (isFallback) {
      return redirect(
          controllers.routes.Application.show(storedFile.getRelativePath()));
    } else {
      ObjectNode result = Json.newObject();
      String sharedPath = controllers.routes.Application.show(
          storedFile.getRelativePath()).toString();
      result.put(JsonPathParam, sharedPath);
      return ok(result);
    }
  }
  
  private static Result returnSaveFileWithError(Throwable t) {
    if (t instanceof MissingFileException) {
      return redirect(controllers.routes.Application.index());
    }
    return badRequest(t.getMessage());
  }
  
  public static Promise<Result> upload() {
    return Promise.promise(() -> saveFile(request()))
                  .map(path -> returnSaveFile(path))
                  .recover(t -> returnSaveFileWithError(t));
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
  
  public enum SupportContentType {
    TEXT, IMAGE, OTHER
  }
}
