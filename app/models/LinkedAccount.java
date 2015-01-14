package models;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import play.db.ebean.Model;

import com.feth.play.module.pa.user.AuthUser;

@Entity
public class LinkedAccount extends Model {
  private static final long serialVersionUID = 1L;

  @Id
  public Long id;

  @ManyToOne
  public User user;

  public String providerUserId;
  public String providerKey;

  public static final Finder<Long, LinkedAccount> find =
      new Finder<>(Long.class, LinkedAccount.class);

  public static LinkedAccount findByProviderKey(final User user, final String key) {
    return find.where()
               .eq("user", user)
               .eq("providerKey", key)
               .findUnique();
  }

  public static LinkedAccount create(final AuthUser authUser) {
    final LinkedAccount account = new LinkedAccount();
    account.update(authUser);
    return account;
  }
  
  public void update(final AuthUser authUser) {
    this.providerKey = authUser.getProvider();
    this.providerUserId = authUser.getId();
  }

  public static LinkedAccount create(final LinkedAccount acc) {
    final LinkedAccount account = new LinkedAccount();
    account.providerKey = acc.providerKey;
    account.providerUserId = acc.providerUserId;

    return account;
  }
}
