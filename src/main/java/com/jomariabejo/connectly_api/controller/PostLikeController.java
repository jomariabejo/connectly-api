package com.jomariabejo.connectly_api.controller;

import com.jomariabejo.connectly_api.dto.GenericResponse;
import com.jomariabejo.connectly_api.dto.post_like.LikeDto;
import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.service.AuthenticationService;
import com.jomariabejo.connectly_api.service.PostLikeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/{postId}")
public class PostLikeController {

    private final PostLikeService postLikeService;
    private final AuthenticationService authenticationService;

    public PostLikeController(PostLikeService postLikeService,
                              AuthenticationService authenticationService) {
        this.postLikeService = postLikeService;
        this.authenticationService = authenticationService;
    }

    /**
     * Toggle like/unlike for a post.
     */
    @PostMapping("/likes")
    public ResponseEntity<GenericResponse<Boolean>> toggleLike(@PathVariable Long postId) {
        User currentUser = authenticationService.getAuthenticatedUser();
        Boolean isNowLiked = postLikeService.togglePostLike(postId, currentUser);

        String message = isNowLiked ? "Post liked successfully." : "Post unliked successfully.";
        return ResponseEntity.ok(new GenericResponse<>(message, isNowLiked));
    }

    /**
     * Get like count for a post.
     */
    @GetMapping("/likes/count")
    public ResponseEntity<GenericResponse<Long>> countLikes(@PathVariable Long postId) {
        long count = postLikeService.countLikesByPost(postId);
        return ResponseEntity.ok(new GenericResponse<>("Total likes retrieved.", count));
    }

    /**
     * Get all likes by the authenticated user (across all posts).
     * This one doesn't need a postId in the path.
     */
    @GetMapping("/likes/my-likes")
    public ResponseEntity<GenericResponse<List<LikeDto>>> getMyLikes() {
        User currentUser = authenticationService.getAuthenticatedUser();

        List<LikeDto> likeDtos = postLikeService.getLikesByUser(currentUser)
                .stream()
                .map(like -> new LikeDto(
                        like.getId(),
                        like.getPost().getId(),
                        like.getCreatedAt()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(new GenericResponse<>("User likes retrieved successfully.", likeDtos));
    }
}
