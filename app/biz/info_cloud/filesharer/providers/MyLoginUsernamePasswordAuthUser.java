package biz.info_cloud.filesharer.providers;

import com.feth.play.module.pa.providers.password.DefaultUsernamePasswordAuthUser;

public class MyLoginUsernamePasswordAuthUser
    extends DefaultUsernamePasswordAuthUser {
  private static final long serialVersionUID = 1L;

  static final long SESSION_TIMEOUT_IN_SEC = 24 * 14 * 3600;  // 2 week
  private long expirationInMillisec;
  
  public MyLoginUsernamePasswordAuthUser(
      final String clearPassword, final String email) {
    super(clearPassword, email);
    expirationInMillisec =
        System.currentTimeMillis() + SESSION_TIMEOUT_IN_SEC * 1000;
  }
  
  public MyLoginUsernamePasswordAuthUser(final String email) {
    this(null, email);
  }
  
  public MyLoginUsernamePasswordAuthUser(
      final MyUsernamePasswordAuthProvider.MyLogin login) {
    this(login.password, login.email);
  }

  @Override
  public long expires() {
    return expirationInMillisec;
  }

}
