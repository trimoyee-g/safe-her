package com.user.UserMicroservice.repository;

import com.user.UserMicroservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    @Query(value = "SELECT * FROM micro_users u WHERE LOWER(u.user_name) LIKE LOWER(CONCAT('%', :name, '%')) LIMIT 5", nativeQuery = true)
    List<User> getUsersByName(@Param("name") String name);
}
