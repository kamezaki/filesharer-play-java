package controllers;


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import javax.activation.MimetypesFileTypeMap;

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
import biz.info_cloud.filesharer.service.FileStoreService;
import biz.info_cloud.filesharer.service.FileStoreService.StoredFile;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class Application extends Controller {
  public static final String storePath = "filesharer.store.path";
  private static Config config = ConfigFactory.load();
  private static MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
  
  public static Result index() {
    return ok(index.render("Your new application is ready."));
  }
  
  
  private static StoredFile getFile(String filename) {
    String folder = config.getString(storePath);
    FileStoreService service = new FileStoreService(folder);
    return service.getStoredFile(filename);
  }
  
  private static Result returnSharer(StoredFile file)
      throws FileNotFoundException {
    String contentType = mimeTypesMap.getContentType(
        file.getAbsolutePath());
    if (!file.exists()) {
      throw new FileNotFoundException(
          String.format("%s is not found", file.getRelativePath()));
    }
    return ok(new FileInputStream(file.getAbsolutePath())).as(contentType);
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
  
  private static Tuple<StoredFile, Boolean> saveFile(
      final Request request) throws IOException {
    MultipartFormData body = request().body().asMultipartFormData();
    if (body == null) {
      throw new IOException("missing mutipart body");
    }
    FilePart uploadFile = body.getFile("file");
    if (uploadFile == null) {
      throw new IOException("missing upload file");
    }
    Map<String, String[]> queryMap = body.asFormUrlEncoded();
    boolean isFallback = false;
    if (queryMap != null &&
        queryMap.containsKey("fallback-mode")) {
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
          controllers.routes.Application.sharer(storedFile.getRelativePath()));
    } else {
      ObjectNode result = Json.newObject();
      result.put("path", "/sharer/" + storedFile.getRelativePath());
      return ok(result);
    }
  }
  
  private static Result returnSaveFileWithError(Throwable t) {
    return badRequest(t.getMessage());
  }
  
  public static Promise<Result> upload() {
    return Promise.promise(() -> saveFile(request()))
                  .map(path -> returnSaveFile(path))
                  .recover(t -> returnSaveFileWithError(t));
  }
}
