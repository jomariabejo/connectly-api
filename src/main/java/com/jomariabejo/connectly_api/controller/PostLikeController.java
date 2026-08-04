package com.jomariabejo.connectly_api.controller;

import com.jomariabejo.connectly_api.dto.GenericResponse;
import com.jomariabejo.connectly_api.dto.post_like.LikeDto;
import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.service.AuthenticationService;
import com.jomariabejo.connectly_api.service.PostLikeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * All routes here are relative to the class-level {@code @RequestMapping("/{postId}")}.
 *
 * <p>Historical note: {@code toggleLike} used to repeat the segment as
 * {@code @PostMapping("/{postId}/likes/toggle")}, which concatenated into
 * {@code /{postId}/{postId}/likes/toggle}. Spring Boot 3's {@code PathPatternParser} refuses to
 * capture the same variable twice, so that mapping aborted application startup outright -- the
 * endpoint never served a request. It now matches the sibling methods at
 * {@code /{postId}/likes/toggle}.
 */
@RestController
@RequestMapping("/{postId}")
@Tag(name = "Likes", description = "Toggle, count and list post likes. All routes are nested under /{postId}.")
@SecurityRequirement(name = "bearerAuth")
public class PostLikeController {

    private final PostLikeService postLikeService;
    private final AuthenticationService authenticationService;

    public PostLikeController(PostLikeService postLikeService,
                              AuthenticationService authenticationService) {
        this.postLikeService = postLikeService;
        this.authenticationService = authenticationService;
    }

    @Operation(
            summary = "Like or unlike a post",
            description = "Idempotent toggle: returns `true` in `data` if the post is now liked, `false` if the like "
                    + "was removed.")
    @ApiResponse(responseCode = "200", description = "Like toggled; `data` holds the new state")
    @PostMapping("/likes/toggle")
    public ResponseEntity<GenericResponse<Boolean>> toggleLike(@PathVariable Long postId) {

        User currentUser = authenticationService.getAuthenticatedUser();

        boolean isNowLiked = postLikeService.togglePostLike(postId, currentUser);

        String message = isNowLiked
                ? "Post liked successfully."
                : "Post unliked successfully.";

        return ResponseEntity.ok(new GenericResponse<>(message, isNowLiked));
    }

    /**
     * Get like count for a post.
     */
    @Operation(summary = "Count a post's likes")
    @ApiResponse(responseCode = "200", description = "Like count returned in `data`")
    @GetMapping("/likes/count")
    public ResponseEntity<GenericResponse<Long>> countLikes(@PathVariable Long postId) {
        long count = postLikeService.countLikesByPost(postId);
        return ResponseEntity.ok(new GenericResponse<>("Total likes retrieved.", count));
    }

    /**
     * Get all likes by the authenticated user (across all posts).
     * This one doesn't need a postId in the path.
     */
    @Operation(
            summary = "List the caller's likes",
            description = "Returns every like the authenticated user has made across all posts. The `{postId}` in the "
                    + "path is inherited from the class-level mapping and is ignored -- pass any value.")
    @ApiResponse(responseCode = "200", description = "Likes returned in `data`")
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
