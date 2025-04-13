package com.jomariabejo.connectly_api.service;

import com.jomariabejo.connectly_api.dto.post_like.LikeDto;
import com.jomariabejo.connectly_api.exception.PostNotFoundException;
import com.jomariabejo.connectly_api.model.Like;
import com.jomariabejo.connectly_api.model.Post;
import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.repository.LikeRespository;
import com.jomariabejo.connectly_api.repository.PostRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;

@Service
public class PostLikeService {
    private final LikeRespository likeRespository;
    private final PostRepository postRepository;

    public PostLikeService(LikeRespository likeRespository, PostRepository postRepository) {
        this.likeRespository = likeRespository;
        this.postRepository = postRepository;
    }

    @Transactional
    public Boolean createPostLike(Long postId, User authenticatedUser) {
        Optional<Post> optionalPost = postRepository.findById(postId);

        if (optionalPost.isPresent()) {
            Post post = optionalPost.get();

            Optional<Like> likeOption = likeRespository.findByUserAndPost(authenticatedUser, post);

            if (likeOption.isPresent())
                return Boolean.FALSE;
            else {
                Like like = new Like();
                like.setUser(authenticatedUser);
                like.setPost(post);
                likeRespository.save(like);

                return Boolean.TRUE;
            }
        } else {
            throw new PostNotFoundException(postId);
        }
    }

    public long countLikesByPost(Long postId) {
        Optional<Post> optionalPost = postRepository.findById(postId);
        if (optionalPost.isPresent()) {

            boolean isPostPublic = optionalPost.get().getPrivacy().equals("public");
            if (isPostPublic) {
                Post post = optionalPost.get();
                return likeRespository.countByPost(post);
            }
        }
        throw new PostNotFoundException(postId);
    }

    public long countLikesByUser(Long postId) {
        Optional<Post> optionalPost = postRepository.findById(postId);
        return optionalPost.map(likeRespository::countByPost).orElse(0L);
    }

    // we can use like dto to reduce response load.
    public List<Like> getLikesByUser(User authenticatedUser) {
        return likeRespository.findAllByUser(authenticatedUser);
    }

    @Transactional
    public Boolean togglePostLike(Long postId, User authenticatedUser) {
        Optional<Post> optionalPost = postRepository.findById(postId);

        if (optionalPost.isPresent()) {
            Post post = optionalPost.get();

            Optional<Like> likeOption = likeRespository.findByUserAndPost(authenticatedUser, post);

            if (likeOption.isPresent()) {
                // User already liked → delete (unlike)
                likeRespository.deleteById(likeOption.get().getId());
                return Boolean.FALSE; // false = unliked
            } else {
                // User hasn't liked yet → create
                Like like = new Like(authenticatedUser, post);
                likeRespository.save(like);
                return Boolean.TRUE; // true = liked
            }
        } else {
            throw new PostNotFoundException(postId);
        }
    }

}
