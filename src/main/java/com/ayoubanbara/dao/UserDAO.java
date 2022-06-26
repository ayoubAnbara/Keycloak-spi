package com.ayoubanbara.dao;

import com.ayoubanbara.model.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@Slf4j
public class UserDAO {

    private final EntityManager entityManager;

    public UserDAO(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public List<User> findAll() {
        return findAll(null, null);
    }

    public List<User> findAll(int start, int max) {
        return findAll((Integer)start, (Integer)max);
    }

    private List<User> findAll(Integer start, Integer max) {
        TypedQuery<User> query = entityManager.createNamedQuery("searchForUser", User.class);
        if(start != null) {
            query.setFirstResult(start);
        }
        if(max != null) {
            query.setMaxResults(max);
        }
        query.setParameter("search", "%");
        List<User> users =  query.getResultList();
        return users;
    }

    /*public Optional<User> getUserByUsername(String username) {
        log.info("getUserByUsername(username: " + username + ")");
        TypedQuery<User> query = entityManager.createNamedQuery("getUserByUsername", User.class);
        query.setParameter("username", username);
        return query.getResultList().stream().findFirst();
    }*/

    public Optional<User> getUserByEmail(String email) {
        log.info("getUserByEmail(email: {} )",email);
        TypedQuery<User> query = entityManager.createNamedQuery("getUserByEmail", User.class);
        query.setParameter("email", email);
        return query.getResultList().stream().findFirst();
    }

    public List<User> searchForUserByUsernameOrEmail(String searchString) {
        log.info("searchForUserByUsernameOrEmail(searchString: {})",searchString);
        return searchForUserByUsernameOrEmail(searchString, null, null);
    }

    public List<User> searchForUserByUsernameOrEmail(String searchString, int start, int max) {
        log.info("searchForUserByUsernameOrEmail(searchString: {}, start: {}, max: {}",searchString,start,max);
        return searchForUserByUsernameOrEmail(searchString, (Integer)start, (Integer)max);
    }

    private List<User> searchForUserByUsernameOrEmail(String searchString, Integer start, Integer max) {
        log.info("searchForUserByUsernameOrEmail(searchString: {}, start: {}, max: {}",searchString,start,max);
        TypedQuery<User> query = entityManager.createNamedQuery("searchForUser", User.class);
        query.setParameter("search", "%" + searchString + "%");
        if(start != null) {
            query.setFirstResult(start);
        }
        if(max != null) {
            query.setMaxResults(max);
        }
        return query.getResultList();
    }

    public User getUserById(String id) {
        log.info("getUserById(id: {} )",id);
//        return entityManager.find(User.class, UUID.fromString(id));
        return entityManager.find(User.class, Integer.parseInt(id));
    }

    public User createUser(User user) {
        EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        entityManager.persist(user);
        transaction.commit();
        return user;
    }


    public void close() {
        this.entityManager.close();
    }

    public User updateUser(User userEntity) {
        EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        entityManager.merge(userEntity);
        transaction.commit();
        return userEntity;
    }

    public int size() {
        return entityManager.createNamedQuery("getUserCount", Integer.class).getSingleResult();
    }
}
