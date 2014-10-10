package biz.info_cloud.filesharer.service.storage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public interface StorageService {
  public InputStream getInputStream(String storedName) throws IOException;
  public String save(File original, String filename) throws IOException;
  public void delete(String storedName) throws IOException;
}
