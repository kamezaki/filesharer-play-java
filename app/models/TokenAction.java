package models;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.QueryIterator;
import com.avaje.ebean.annotation.EnumValue;

import play.data.format.Formats;
import play.db.ebean.Model;

@Entity
public class TokenAction extends Model {
  public enum Type {
    @EnumValue("EV")
    EMAIL_VERIFICATION,
    @EnumValue("PR")
    PASSWORD_RESET
  }
  
  private static final long serialVersionUID = 1L;
  
  private final static long VERIFICATION_TIME_IN_SEC = 7 * 24 * 3600;  // 1 week
  
  @Id
  public Long id;
  
  @Column(unique = true)
  public String token;
  
  @ManyToOne
  public User targetUser;
  
  public Type type;
  
  @Formats.DateTime(pattern = "yyyy-MM-dd HH:mm:ss")
  public Date created;
  
  @Formats.DateTime(pattern = "yyyy-MM-dd HH:mm:ss")
  public Date expires;
  
  public static final Finder<Long, TokenAction> find =
      new Finder<>(Long.class, TokenAction.class);
  
  public static TokenAction findByToken(final String token, final Type type) {
    return find.where().eq("token", token)
                       .eq("type", type)
                       .findUnique();
  }
  
  public static void deleteByUser(final User user, final Type type) {
    QueryIterator<TokenAction> iterator =
        find.where()
            .eq("targetUser.id", user.id)
            .eq("type", type)
            .findIterate();
    Ebean.delete(iterator);
    iterator.close();
  }
  
  public boolean isValid() {
    return this.expires.after(new Date());
  }
  
  public static TokenAction create(
      final Type type, final String token, final User targetUser) {
    final TokenAction ua = new TokenAction();
    ua.targetUser = targetUser;
    ua.token = token;
    ua.type = type;
    final Date created = new Date();
    ua.created = created;
    ua.expires = new Date(created.getTime() + VERIFICATION_TIME_IN_SEC * 1000);
    ua.save();
    
    return ua;
  }
}
