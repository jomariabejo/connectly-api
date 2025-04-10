package com.jomariabejo.connectly_api.controller;

import com.jomariabejo.connectly_api.dto.post.CreatePostDto;
import com.jomariabejo.connectly_api.dto.post.PostResponseDto;
import com.jomariabejo.connectly_api.dto.post.UpdatePostDto;
import com.jomariabejo.connectly_api.service.AuthenticationService;
import com.jomariabejo.connectly_api.service.PostService;
import com.jomariabejo.connectly_api.model.User;
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
}
