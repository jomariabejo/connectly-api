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
    Optional<Comment> findById(Long id);
    void deleteById(Long id);

    List<Comment> findByPostId(Long postId);

    // Pagination methods
    Page<Comment> findByPostId(Long postId, Pageable pageable);

    // Filtering methods
    @Query("SELECT c FROM Comment c WHERE c.post.id = :postId AND " +
            "(:content IS NULL OR c.text LIKE %:content%) AND " +
            "(:createdById IS NULL OR c.user.id = :createdById)")
    Page<Comment> findPostCommentsWithFilters(
            @Param("postId") Long postId,
            @Param("content") String content,
            @Param("createdById") Long createdById,
            Pageable pageable);
}
