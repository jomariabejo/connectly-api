package com.jomariabejo.connectly_api.repository;

import com.jomariabejo.connectly_api.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    // Pagination and sorting methods
    Page<Post> findAll(Pageable pageable);
    Page<Post> findByCreatedById(Long userId, Pageable pageable);

    // Filtering methods
    @Query("SELECT p FROM Post p WHERE " +
            "(:title IS NULL OR p.title LIKE %:title%) AND " +
            "(:content IS NULL OR p.content LIKE %:content%) AND " +
            "(:postType IS NULL OR p.postType = :postType) AND " +
            "(:privacy IS NULL OR p.privacy = :privacy) AND " +
            "(:createdById IS NULL OR p.createdBy.id = :createdById)")
    Page<Post> findWithFilters(
            @Param("title") String title,
            @Param("content") String content,
            @Param("postType") String postType,
            @Param("privacy") String privacy,
            @Param("createdById") Long createdById,
            Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.createdBy.id = :userId AND " +
            "(:title IS NULL OR p.title LIKE %:title%) AND " +
            "(:content IS NULL OR p.content LIKE %:content%) AND " +
            "(:postType IS NULL OR p.postType = :postType) AND " +
            "(:privacy IS NULL OR p.privacy = :privacy)")
    Page<Post> findUserPostsWithFilters(
            @Param("userId") Long userId,
            @Param("title") String title,
            @Param("content") String content,
            @Param("postType") String postType,
            @Param("privacy") String privacy,
            Pageable pageable);
}
