package com.place.PlaceMicroservice.externalservices;

import com.place.PlaceMicroservice.entity.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@FeignClient(name = "USERMICROSERVICE")
public interface UserService {

    @GetMapping("/users/{userId}")
    public User getSingleUser(@PathVariable String userId);

    @GetMapping("/users/search")
    public ResponseEntity<?> getUsersByName(@RequestParam("name") String name);
}
