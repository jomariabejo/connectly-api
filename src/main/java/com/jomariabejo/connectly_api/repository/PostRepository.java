package com.jomariabejo.connectly_api.repository;

import com.jomariabejo.connectly_api.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findAll();
    Optional<Post> findById(Long id);
    Optional<Post> findByTitle(String title);
    Post save(Post post);
    void delete(Post post);
    List<Post> findByCreatedById(Long userId);
}
