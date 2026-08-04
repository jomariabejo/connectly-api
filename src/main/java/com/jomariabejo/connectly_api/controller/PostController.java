package com.jomariabejo.connectly_api.controller;

import com.jomariabejo.connectly_api.dto.post.CreatePostDto;
import com.jomariabejo.connectly_api.dto.post.PostResponseDto;
import com.jomariabejo.connectly_api.dto.post.UpdatePostDto;
import com.jomariabejo.connectly_api.dto.PaginationDto;
import com.jomariabejo.connectly_api.dto.PostFilterDto;
import com.jomariabejo.connectly_api.service.AuthenticationService;
import com.jomariabejo.connectly_api.service.PostService;
import com.jomariabejo.connectly_api.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/posts")
@Tag(name = "Posts", description = "Create, read, update and delete posts. Reads are owner-scoped: a post is only visible to its author.")
@SecurityRequirement(name = "bearerAuth")
public class PostController {

    private final PostService postService;
    private final AuthenticationService authenticationService;

    public PostController(PostService postService, AuthenticationService authenticationService) {
        this.postService = postService;
        this.authenticationService = authenticationService;
    }

    @Operation(summary = "Create a post", description = "The authenticated user becomes the post's author.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Post created"),
            @ApiResponse(responseCode = "400", description = "Validation failed on the request body"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token")
    })
    @PostMapping
    public ResponseEntity<CreatePostDto> createPost(
            @RequestBody @Valid CreatePostDto createPostDto) {

        User currentUser = authenticationService.getAuthenticatedUser();

        CreatePostDto createPost = postService.createPost(createPostDto, currentUser);

        return ResponseEntity.status(201).body(createPost);
    }

    @Operation(
            summary = "Get a single post",
            description = "Returns 403 both when the post does not exist and when the caller is not its author -- "
                    + "PostService.getPost throws in both cases and the controller maps either to FORBIDDEN, so a "
                    + "missing post is deliberately indistinguishable from someone else's post.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Post returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token"),
            @ApiResponse(responseCode = "403", description = "Post not found, or not owned by the caller")
    })
    @GetMapping("/{id}")
    public ResponseEntity<PostResponseDto> getPost(@PathVariable Long id) {
        User currentUser = authenticationService.getAuthenticatedUser();

        PostResponseDto postResponseDto;
        try {
            postResponseDto = postService.getPost(id, currentUser);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        return ResponseEntity.ok(postResponseDto);
    }

    @Operation(summary = "Update a post", description = "Only the author may update a post.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Post updated"),
            @ApiResponse(responseCode = "401", description = "Missing bearer token, or the caller is not the author"),
            @ApiResponse(responseCode = "500", description = "No post exists with this id")
    })
    @PutMapping("/{id}")
    public ResponseEntity<PostResponseDto> updatePost(@PathVariable Long id,
                                                      @RequestBody UpdatePostDto updatePostDto) {
        User currentUser = authenticationService.getAuthenticatedUser();

        PostResponseDto updatedPost = postService.updatePost(id, updatePostDto, currentUser);

        return ResponseEntity.ok(updatedPost);
    }

    @Operation(
            summary = "Delete a post",
            description = "Permitted for the author. As with GET, a missing post and an unowned post both yield 403.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Post deleted"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token"),
            @ApiResponse(responseCode = "403", description = "Post not found, or not owned by the caller")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id) {
        User currentUser = authenticationService.getAuthenticatedUser();

        boolean isDeleted = postService.deletePost(id, currentUser);

        if (isDeleted)
            return ResponseEntity.noContent().build();
        else
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @Operation(
            summary = "List the caller's posts",
            description = "Unpaginated. Prefer /posts/my-posts/paginated for anything but small accounts.")
    @ApiResponse(responseCode = "200", description = "Posts returned")
    @GetMapping("/my-posts")
    public ResponseEntity<List<PostResponseDto>> getPostsByAuthenticatedUser() {
        User currentUser = authenticationService.getAuthenticatedUser();

        List<PostResponseDto> posts = postService.getPostsByUser(currentUser.getId());
        return ResponseEntity.ok(posts);
    }

    // Pagination endpoints
    @Operation(
            summary = "List posts (paginated, filterable)",
            description = "Standard Spring Data paging: `?page=0&size=10&sort=createdAt,desc` (that is also the default). "
                    + "Supplying any of the filter parameters switches to the filtered query.")
    @ApiResponse(responseCode = "200", description = "A PaginationDto page of posts")
    @GetMapping
    public ResponseEntity<PaginationDto<PostResponseDto>> getAllPostsPaginated(
            @PageableDefault(size = 10, page = 0, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String content,
            @RequestParam(required = false) String postType,
            @RequestParam(required = false) String privacy,
            @RequestParam(required = false) Long createdById) {

        PostFilterDto filterDto = new PostFilterDto(title, content, postType, privacy, createdById);
        PaginationDto<PostResponseDto> result;

        if (hasFilters(filterDto)) {
            result = postService.getAllPostsWithFilters(filterDto, pageable);
        } else {
            result = postService.getAllPostsPaginated(pageable);
        }

        return ResponseEntity.ok(result);
    }

    @Operation(summary = "List one user's posts (paginated, filterable)")
    @ApiResponse(responseCode = "200", description = "A PaginationDto page of that user's posts")
    @GetMapping("/user/{userId}/paginated")
    public ResponseEntity<PaginationDto<PostResponseDto>> getUserPostsPaginated(
            @PathVariable Long userId,
            @PageableDefault(size = 10, page = 0, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String content,
            @RequestParam(required = false) String postType,
            @RequestParam(required = false) String privacy) {

        PostFilterDto filterDto = new PostFilterDto(title, content, postType, privacy, null);
        PaginationDto<PostResponseDto> result;

        if (hasUserPostFilters(filterDto)) {
            result = postService.getUserPostsWithFilters(userId, filterDto, pageable);
        } else {
            result = postService.getUserPostsPaginated(userId, pageable);
        }

        return ResponseEntity.ok(result);
    }

    @Operation(summary = "List the caller's posts (paginated, filterable)")
    @ApiResponse(responseCode = "200", description = "A PaginationDto page of the caller's posts")
    @GetMapping("/my-posts/paginated")
    public ResponseEntity<PaginationDto<PostResponseDto>> getAuthenticatedUserPostsPaginated(
            @PageableDefault(size = 10, page = 0, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String content,
            @RequestParam(required = false) String postType,
            @RequestParam(required = false) String privacy) {

        User currentUser = authenticationService.getAuthenticatedUser();
        PostFilterDto filterDto = new PostFilterDto(title, content, postType, privacy, null);
        PaginationDto<PostResponseDto> result;

        if (hasUserPostFilters(filterDto)) {
            result = postService.getUserPostsWithFilters(currentUser.getId(), filterDto, pageable);
        } else {
            result = postService.getUserPostsPaginated(currentUser.getId(), pageable);
        }

        return ResponseEntity.ok(result);
    }

    private boolean hasFilters(PostFilterDto filterDto) {
        return filterDto.getTitle() != null || filterDto.getContent() != null ||
               filterDto.getPostType() != null || filterDto.getPrivacy() != null ||
               filterDto.getCreatedById() != null;
    }

    private boolean hasUserPostFilters(PostFilterDto filterDto) {
        return filterDto.getTitle() != null || filterDto.getContent() != null ||
               filterDto.getPostType() != null || filterDto.getPrivacy() != null;
    }
}
