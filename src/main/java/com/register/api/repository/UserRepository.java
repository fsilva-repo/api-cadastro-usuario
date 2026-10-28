package com.register.api.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.register.api.model.User;

public interface UserRepository extends MongoRepository<User, String>{
  Optional<User> findByEmail(String email);
  boolean existsByEmail(String email);
}
