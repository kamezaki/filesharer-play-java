package controllers;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
      return notFound(String.format("%s not found", file));
    }
  }
  
  public static Result upload() {
    MultipartFormData body = request().body().asMultipartFormData();
    try {
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
      File file = uploadFile.getFile();

      // save to store
      String folder = ConfigFactory.load().getString("filesharer.store.path");
      String saveFilename = UUID.randomUUID().toString() + ext;
      File writeFile = new File(folder, saveFilename);
      Files.copy(file, writeFile);
      
      ObjectNode result = Json.newObject();
      result.put("path", String.format("/sharer/%s", saveFilename));
      return ok(result);
      
    } catch(Exception e) {
      Logger.error("unknown error", e);
      return redirect(routes.Application.index());
    }
  }

}
