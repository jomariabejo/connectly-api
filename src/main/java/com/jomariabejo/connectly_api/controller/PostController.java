package com.jomariabejo.connectly_api.controller;

import com.jomariabejo.connectly_api.dto.post.CreatePostDto;
import com.jomariabejo.connectly_api.dto.post.PostResponseDto;
import com.jomariabejo.connectly_api.dto.post.UpdatePostDto;
import com.jomariabejo.connectly_api.dto.PaginationDto;
import com.jomariabejo.connectly_api.dto.PostFilterDto;
import com.jomariabejo.connectly_api.service.AuthenticationService;
import com.jomariabejo.connectly_api.service.PostService;
import com.jomariabejo.connectly_api.model.User;
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
public class PostController {

    private final PostService postService;
    private final AuthenticationService authenticationService;

    public PostController(PostService postService, AuthenticationService authenticationService) {
        this.postService = postService;
        this.authenticationService = authenticationService;
    }

    @PostMapping
    public ResponseEntity<CreatePostDto> createPost(
            @RequestBody @Valid CreatePostDto createPostDto) {

        User currentUser = authenticationService.getAuthenticatedUser();

        CreatePostDto createPost = postService.createPost(createPostDto, currentUser);

        return ResponseEntity.status(201).body(createPost);
    }

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

    @PutMapping("/{id}")
    public ResponseEntity<PostResponseDto> updatePost(@PathVariable Long id,
                                                      @RequestBody UpdatePostDto updatePostDto) {
        User currentUser = authenticationService.getAuthenticatedUser();

        PostResponseDto updatedPost = postService.updatePost(id, updatePostDto, currentUser);

        return ResponseEntity.ok(updatedPost);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id) {
        User currentUser = authenticationService.getAuthenticatedUser();

        boolean isDeleted = postService.deletePost(id, currentUser);

        if (isDeleted)
            return ResponseEntity.noContent().build();
        else
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @GetMapping("/my-posts")
    public ResponseEntity<List<PostResponseDto>> getPostsByAuthenticatedUser() {
        User currentUser = authenticationService.getAuthenticatedUser();

        List<PostResponseDto> posts = postService.getPostsByUser(currentUser.getId());
        return ResponseEntity.ok(posts);
    }

    // Pagination endpoints
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
