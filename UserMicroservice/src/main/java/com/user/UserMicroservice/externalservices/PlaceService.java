package com.user.UserMicroservice.externalservices;

import com.user.UserMicroservice.entity.Place;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "PLACEMICROSERVICE")
public interface PlaceService {

    @GetMapping("/places/{placeId}")
    public Place getPlace(@PathVariable String placeId);
}
