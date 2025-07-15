package com.user.UserMicroservice.entity;

import lombok.*;

//@Getter
//@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Place {
    private String placeId;
    private String placeName;
    private String placeLocation;
    private String placeAbout;
    private Double averageRating;

    public String getPlaceId() {
        return placeId;
    }

    public void setPlaceId(String placeId) {
        this.placeId = placeId;
    }

    public String getPlaceName() {
        return placeName;
    }

    public void setPlaceName(String placeName) {
        this.placeName = placeName;
    }

    public String getPlaceLocation() {
        return placeLocation;
    }

    public void setPlaceLocation(String placeLocation) {
        this.placeLocation = placeLocation;
    }

    public String getPlaceAbout() {
        return placeAbout;
    }

    public void setPlaceAbout(String placeAbout) {
        this.placeAbout = placeAbout;
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }
}

