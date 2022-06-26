package com.ayoubanbara.representations;

import com.ayoubanbara.beans.PasswordEncoderSingleton;
import com.ayoubanbara.dao.UserDAO;
import com.ayoubanbara.model.User;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.storage.ReadOnlyException;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;

public class UserRepresentation extends AbstractUserAdapterFederatedStorage {
    private  User userEntity;
    private final UserDAO userDAO;

    public UserRepresentation(KeycloakSession session,
                              RealmModel realm,
                              ComponentModel storageProviderModel,
                              User userEntity,
                              UserDAO userDAO) {
        super(session, realm, storageProviderModel);
        this.userEntity = userEntity;
        this.userDAO = userDAO;
    }

    @Override
    public String getUsername() {
        return userEntity.getEmail();
    }

    @Override
    public void setUsername(String username) {
        throw new ReadOnlyException("user is read only");
//        userEntity.setEmail(username);
//        userEntity = userDAO.updateUser(userEntity);
    }

    @Override
    public void setEmail(String email) {
        throw new ReadOnlyException("user is read only");
//        userEntity.setEmail(email);
//        userEntity = userDAO.updateUser(userEntity);
    }

    @Override
    public String getEmail() {
        return userEntity.getEmail();
    }

    @Override
    public void setSingleAttribute(String name, String value) {
//        if (name.equals("phone")) {
//            userEntity.setPhone(value);
//        } else {
        super.setSingleAttribute(name, value);
        //     }
    }

    @Override
    public void removeAttribute(String name) {
        throw new ReadOnlyException("user is read only");
      /* if (name.equals("phone")) {
            userEntity.setPhone(null);
        } else {
            super.removeAttribute(name);
        }
        userEntity = userDAO.updateUser(userEntity);*/
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        throw new ReadOnlyException("user is read only");
//        if (name.equals("phone")) {
//            userEntity.setPhone(values.get(0));
//        } else {
        //        super.setAttribute(name, values);
        //   }
        //     userEntity = userDAO.updateUser(userEntity);
    }

    @Override
    public String getFirstAttribute(String name) {
//        if (name.equals("phone")) {
//            return userEntity.getPhone();
//        } else {
        return super.getFirstAttribute(name);
        //     }
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        Map<String, List<String>> attrs = super.getAttributes();
        MultivaluedHashMap<String, String> all = new MultivaluedHashMap<>();
        all.putAll(attrs);
        //  all.add("phone", userEntity.getPhone());
        return all;
    }

    @Override
    public List<String> getAttribute(String name) {
//        if (name.equals("phone")) {
//            List<String> phone = new LinkedList<>();
//            phone.add(userEntity.getPhone());
//            return phone;
//        } else {
        return super.getAttribute(name);
        //      }
    }

    @Override
    public String getId() {
        return StorageId.keycloakId(storageProviderModel, userEntity.getId().toString());
    }

    public String getPassword() {
        return userEntity.getPassword();
    }

    public void setPassword(String password) {
        PasswordEncoder passwordEncoder = PasswordEncoderSingleton.getInstance();
        userEntity.setPassword(passwordEncoder.encode(password));
        userEntity = userDAO.updateUser(userEntity);
    }

    @Override
    public boolean isEnabled() {
        return userEntity.isEnabled() && !userEntity.isBlocked();
    }

    @Override
    public void setEnabled(boolean enabled) {
        throw new ReadOnlyException("user is read only");
//        userEntity.setEnabled(enabled);
//        userEntity = userDAO.updateUser(userEntity);
    }
}
