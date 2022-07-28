package com.ayoubanbara.model;

import com.ayoubanbara.utils.StringTrimConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.hibernate.annotations.ColumnDefault;

/**
 * @author Ayoub Anbara, https://www.ayoubanbara.com
 */
@NamedQueries({
       // @NamedQuery(name="getUserByUsername", query="select u from User u where u.username = :username"),
        @NamedQuery(name="getUserByEmail", query="select u from User u where u.email = :email"),
        @NamedQuery(name="getUserCount", query="select count(u) from User u"),
        @NamedQuery(name="getAllUsers", query="select u from User u"),
        @NamedQuery(name="searchForUser", query="select u from User u where " +
               // "( lower(u.username) like :search or u.email like :search ) order by u.username"),
                "( lower(u.email) like :search ) order by u.email"),
})
@Entity
@Table(name = "business_users")
@Data
@Accessors(chain = true)
@ToString
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

   /* @Column(unique = true)
    @Convert(converter = StringTrimConverter.class)
    private String username; */// this filead should not be null try to initialize it

    @Column(unique = true,updatable = false)
    @Convert(converter = StringTrimConverter.class)
    private String email;
    @Convert(converter = StringTrimConverter.class)
    private String password;
    @Column(nullable = false, insertable = false)
    @ColumnDefault("0")
    private boolean enabled;
    @Column(nullable = false, insertable = false)
    @ColumnDefault("0")
    private boolean blocked;




}
