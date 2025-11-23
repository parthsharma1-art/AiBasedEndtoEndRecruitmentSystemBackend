package com.aibackend.AiBasedEndtoEndSystem.repository;

import com.aibackend.AiBasedEndtoEndSystem.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByMobileNumber(String mobileNumber);

    Optional<User> findByName(String name);

}
