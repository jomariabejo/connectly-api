package com.jomariabejo.connectly_api.controller;

import com.jomariabejo.connectly_api.dto.comment.CommentResponseDto;
import com.jomariabejo.connectly_api.dto.comment.CreateCommentDto;
import com.jomariabejo.connectly_api.dto.comment.UpdateCommentDto;
import com.jomariabejo.connectly_api.service.AuthenticationService;
import com.jomariabejo.connectly_api.service.CommentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/posts/{postId}/comments")
public class CommentController {

    private static final Logger log = LoggerFactory.getLogger(CommentController.class);

    private final CommentService commentService;
    private final AuthenticationService authenticationService;

    public CommentController(
            CommentService commentService,
            AuthenticationService authenticationService
    ) {
        this.commentService = commentService;
        this.authenticationService = authenticationService;
    }

    @PostMapping
    public ResponseEntity<CreateCommentDto> addComment(
            @PathVariable Long postId,
            @RequestBody CreateCommentDto createCommentDto
    ) {
        log.info("Adding comment to post {}", postId);
        CreateCommentDto savedComment = commentService.addCommentToPost(
                postId,
                createCommentDto,
                authenticationService.getAuthenticatedUser()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(savedComment);
    }

    @GetMapping("/{commentId}")
    public ResponseEntity<CommentResponseDto> getComment(
            @PathVariable Long postId,
            @PathVariable Long commentId
    ) {
        log.info("Fetching comment {} for post {}", commentId, postId);
        CommentResponseDto comment = commentService.getComment(
                postId,
                commentId,
                authenticationService.getAuthenticatedUser()
        );
        return ResponseEntity.ok(comment);
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<CommentResponseDto> updateComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestBody UpdateCommentDto updateCommentDto
    ) {
        log.info("Updating comment {} for post {}", commentId, postId);
        CommentResponseDto updatedComment = commentService.updateComment(
                commentId,
                updateCommentDto,
                authenticationService.getAuthenticatedUser()
        );
        return ResponseEntity.ok(updatedComment);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long postId,
            @PathVariable Long commentId
    ) {
        log.info("Deleting comment {} for post {}", commentId, postId);
        commentService.deleteComment(
                postId,
                commentId,
                authenticationService.getAuthenticatedUser()
        );
        return ResponseEntity.noContent().build();
    }
}
