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
    
    @Query("SELECT p FROM Post p WHERE p.createdBy.deletedAt IS NULL")
    List<Post> findAll();
    
    @Query("SELECT p FROM Post p WHERE p.id = :id AND p.createdBy.deletedAt IS NULL")
    Optional<Post> findById(@Param("id") Long id);
    
    Optional<Post> findByTitle(String title);
    
    Post save(Post post);
    
    void delete(Post post);
    
    @Query("SELECT p FROM Post p WHERE p.createdBy.id = :userId AND p.createdBy.deletedAt IS NULL")
    List<Post> findByCreatedById(@Param("userId") Long userId);

    // Pagination and sorting methods with deleted user filtering
    @Query("SELECT p FROM Post p WHERE p.createdBy.deletedAt IS NULL")
    Page<Post> findAll(Pageable pageable);
    
    @Query("SELECT p FROM Post p WHERE p.createdBy.id = :userId AND p.createdBy.deletedAt IS NULL")
    Page<Post> findByCreatedById(@Param("userId") Long userId, Pageable pageable);

    // Filtering methods with deleted user filtering
    @Query("SELECT p FROM Post p WHERE " +
            "p.createdBy.deletedAt IS NULL AND " +
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

    @Query("SELECT p FROM Post p WHERE p.createdBy.id = :userId AND p.createdBy.deletedAt IS NULL AND " +
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

    // Home feed: posts from users the authenticated user follows or their own posts
    // Filters by privacy level and user active status
    @Query("SELECT p FROM Post p WHERE " +
            "p.createdBy.deletedAt IS NULL AND " +
            "(p.createdBy.id = :userId OR " +  // User's own posts
            "EXISTS (SELECT f FROM Follow f WHERE f.follower.id = :userId AND f.following = p.createdBy AND f.approved = true)) " +
            "ORDER BY p.createdAt DESC")
    Page<Post> findHomeFeed(@Param("userId") Long userId, Pageable pageable);
}
