package com.user.UserMicroservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name="micro_users")
public class User {

    @Id
    @Column(name="USER_ID")
    private String userId;

    @Column(name="USER_NAME")
    private String userName;

    @Column(name="USER_EMAIL")
    private String userEmail;

    @Column(name="USER_ABOUT")
    private String userAbout;

    @Column(name="USER_GENDER")
    private String userGender;

    @Transient
    private List<Rating> userRatings = new ArrayList<>();

//    public List<Rating> getRatings() {
//        return userRatings;
//    }
//
//    public void setRatings(List<Rating> ratings) {
//        this.userRatings = ratings;
//    }
//
//    public String getUserId() {
//        return userId;
//    }
//
//    public void setUserId(String userId) {
//        this.userId = userId;
//    }
//
//    public String getUserName() {
//        return userName;
//    }
//
//    public void setUserName(String userName) {
//        this.userName = userName;
//    }
//
//    public String getUserEmail() {
//        return userEmail;
//    }
//
//    public void setUserEmail(String userEmail) {
//        this.userEmail = userEmail;
//    }
//
//    public String getUserAbout() {
//        return userAbout;
//    }
//
//    public void setUserAbout(String userAbout) {
//        this.userAbout = userAbout;
//    }
//
//    public String getUserGender() {
//        return userGender;
//    }
//
//    public void setUserGender(String userGender) {
//        this.userGender = userGender;
//    }
//
//    public List<Rating> getUserRatings() {
//        return userRatings;
//    }
//
//    public void setUserRatings(List<Rating> userRatings) {
//        this.userRatings = userRatings;
//    }
}
