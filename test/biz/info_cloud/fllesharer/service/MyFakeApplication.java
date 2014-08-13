package biz.info_cloud.fllesharer.service;
import java.util.HashMap;
import java.util.Map;

import org.junit.rules.ExternalResource;

import play.test.FakeApplication;
import play.test.Helpers;

public class MyFakeApplication extends ExternalResource {
  private FakeApplication fakeApplication;
  
  @Override
  protected void before() throws Throwable {
    super.before();
    Map<String, String> settings = new HashMap<>();
    settings.put("db.default.driver", "org.h2.Driver");
    settings.put("db.default.url", "jdbc:h2:mem:play-test");
    
    fakeApplication = Helpers.fakeApplication(settings);
    Helpers.start(fakeApplication);
  }
  
  @Override
  protected void after() {
    super.after();
    Helpers.stop(fakeApplication);
  }

}
