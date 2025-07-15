package com.place.PlaceMicroservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.util.List;

//@Getter
//@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@Table(name = "micro_places")
public class Place {

    @Id
    @Column(name = "PLACE_ID")
    private String placeId;

    @Column(name = "PLACE_NAME")
    private String placeName;

    @Column(name = "PLACE_LOCATION")
    private String placeLocation;

    @Column(name = "PLACE_ABOUT")
    private String placeAbout;

    @Column(name = "PLACE_CREATED_BY")
    private String createdBy;

    @Transient
    private List<Rating> ratings;

    @Transient
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

    public List<Rating> getRatings() {
        return ratings;
    }

    public void setRatings(List<Rating> ratings) {
        this.ratings = ratings;
    }

    public String getCreatedBy(){
        return createdBy;
    }

    public void setCreatedBy(String createdBy){
        this.createdBy = createdBy;
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }
}
