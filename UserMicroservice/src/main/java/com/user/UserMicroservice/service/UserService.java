package com.user.UserMicroservice.service;

import com.user.UserMicroservice.entity.User;

import java.util.List;

public interface UserService {

    User saveUser(User user);

    List<User> getAllUser();

    User getUser(String userId);

    User updateUser(String userId, User user);

    void deleteUser(String userId);
}
