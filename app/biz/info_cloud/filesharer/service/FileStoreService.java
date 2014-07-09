package biz.info_cloud.filesharer.service;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;

import com.google.common.io.Files;

public class FileStoreService {
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
    
    return new StoredFile(
        storeTopDirName,
        String.format("%s/%s", parentDirName, saveFilename));
  }
  
  public StoredFile getStoredFile(final String relativePath) {
    return new StoredFile(storeTopDirName, relativePath);
  }
  
  public String getStoreRelativeDirectory() {
    Calendar cal = Calendar.getInstance();
    SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd", Locale.US);
    return format.format(cal.getTime());
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
  }
}
