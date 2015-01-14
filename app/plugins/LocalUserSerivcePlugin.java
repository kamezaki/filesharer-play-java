package plugins;

import models.User;
import play.Application;

import com.feth.play.module.pa.service.UserServicePlugin;
import com.feth.play.module.pa.user.AuthUser;
import com.feth.play.module.pa.user.AuthUserIdentity;

public class LocalUserSerivcePlugin extends UserServicePlugin {

  public LocalUserSerivcePlugin(final Application app) {
    super(app);
  }

  @Override
  public Object getLocalIdentity(final AuthUserIdentity identity) {
    final User u = User.findByAuthUserIdentity(identity);
    if (u != null) {
      return u.id;
    } else {
      return null;
    }
  }

  @Override
  public AuthUser link(final AuthUser oldUser, final AuthUser newUser) {
    User.addLinkedAccount(oldUser, newUser);
    return null;
  }

  @Override
  public AuthUser merge(final AuthUser newUser, final AuthUser oldUser) {
    if (!oldUser.equals(newUser)) {
      User.merge(oldUser, newUser);
    }
    return oldUser;
  }

  @Override
  public Object save(AuthUser user) {
    final boolean isLinked = User.existsByAuthUserIdentity(user);
    if (isLinked) {
      // we have this user already
      return null;
    }
    return User.create(user).id;
  }
}
