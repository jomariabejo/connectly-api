package com.jomariabejo.connectly_api.repository;

import com.jomariabejo.connectly_api.model.Like;
import com.jomariabejo.connectly_api.model.Post;
import com.jomariabejo.connectly_api.model.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LikeRespository extends CrudRepository<Like, Long> {
    Like save(Like like);
    Optional<Like> findById(Long id);
    void deleteById(Long id);
    long countByPost(Post post);

    Optional<Like> findByUserAndPost(User user, Post post);// <- Check if already liked
    List<Like> findAllByUser(User user); // Get all likes by user

    // Used when permanently deleting an account: every FK to app_user is NO ACTION,
    // so dependants must be removed before the user row can go.
    void deleteAllByUser(User user);

    void deleteAllByPost(Post post);
}
