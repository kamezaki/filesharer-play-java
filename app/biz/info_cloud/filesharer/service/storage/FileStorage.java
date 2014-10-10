package biz.info_cloud.filesharer.service.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import biz.info_cloud.filesharer.LocalConfig;

import com.google.common.io.Files;

public class FileStorage implements StorageService {

  @Override
  public InputStream getInputStream(String storedName)
      throws IOException{
    File file = getAbsoluteFile(storedName);
    if (!file.exists()) {
      throw new FileNotFoundException(
          String.format("[%s] was not exist", file.getAbsoluteFile()));
    }
    return new FileInputStream(file);
  }

  @Override
  public String save(File original, String filename)
      throws IOException {
    File parent = getDairyDirectory();
    File writeFile = new File(parent, filename);
    Files.copy(original, writeFile);
    return parent.getName() + "/" + filename;
  }

  @Override
  public void delete(String storedName) throws IOException{
    File file = getAbsoluteFile(storedName);
    if (file.exists()) {
      file.delete();
    }
  }
  
  private String getTopDirectory() {
    return LocalConfig.getStorePath();
  }
  
  private File getAbsoluteFile(String path) throws IOException {
    File file = new File(getTopDirectory(), path);
    return file;
  }
  
  private File getDairyDirectory() throws IOException {
    Calendar cal = Calendar.getInstance();
    String dailyDirectoryName = getCalendarDirectory(cal);
    File directory = new File(getTopDirectory(), dailyDirectoryName);
    if (!directory.exists()) {
      directory.mkdir();
    }
    if (!directory.isDirectory()) {
      throw new IOException(
          String.format("[%s] was not directory", directory.getAbsoluteFile()));
    }
    return directory;
  }
  
  private String getCalendarDirectory(Calendar cal) {
    SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd", Locale.US);
    return format.format(cal.getTime());
  }

}
