package com.newsportal.repository;

import com.newsportal.model.Author;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthorRepository extends JpaRepository<Author, Long> {
    Optional<Author> findByEmail(String email);

    @Query("SELECT COUNT(a) > 0 FROM Author a WHERE a.email = :email AND a.id != :id")
    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByEmail(String email);
}