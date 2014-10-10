package biz.info_cloud.web.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.mozilla.universalchardet.UniversalDetector;

import play.Logger;

public class ContentsUtils {
  private ContentsUtils() {
  }
  
  /**
   * detect character encoding of file
   * 
   * @param path
   * @return encoding
   * @throws IOException 
   */
  public static String detectFileEncoding(String path) throws IOException {
    String encoding = null;
    UniversalDetector detector = null;
    try (FileInputStream fis = new java.io.FileInputStream(path)) {
      encoding = detectEncoding(fis);
    } catch (FileNotFoundException e) {
      Logger.error("error on detect encoding", e);
      throw e;
    } finally {
      if (detector != null) {
        detector.reset();
      }
    }
    return encoding;
  }
  
  public static String detectEncoding(InputStream stream) throws IOException {
    String encoding = null;
    UniversalDetector detector = null;
    try {
      byte[] buf = new byte[4096];
      detector = new UniversalDetector(null);
      int nread;
      while ((nread = stream.read(buf)) > 0 && !detector.isDone()) {
        detector.handleData(buf, 0, nread);
      }
      detector.dataEnd();

      encoding = detector.getDetectedCharset();
    } catch (IOException e) {
      Logger.error("error on detect encoding", e);
      throw e;
    } finally {
      if (detector != null) {
        detector.reset();
      }
    }
    return encoding;
  }
}
