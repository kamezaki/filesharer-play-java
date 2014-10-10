package biz.info_cloud.fllesharer.service;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.rules.ExternalResource;

import play.test.FakeApplication;
import play.test.Helpers;

import com.google.common.io.Files;

public class MyFakeApplication extends ExternalResource {
  private FakeApplication fakeApplication;
  private String topDir;
  
  @Override
  protected void before() throws Throwable {
    super.before();
    Map<String, String> settings = new HashMap<>();
    topDir = Files.createTempDir().getAbsolutePath();
    settings.put("db.default.driver", "org.h2.Driver");
    settings.put("db.default.url", "jdbc:h2:mem:play-test");
    settings.put("filesharer.store.type", "file");
    settings.put("filesharer.store.path", topDir);
    
    fakeApplication = Helpers.fakeApplication(settings);
    Helpers.start(fakeApplication);
  }
  
  @Override
  protected void after() {
    super.after();
    removeFiles(new File(topDir));
    Helpers.stop(fakeApplication);
  }
  
  private void removeFiles(File file) {
    if (!file.exists()) {
      return;
    }
    
    if (file.isDirectory()) {
      List<File> list = Arrays.asList(file.listFiles());
      list.forEach(f -> removeFiles(f));
      file.delete();
    } else {
      file.delete();
    }
  }
  
}
