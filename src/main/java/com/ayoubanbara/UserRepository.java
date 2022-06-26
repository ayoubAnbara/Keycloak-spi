package com.ayoubanbara;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;


class UserRepository {

    private final List<User> users;

    UserRepository() {
        Long created = System.currentTimeMillis();
        List<String> roles = Collections.singletonList("stoneage");
        users = Arrays.asList(
                new User("1", "Fred", "Flintstone", true, created, roles),
                new User("2", "Wilma", "Flintstone", true, created, roles),
                new User("3", "Pebbles", "Flintstone", true, created, roles),
                new User("4", "Barney", "Rubble", true, created, roles),
                new User("5", "Betty", "Rubble", true, created, Collections.emptyList()),
                new User("6", "Bam Bam", "Rubble", false, created, Collections.emptyList())
        );
    }

    List<User> getAllUsers() {
        return users;
    }

    int getUsersCount() {
        return users.size();
    }

    User findUserById(String id) {
        return users.stream().filter(user -> user.getId().equals(id)).findFirst().orElse(null);
    }

    User findUserByUsernameOrEmail(String username) {
        return users.stream()
                .filter(user -> user.getUsername().equalsIgnoreCase(username) || user.getEmail().equalsIgnoreCase(username))
                .findFirst().orElse(null);
    }

    List<User> findUsers(String query) {
        return users.stream()
                .filter(user -> user.getUsername().contains(query) || user.getEmail().contains(query))
                .collect(Collectors.toList());
    }

    boolean validateCredentials(String username, String password) {
        return findUserByUsernameOrEmail(username).getPassword().equals(password);
    }

    boolean updateCredentials(String username, String password) {
        findUserByUsernameOrEmail(username).setPassword(password);
        return true;
    }



}
