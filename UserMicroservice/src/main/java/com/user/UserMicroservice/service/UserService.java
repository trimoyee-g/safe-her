package com.user.UserMicroservice.service;

import com.user.UserMicroservice.entity.User;

import java.util.List;

public interface UserService {

    // create
    User saveUser(User user);

    // get all users
    List<User> getAllUser();

    // get particular user
    User getUser(String userId);

    // get list of users with similar names
    List<User> getUsersByName(String name);

    // update user
    User updateUser(String userId, User user);

    // delete user
    void deleteUser(String userId);
}
