package com.place.PlaceMicroservice.externalservices;

import com.place.PlaceMicroservice.entity.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@FeignClient(name = "USERMICROSERVICE")
public interface UserService {

    @GetMapping("/users/{userId}")
    public User getUser(@PathVariable String userId);
}
