package controllers;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.UUID;

import javax.activation.MimetypesFileTypeMap;

import play.Logger;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;
import views.html.index;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.io.Files;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class Application extends Controller {
  private static Config config = ConfigFactory.load();
  private static MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
  
  public static Result index() {
    return ok(index.render("Your new application is ready."));
  }
  
  public static Result sharer(String file) {
    String folder = config.getString("filesharer.store.path");
    File target = new File(folder, file);
    String contentType = mimeTypesMap.getContentType(target);
    try {
      return ok(new FileInputStream(target)).as(contentType);
    } catch (FileNotFoundException e) {
      Logger.info("file not found %s", target.getAbsolutePath());
      flash("error", "read file");
      return redirect(routes.Application.index());
    }
  }
  
  public static Result upload() {
    MultipartFormData body = request().body().asMultipartFormData();
    FilePart uploadFile = body.getFile("file");
    if (uploadFile == null) {
      flash("error", "Missing file");
      return redirect(routes.Application.index());
    }
    String filename = uploadFile.getFilename();
    int index = filename.lastIndexOf(".");
    String ext = "";
    if (index >= 0) {
      ext = filename.substring(index);
    }
    String contentType = uploadFile.getContentType();
    File file = uploadFile.getFile();
    Logger.debug(String.format("filename %s contentType %s ext %s", filename, contentType, ext));

    String folder = ConfigFactory.load().getString("filesharer.store.path");
    String saveFilename = UUID.randomUUID().toString() + ext;
    File writeFile = new File(folder, saveFilename);
    try {
      Files.copy(file, writeFile);
    } catch (IOException e) {
      Logger.error("write file error", e);
      flash("error", "write file");
      return redirect(routes.Application.index());
    }
    
    Logger.debug(String.format("folder %s", folder));
    ObjectNode result = Json.newObject();
    result.put("path", String.format("/sharer/%s", saveFilename));
    return ok(result);
  }

}
