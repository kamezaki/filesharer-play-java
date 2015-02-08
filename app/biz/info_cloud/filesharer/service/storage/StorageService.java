package biz.info_cloud.filesharer.service.storage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;

public interface StorageService {
  public InputStream getInputStream(String storedName) throws IOException;
  public String save(File original, String filename) throws IOException;
  public void delete(String storedName) throws IOException;
  public void cleanup(LocalDateTime deleteDate);
}
