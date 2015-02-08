package biz.info_cloud.filesharer.service.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Locale;

import play.Logger;
import biz.info_cloud.filesharer.LocalConfig;

import com.google.common.io.Files;

public class FileStorage implements StorageService {
  private static final String DateFormat = "yyyyMMdd";
  private static DateTimeFormatter DateFormatter =
      DateTimeFormatter.ofPattern(DateFormat, Locale.US);

  @Override
  public InputStream getInputStream(final String storedName)
      throws IOException{
    File file = getAbsoluteFile(storedName);
    if (!file.exists()) {
      throw new FileNotFoundException(
          String.format("[%s] was not exist", file.getAbsoluteFile()));
    }
    return new FileInputStream(file);
  }

  @Override
  public String save(final File original, final String filename)
      throws IOException {
    File parent = getDairyDirectory();
    File writeFile = new File(parent, filename);
    Files.copy(original, writeFile);
    return parent.getName() + "/" + filename;
  }

  @Override
  public void delete(final String storedName) throws IOException{
    File file = getAbsoluteFile(storedName);
    if (file.exists()) {
      file.delete();
    }
  }
  
  @Override
  public void cleanup(final LocalDateTime deleteDate) {
    if (deleteDate == null) {
      return;
    }
    
    final File storeDir = new File(getTopDirectory());
    if (!storeDir.exists() || !storeDir.isDirectory()) {
      return;
    }
    
    File[] listFiles = storeDir.listFiles();
    if (listFiles == null || listFiles.length < 1) {
      return;
    }
    
    Arrays.asList(listFiles)
          .stream()
          .filter(file -> isCleanupTargetDirectory(file, deleteDate))
          .forEach(file -> cleanupDirectory(file));
  }
  
  private boolean isCleanupTargetDirectory(
      final File dir, final LocalDateTime deleteDate) {
    if (dir == null || !dir.isDirectory()) {
      return false;
    }
    try {
      LocalDate dirDate = LocalDate.parse(dir.getName(), DateFormatter);
      return dirDate.atTime(0, 0, 0).isBefore(deleteDate);
    } catch (DateTimeParseException e) {
      Logger.info(String.format("Unknown directory name : [%s]", dir.getPath()), e);
      return false;
    }
  }
  
  private void cleanupDirectory(final File file) {
    if (file == null) {
      return;
    }
    
    if (file.isDirectory()) {
      File[] list = file.listFiles();
      if (list != null) {
        Arrays.asList(list)
              .stream()
              .forEach(child -> cleanupDirectory(child));
      }
    }
    file.delete();
    return;
  }

  private String getTopDirectory() {
    return LocalConfig.getStorePath();
  }
  
  private File getAbsoluteFile(final String path) throws IOException {
    File file = new File(getTopDirectory(), path);
    return file;
  }
  
  private File getDairyDirectory() throws IOException {
    String dailyDirectoryName = getCalendarDirectory(LocalDateTime.now());
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
  
  private String getCalendarDirectory(final LocalDateTime dt) {
    return dt.format(DateFormatter);
  }
}
