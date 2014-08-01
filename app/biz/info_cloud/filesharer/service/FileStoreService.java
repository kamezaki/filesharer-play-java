package biz.info_cloud.filesharer.service;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import models.ShareFileEntity;
import play.Logger;

import com.google.common.io.Files;

public class FileStoreService {
  private static final String DirectoryFormat = "yyyyMMdd";
  private String storeTopDirName;

  public FileStoreService(final String topDirName) {
    this.storeTopDirName = topDirName;
  }
  
  public StoredFile saveFile(final File fromFile, final String fromFilename)
      throws IOException {
    // check extension
    int index = fromFilename.lastIndexOf(".");
    String ext = "";
    if (index >= 0) {
      ext = fromFilename.substring(index);
    }
    
    String parentDirName = getStoreRelativeDirectory();
    File parentDir = new File(storeTopDirName, parentDirName);
    if (!parentDir.exists()) {
      parentDir.mkdir();
    }
    if (!parentDir.isDirectory()) {
      throw new IOException(String.format("%s is not directory", parentDir));
    }
    
    String saveFilename = UUID.randomUUID().toString() + ext;
    File writeFile = new File(parentDir, saveFilename);
    Files.copy(fromFile, writeFile);
    
    ShareFileEntity entity = new ShareFileEntity();
    entity.filePath = saveFilename;
    entity.originalFilename = fromFilename;
    entity.save();
    
    return new StoredFile(
        storeTopDirName,
        String.format("%s/%s", parentDirName, saveFilename));
  }
  
  public StoredFile getStoredFile(final String relativePath) {
    return new StoredFile(storeTopDirName, relativePath);
  }
  
  public String getStoreRelativeDirectory() {
    Calendar cal = Calendar.getInstance();
    return getStoreRelativeDirectory(cal);
  }
  
  public String getStoreRelativeDirectory(Calendar cal) {
    SimpleDateFormat format = new SimpleDateFormat(DirectoryFormat, Locale.US);
    return format.format(cal.getTime());
  }
  
  public void deleteFiles() {
    Logger.info(String.format("Delete shared files start [%s]", new Date().toString()));
    
    File topDir = new File(storeTopDirName);
    if (!topDir.exists() || !topDir.isDirectory()) {
      Logger.debug(String.format("Directory [%s] not found" , storeTopDirName));
      return;
    }
    
    File[] list = topDir.listFiles((file, name) -> file.isDirectory());
    if (list == null || list.length < 1) {
      Logger.debug(
          String.format("could not find any directory in %s", storeTopDirName));
      return;
    }
    int sum = 
    Arrays.stream(list)
          .parallel()
          .filter(f -> isDeleteTarget(f))
          .mapToInt(f -> deleteFile(f))
          .sum();
    Logger.info(String.format(
        "Delete shared files finished [%s] %d files deleted", new Date().toString(), sum));
  }
  
  private int deleteFile(File file) {
    if (file.isDirectory()) {
      int num = 0;
      File[] list = file.listFiles();
      if (list != null && list.length > 0) {
        num = Arrays.stream(list)
                    .parallel()
                    .mapToInt(f -> deleteFile(f))
                    .sum();
      }
      file.delete();
      return num;
    } else {
      file.delete();
      ShareFileEntity entity = ShareFileEntity.find.byId(file.getName());
      if (entity != null) {
        entity.delete();
      }
      return 1;
    }
  }
  
  private boolean isDeleteTarget(File dir) {
    if (!dir.isDirectory()) {
      return false;
    }

    Calendar cal = Calendar.getInstance();
    cal.add(Calendar.DATE, -7);
    String oldestKeepDirectoryName = getStoreRelativeDirectory(cal);
    if (dir.getName().compareTo(oldestKeepDirectoryName) < 0) {
      return true;
    }
    return false;
  }
  
  public static class StoredFile {
    private String relativePath;
    private File storedFile;
    
    public StoredFile(final String topDir, final String path) {
      this.relativePath = path;
      this.storedFile = new File(topDir, this.relativePath);
    }
    
    public String getRelativePath() {
      return relativePath;
    }
    
    public String getAbsolutePath() {
      return storedFile.getAbsolutePath();
    }
    
    public boolean exists() {
      return storedFile.exists();
    }
    
    public String getOriginalFilename() {
      String path = relativePath.substring(relativePath.lastIndexOf("/") + 1);
      ShareFileEntity entity = ShareFileEntity.find.byId(path);
      if (entity == null) {
        Logger.debug(String.format("Not found in database %s", relativePath));
      }
      return (entity != null) ? entity.originalFilename : relativePath;
    }
  }
}
