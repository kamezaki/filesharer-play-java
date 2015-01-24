package biz.info_cloud.filesharer.providers;

import com.feth.play.module.pa.providers.password.UsernamePasswordAuthUser;
import com.feth.play.module.pa.user.NameIdentity;

public class MyUsernamePasswordAuthUser
    extends UsernamePasswordAuthUser implements NameIdentity {
  private static final long serialVersionUID = 1L;

  private final String name;
  
  public MyUsernamePasswordAuthUser(
      final MyUsernamePasswordAuthProvider.MySignup signup) {
    super(signup.password, signup.email);
    this.name = signup.name;
  }

  @Override
  public String getName() {
    return name;
  }

}
