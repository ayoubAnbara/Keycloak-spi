package com.ayoubanbara;


import com.ayoubanbara.beans.PasswordEncoderSingleton;
import com.ayoubanbara.dao.UserDAO;
import com.ayoubanbara.model.User;
import com.ayoubanbara.representations.UserRepresentation;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputUpdater;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.cache.CachedUserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.ReadOnlyException;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;
import org.keycloak.storage.user.UserRegistrationProvider;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class CustomUserStorageProvider implements UserStorageProvider,
        UserLookupProvider, UserQueryProvider, CredentialInputUpdater, CredentialInputValidator,
        UserRegistrationProvider {
    private final KeycloakSession session;
    private final ComponentModel model;
    private final UserDAO userDAO;

    public CustomUserStorageProvider(KeycloakSession session, ComponentModel model, UserDAO userDAO) {
        this.session = session;
        this.model = model;
        this.userDAO = userDAO;
    }

    @Override
    public void close() {
        userDAO.close();
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        log.info("isConfiguredFor(" + realm + ", " + user + ", " + credentialType + ")");
        return supportsCredentialType(credentialType) && getPassword(user) != null;
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput credentialInput) {
        log.info("isValid(" + realm + ", " + user + ", " + credentialInput + ")");

        if (!(credentialInput instanceof UserCredentialModel)) return false;
        if (supportsCredentialType(credentialInput.getType())) {
            final String password = getPassword(user);
            final PasswordEncoder passwordEncoder= PasswordEncoderSingleton.getInstance();
            return password != null && passwordEncoder.matches(credentialInput.getChallengeResponse(),password);
        } else {
            return false; // invalid cred type
        }
    }


    @Override
    public boolean supportsCredentialType(String credentialType) {
        boolean result = PasswordCredentialModel.TYPE.equals(credentialType);
        log.info("supportsCredentialType({}) result: {}", credentialType,result);
        return result;
    }


    @Override
    public boolean updateCredential(RealmModel realm, UserModel userModel, CredentialInput input) {
        // todo remove this
        log.info("updateCredential( {}, {}, {})",realm,userModel,input);
        //throw new ReadOnlyException("user is read only for this update");
        if (!supportsCredentialType(input.getType()) || !(input instanceof UserCredentialModel)) return false;
        String id = StorageId.externalId(userModel.getId());
        User user = userDAO.getUserById(id);

        // user.setUsername(userModel.getUsername());
        PasswordEncoder passwordEncoder=PasswordEncoderSingleton.getInstance();
        user.setPassword(passwordEncoder.encode(input.getChallengeResponse()));
        userDAO.updateUser(user);
        return true;
    }
    @Override
    public void disableCredentialType(RealmModel realm, UserModel user, String credentialType) {
        log.info("disableCredentialType({}, {}, {})",realm,user,credentialType);
        // throw new ReadOnlyException("user is read only for this update");
        if (!supportsCredentialType(credentialType)) return;
        getUserRepresentation(user).setPassword(null);
    }

    @Override
    public Set<String> getDisableableCredentialTypes(RealmModel realm, UserModel user) {
        if (getUserRepresentation(user).getPassword() != null) {
            Set<String> set = new HashSet<>();
            set.add(PasswordCredentialModel.TYPE);
            return set;
        } else {
            return Collections.emptySet();
        }
    }

    public UserRepresentation getUserRepresentation(UserModel user) {
        UserRepresentation userRepresentation ;
        if (user instanceof CachedUserModel) {
            userRepresentation = (UserRepresentation) ((CachedUserModel) user).getDelegateForUpdate();
        } else {
            userRepresentation = (UserRepresentation) user;
        }
        return userRepresentation;
    }

    public UserRepresentation getUserRepresentation(User user, RealmModel realm) {
        return new UserRepresentation(session, realm, model, user, userDAO);
    }

    @Override
    public int getUsersCount(RealmModel realm) {
        log.info("getUsersCount( {} )",realm);
        return userDAO.size();
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm) {
        log.info("getUsers( {} )",realm);
        return userDAO.findAll()
                .stream()
                .map(user -> new UserRepresentation(session, realm, model, user, userDAO))
                .collect(Collectors.toList());
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm, int firstResult, int maxResults) {
        log.info("getUsers(RealmModel realm, int firstResult, int maxResults)");
        return userDAO.findAll(firstResult, maxResults)
                .stream()
                .map(user -> new UserRepresentation(session, realm, model, user, userDAO))
                .collect(Collectors.toList());
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm) {
        log.info("searchForUser(String search, RealmModel realm)");
        return userDAO.searchForUserByUsernameOrEmail(search)
                .stream()
                .map(user -> new UserRepresentation(session, realm, model, user, userDAO))
                .collect(Collectors.toList());
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm, int firstResult, int maxResults) {
        log.info("searchForUser(String search, RealmModel realm, int firstResult, int maxResults)");
        return userDAO.searchForUserByUsernameOrEmail(search, firstResult, maxResults)
                .stream()
                .map(user -> new UserRepresentation(session, realm, model, user, userDAO))
                .collect(Collectors.toList());
    }

    @Override
    public List<UserModel> searchForUser(Map<String, String> params, RealmModel realm) {
        log.info("searchForUser(params: {} , realm: {})" ,params, realm  );
        // TODO Will probably never implement; Only used by REST API
        return new ArrayList<>();
    }

    @Override
    public List<UserModel> searchForUser(Map<String, String> params, RealmModel realm, int firstResult,
                                         int maxResults) {
        return userDAO.findAll(firstResult, maxResults)
                .stream()
                .map(user -> new UserRepresentation(session, realm, model, user, userDAO))
                .collect(Collectors.toList());
    }

    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group, int firstResult, int maxResults) {
        // TODO Will probably never implement
        return new ArrayList<>();
    }

    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group) {
        // TODO Will probably never implement
        return new ArrayList<>();
    }

    @Override
    public List<UserModel> searchForUserByUserAttribute(String attrName, String attrValue, RealmModel realm) {
        // TODO Will probably never implement
        return new ArrayList<>();
    }

    @Override
    public UserModel getUserById(String keycloakId, RealmModel realm) {
        // keycloakId := keycloak internal id; needs to be mapped to external id
        log.info("method invoked: getUserById() with keycloakId={}",keycloakId);
        String id = StorageId.externalId(keycloakId);
        return new UserRepresentation(session, realm, model, userDAO.getUserById(id), userDAO);
    }

    @Override
    public UserModel getUserByUsername(String username, RealmModel realm) {
        log.info("getUserByUsername(String username, RealmModel realm)");
        Optional<User> optionalUser = userDAO.getUserByEmail(username);
        return optionalUser.map(user -> getUserRepresentation(user, realm)).orElse(null);
    }

    @Override
    public UserModel getUserByEmail(String email, RealmModel realm) {
        log.info("getUserByEmail(String email, RealmModel realm)");
        Optional<User> optionalUser = userDAO.getUserByEmail(email);
        return optionalUser.map(user -> getUserRepresentation(user, realm)).orElse(null);
    }

    @Override
    public UserModel addUser(RealmModel realm, String username) {
        throw new ReadOnlyException("user is read only");

      /*  log.info("addUser");
        User user = new User();
        user.setUsername(username);
        user = userDAO.createUser(user);

        return new UserRepresentation(session, realm, model, user, userDAO);*/

    }

    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        log.info("removeUser(" + realm + ", " + user + ")");
        throw new ReadOnlyException("user is read only");
        /*User userEntity = userDAO.getUserById(StorageId.externalId(user.getId()));
        if (userEntity == null) {
            log.info("Tried to delete invalid user with ID " + user.getId());
            return false;
        }
        userDAO.deleteUser(userEntity);*/
    }

    public String getPassword(UserModel user) {
        String password = null;
        if (user instanceof UserRepresentation) {
            password = ((UserRepresentation) user).getPassword();
        }
        return password;
    }
}
