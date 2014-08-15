package biz.info_cloud.filesharer;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class LocalConfig {
  private static Config config = ConfigFactory.load();
  
  /**
   * Get file storage path.
   * 
   * @return path
   * @throws ConfigException.Missing
   * @throws ConfigException.WrongType
   */
  public static String getStorePath() {
    return config.getString("filesharer.store.path");
  }

  /**
   * Get Schedule frequency time in hours
   * @return frequency time.
   */
  public static int getScheduleFrequencyInHours() {
    return getIntValue("filesharer.scheduler.interval", n -> n > 0, 22);
  }
  
  /**
   * Get keep duration in days
   * @return days
   */
  public static int getKeepDurationInDays() {
    int value = getIntValue("filesharer.keep.duration", n -> n > 0, 7);
    return value;
  }
  
  private static int getIntValue(String key, Condition<Integer> condition, int defaultValue) {
    int value = defaultValue;
    if (config.hasPath(key)) {
      value = config.getInt(key);
    }
    if (!condition.isAccpeptable(value)) {
      value = defaultValue;
    }
    return value;
  }
  
  private LocalConfig() {
  }
  
  @FunctionalInterface
  private interface Condition<T> {
    public boolean isAccpeptable(T n);
  }
  
}
