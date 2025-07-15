package com.auth.AuthMicroservice.external;

import com.auth.AuthMicroservice.payload.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "USERMICROSERVICE") // same as registered in Eureka
public interface UserClient {

    @PostMapping("/users")
    void createUser(UserDto user);
}
