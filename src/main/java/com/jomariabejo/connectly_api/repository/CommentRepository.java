package com.jomariabejo.connectly_api.repository;

import com.jomariabejo.connectly_api.model.Comment;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentRepository extends CrudRepository<Comment, Long> {
    Comment save(Comment comment);
    Optional<Comment> findById(Long id);
    void deleteById(Long id);

    List<Comment> findByPostId(Long postId);
}
