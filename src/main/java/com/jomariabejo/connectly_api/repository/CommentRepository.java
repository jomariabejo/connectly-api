package com.jomariabejo.connectly_api.repository;

import com.jomariabejo.connectly_api.model.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    
    Comment save(Comment comment);
    
    @Query("SELECT c FROM Comment c WHERE c.id = :id AND c.user.deletedAt IS NULL")
    Optional<Comment> findById(@Param("id") Long id);
    
    void deleteById(Long id);

    // Used when permanently deleting an account -- see UserService.permanentlyDeleteUser.
    void deleteAllByUser(com.jomariabejo.connectly_api.model.User user);

    void deleteAllByPost(com.jomariabejo.connectly_api.model.Post post);

    @Query("SELECT c FROM Comment c WHERE c.post.id = :postId AND c.user.deletedAt IS NULL")
    List<Comment> findByPostId(@Param("postId") Long postId);

    // Pagination methods with deleted user filtering
    @Query("SELECT c FROM Comment c WHERE c.post.id = :postId AND c.user.deletedAt IS NULL")
    Page<Comment> findByPostId(@Param("postId") Long postId, Pageable pageable);

    // Filtering methods with deleted user filtering
    @Query("SELECT c FROM Comment c WHERE c.post.id = :postId AND c.user.deletedAt IS NULL AND " +
            "(:content IS NULL OR c.text LIKE %:content%) AND " +
            "(:createdById IS NULL OR c.user.id = :createdById)")
    Page<Comment> findPostCommentsWithFilters(
            @Param("postId") Long postId,
            @Param("content") String content,
            @Param("createdById") Long createdById,
            Pageable pageable);
}
