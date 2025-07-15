package com.user.UserMicroservice.service.impl;

import com.user.UserMicroservice.entity.Place;
import com.user.UserMicroservice.entity.Rating;
import com.user.UserMicroservice.entity.User;
import com.user.UserMicroservice.exception.ResourceNotFound;
import com.user.UserMicroservice.externalservices.PlaceService;
import com.user.UserMicroservice.externalservices.RatingService;
import com.user.UserMicroservice.repository.UserRepository;
import com.user.UserMicroservice.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlaceService placeService;

    @Autowired
    private RatingService ratingService;

    @Override
    public User saveUser(User user) {
        if (user.getUserId() == null || user.getUserId().isEmpty()) {
            throw new RuntimeException("User ID must be provided");
        }
        return userRepository.save(user);
    }

    @Override
    public List<User> getAllUser() {
        List<User> users = userRepository.findAll();

        for (User user : users) {
            List<Rating> ratings = ratingService.getRatingsByUserId(user.getUserId());

            List<Rating> ratingList = ratings.stream().map(rating -> {
                Place place = placeService.getPlaceByPlaceId(rating.getPlaceId());
                rating.setPlace(place);
                return rating;
            }).collect(Collectors.toList());

            user.setUserRatings(ratingList);
        }

        return users;
    }


    @Override
    public User getUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFound("User with given ID is not found on server"));

        List<Rating> ratings = ratingService.getRatingsByUserId(userId);

        List<Rating> ratingList = ratings.stream().map(rating -> {
            Place place = placeService.getPlaceByPlaceId(rating.getPlaceId());
            rating.setPlace(place);
            return rating;
        }).collect(Collectors.toList());

        user.setUserRatings(ratingList);
        return user;
    }

    @Override
    public List<User> getUsersByName(String name) {
        return userRepository.getUsersByName(name);
    }



    @Override
    public User updateUser(String userId, User user) {
        return userRepository.findById(userId)
                .map(existingUser -> {
                    existingUser.setUserName(user.getUserName());
                    existingUser.setUserEmail(user.getUserEmail());
                    existingUser.setUserAbout(user.getUserAbout());
                    existingUser.setUserGender(user.getUserGender());
                    return userRepository.save(existingUser);
                })
                .orElseThrow(() -> new ResourceNotFound("User with given ID is not found on server"));
    }

    @Override
    public void deleteUser(String userId){
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFound("User with given ID is not found on server");
        }
        userRepository.deleteById(userId);
    }
}
