package controllers;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.UUID;

import javax.activation.MimetypesFileTypeMap;

import play.Logger;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Http.Request;
import play.mvc.Result;
import views.html.index;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.io.Files;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class Application extends Controller {
  public static final String storePath = "filesharer.store.path";
  private static Config config = ConfigFactory.load();
  private static MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
  
  public static Result index() {
    return ok(index.render("Your new application is ready."));
  }
  
  
  private static File getFile(String filename) {
    String folder = config.getString(storePath);
    return new File(folder, filename);
  }
  
  private static Result returnSharer(File file) throws FileNotFoundException {
    String contentType = mimeTypesMap.getContentType(file);
    return ok(new FileInputStream(file)).as(contentType);
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
  
  public static Result sharer2(String file) {
    String folder = config.getString(storePath);
    File target = new File(folder, file);
    String contentType = mimeTypesMap.getContentType(target);
    try {
      return ok(new FileInputStream(target)).as(contentType);
    } catch (FileNotFoundException e) {
      Logger.info("file not found %s", target.getAbsolutePath());
      flash("error", "read file");
      return notFound(String.format("%s not found", file));
    }
  }
  
  private static String saveFile(final Request request) throws IOException {
    MultipartFormData body = request().body().asMultipartFormData();
    FilePart uploadFile = body.getFile("file");
    if (uploadFile == null) {
      throw new IOException("missing upload file");
    }
    String filename = uploadFile.getFilename();
    int index = filename.lastIndexOf(".");
    String ext = "";
    if (index >= 0) {
      ext = filename.substring(index);
    }
    File file = uploadFile.getFile();

    // save to store
    String folder = config.getString("filesharer.store.path");
    String saveFilename = UUID.randomUUID().toString() + ext;
    File writeFile = new File(folder, saveFilename);
    Files.copy(file, writeFile);
    return saveFilename;
  }
  
  private static Result returnSaveFile(final String path) {
    ObjectNode result = Json.newObject();
    result.put("path", String.format("/sharer/%s", path));
    return ok(result);
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
