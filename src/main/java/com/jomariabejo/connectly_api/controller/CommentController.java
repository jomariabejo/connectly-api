package com.jomariabejo.connectly_api.controller;

import com.jomariabejo.connectly_api.dto.comment.CommentResponseDto;
import com.jomariabejo.connectly_api.dto.comment.CreateCommentDto;
import com.jomariabejo.connectly_api.dto.comment.UpdateCommentDto;
import com.jomariabejo.connectly_api.dto.CommentFilterDto;
import com.jomariabejo.connectly_api.dto.PaginationDto;
import com.jomariabejo.connectly_api.service.AuthenticationService;
import com.jomariabejo.connectly_api.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/posts/{postId}/comments")
@Tag(name = "Comments", description = "Comments nested under a post. Editing and deleting are restricted to the comment's author.")
@SecurityRequirement(name = "bearerAuth")
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

    @Operation(summary = "Add a comment to a post")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Comment created"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token"),
            @ApiResponse(responseCode = "500", description = "No post exists with this id")
    })
    @PostMapping
    public ResponseEntity<CreateCommentDto> addComment(
            @PathVariable Long postId,
            @Valid @RequestBody CreateCommentDto createCommentDto
    ) {
        log.info("Adding comment to post {}", postId);
        CreateCommentDto savedComment = commentService.addCommentToPost(
                postId,
                createCommentDto,
                authenticationService.getAuthenticatedUser()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(savedComment);
    }

    @Operation(summary = "Get a single comment")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comment returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token"),
            @ApiResponse(responseCode = "404", description = "No comment exists with this id")
    })
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

    @Operation(summary = "Update a comment", description = "Only the comment's author may edit it.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comment updated"),
            @ApiResponse(responseCode = "401", description = "Missing bearer token, or the caller is not the comment's author"),
            @ApiResponse(responseCode = "404", description = "No comment exists with this id")
    })
    @PutMapping("/{commentId}")
    public ResponseEntity<CommentResponseDto> updateComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @Valid @RequestBody UpdateCommentDto updateCommentDto
    ) {
        log.info("Updating comment {} for post {}", commentId, postId);
        CommentResponseDto updatedComment = commentService.updateComment(
                commentId,
                updateCommentDto,
                authenticationService.getAuthenticatedUser()
        );
        return ResponseEntity.ok(updatedComment);
    }

    @Operation(summary = "Delete a comment", description = "Only the comment's author may delete it.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Comment deleted"),
            @ApiResponse(responseCode = "401", description = "Missing bearer token, or the caller is not the comment's author"),
            @ApiResponse(responseCode = "404", description = "No comment exists with this id")
    })
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

    // Pagination endpoints
    @Operation(
            summary = "List a post's comments (paginated, filterable)",
            description = "Defaults to `?page=0&size=10&sort=createdAt,desc`. Passing `content` or `createdById` "
                    + "switches to the filtered query.")
    @ApiResponse(responseCode = "200", description = "A PaginationDto page of comments")
    @GetMapping
    public ResponseEntity<PaginationDto<CommentResponseDto>> getPostCommentsPaginated(
            @PathVariable Long postId,
            @PageableDefault(size = 10, page = 0, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String content,
            @RequestParam(required = false) Long createdById) {

        log.info("Fetching paginated comments for post {}", postId);
        CommentFilterDto filterDto = new CommentFilterDto(content, createdById, postId);
        PaginationDto<CommentResponseDto> result;

        if (hasFilters(filterDto)) {
            result = commentService.getPostCommentsWithFilters(postId, filterDto, pageable);
        } else {
            result = commentService.getPostCommentsPaginated(postId, pageable);
        }

        return ResponseEntity.ok(result);
    }

    private boolean hasFilters(CommentFilterDto filterDto) {
        return filterDto.getContent() != null || filterDto.getCreatedById() != null;
    }
}
