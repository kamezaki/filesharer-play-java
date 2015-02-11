package models;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import models.TokenAction.Type;
import play.data.format.Formats;
import play.data.validation.Constraints;
import play.db.ebean.Model;
import be.objectify.deadbolt.core.models.Permission;
import be.objectify.deadbolt.core.models.Role;
import be.objectify.deadbolt.core.models.Subject;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.ExpressionList;
import com.feth.play.module.pa.providers.password.UsernamePasswordAuthUser;
import com.feth.play.module.pa.user.AuthUser;
import com.feth.play.module.pa.user.AuthUserIdentity;
import com.feth.play.module.pa.user.EmailIdentity;
import com.feth.play.module.pa.user.NameIdentity;

import controllers.UserRole;

@Entity
@Table(name = "users")
public class User extends Model implements Subject {
  private static final long serialVersionUID = 3L;

  @Id
  public Long id;

  @Constraints.Email
  // if you make this unique, keep in mind that users *must* merge/link their
  // accounts then on signup with additional providers
  // @Column(unique = true)
  public String email;

  public String name;

  @Formats.DateTime(pattern = "yyyy-MM-dd HH:mm:ss")
  public Date lastLogin;
  
  public boolean active;

  public boolean emailValidated;

  @OneToMany(cascade = CascadeType.ALL)
  public List<LinkedAccount> linkedAccounts;
  
  @ManyToMany
  public List<SecurityRole> roles;

  public static final Finder<Long, User> find =
      new Finder<Long, User>(Long.class, User.class);

  public static boolean existsByAuthUserIdentity(final AuthUserIdentity identity) {
    final ExpressionList<User> exp;
    if (identity instanceof UsernamePasswordAuthUser) {
      exp = getUsernamePasswordAuthUserFind((UsernamePasswordAuthUser) identity);
    } else {
      exp = getAuthUserFind(identity);
    }
    return exp.findRowCount() > 0;
  }
  

  private static ExpressionList<User> getAuthUserFind(final AuthUserIdentity identity) {
    return find.where()
               .eq("active", true)
               .eq("linkedAccounts.providerUserId", identity.getId())
               .eq("linkedAccounts.providerKey", identity.getProvider());
  }
  
  public static User findByAuthUserIdentity(final AuthUserIdentity identity) {
    if (identity == null) {
      return null;
    }
    if (identity instanceof UsernamePasswordAuthUser) {
      return findByUsernamePasswordIdentity((UsernamePasswordAuthUser) identity);
    }
    return getAuthUserFind(identity).findUnique();
  }

  public void merge(final User otherUser) {
//  otherUser.linkedAccounts.forEach(acc -> LinkedAccount.create(acc));
    for (final LinkedAccount acc : otherUser.linkedAccounts) {
      this.linkedAccounts.add(LinkedAccount.create(acc));
    }
    // do all other merging stuff here - like resources, etc.

    // deactivate the merged user that got added to this one
    otherUser.active = false;
    Ebean.save(Arrays.asList(new User[] { otherUser, this }));
  }

  public static User create(final AuthUser authUser) {
    final User user = new User();
    user.roles = Collections.singletonList(
        SecurityRole.findByRoleName(UserRole.USER));
    user.active = true;
    user.linkedAccounts = Collections.singletonList(
        LinkedAccount.create(authUser));

    if (authUser instanceof EmailIdentity) {
      final EmailIdentity identity = (EmailIdentity) authUser;
      // Remember, even when getting them from FB & Co., emails should be
      // verified within the application as a security breach there might
      // break your security as well!
      user.email = identity.getEmail();
      user.emailValidated = false;
    }

    if (authUser instanceof NameIdentity) {
      final NameIdentity identity = (NameIdentity) authUser;
      final String name = identity.getName();
      if (name != null) {
        user.name = name;
      }
    }

    user.save();
    user.saveManyToManyAssociations("roles");
    return user;
  }

  public static void merge(final AuthUser oldUser, final AuthUser newUser) {
    User.findByAuthUserIdentity(oldUser)
        .merge(User.findByAuthUserIdentity(newUser));
  }

  public Set<String> getProviders() {
    final Set<String> providerKeys = new HashSet<String>(linkedAccounts.size());
    for (final LinkedAccount acc : linkedAccounts) {
      providerKeys.add(acc.providerKey);
    }
    return providerKeys;
  }

  public static void addLinkedAccount(final AuthUser oldUser, final AuthUser newUser) {
    final User user = User.findByAuthUserIdentity(oldUser);
    user.linkedAccounts.add(LinkedAccount.create(newUser));
    user.save();
  }
  
  public static User findByEmail(final String email) {
    return getEmailUserFind(email).findUnique();
  }

  public static void setLastLoginDate(final AuthUser knownUser) {
    final User user = findByAuthUserIdentity(knownUser);
    user.lastLogin = new Date();
    user.save();
  }
  
  private static ExpressionList<User> getEmailUserFind(final String email) {
    return find.where()
               .eq("active", true)
               .eq("email", email);
  }
  
  public static User findByUsernamePasswordIdentity(final UsernamePasswordAuthUser identity) {
    return getUsernamePasswordAuthUserFind(identity).findUnique();
  }
  
  private static ExpressionList<User> getUsernamePasswordAuthUserFind(
      final UsernamePasswordAuthUser identity) {
    return getEmailUserFind(identity.getEmail())
        .eq("linkedAccounts.providerKey", identity.getProvider());
  }

  public LinkedAccount getAccountByProvider(final String providerKey) {
    return LinkedAccount.findByProviderKey(this, providerKey);
  }
  
  public static void verify(final User unverified) {
    unverified.emailValidated = true;
    unverified.save();
    TokenAction.deleteByUser(unverified, Type.EMAIL_VERIFICATION);
  }
  
  public void changePassword(
      final UsernamePasswordAuthUser authUser, final boolean create) {
    LinkedAccount link = getAccountByProvider(authUser.getProvider());
    if (link == null) {
      if (create) {
        link = LinkedAccount.create(authUser);
        link.user = this;
      } else {
        throw new RuntimeException("Account not enabled for password usage.");
      }
      link.providerUserId = authUser.getHashedPassword();
      link.save();
    }
  }
  
  public void resetPassword(
      final UsernamePasswordAuthUser authUser, final boolean create) {
    changePassword(authUser, create);
    TokenAction.deleteByUser(this, Type.PASSWORD_RESET);
  }

  @Override
  public void delete() {
    AccessLog.deleteByUser(this);
    super.delete();
  }

  @Override
  public String getIdentifier() {
    return Long.toString(id);
  }


  @Override
  public List<? extends Permission> getPermissions() {
    return null;
  }


  @Override
  public List<? extends Role> getRoles() {
    return roles;
  }
}
