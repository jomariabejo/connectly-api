package com.jomariabejo.connectly_api.controller;

import com.jomariabejo.connectly_api.dto.post.CreatePostDto;
import com.jomariabejo.connectly_api.dto.post.PostResponseDto;
import com.jomariabejo.connectly_api.dto.post.UpdatePostDto;
import com.jomariabejo.connectly_api.service.PostService;
import com.jomariabejo.connectly_api.model.User;
import org.hibernate.sql.Update;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping
    public ResponseEntity<CreatePostDto> createPost(
            @RequestBody @Valid CreatePostDto createPostDto) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User authenticatedUser = (User) authentication.getPrincipal();

        CreatePostDto createPost = postService.createPost(createPostDto, authenticatedUser);

        return ResponseEntity.status(201).body(createPost);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostResponseDto> getPost(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User authenticatedUser = (User) authentication.getPrincipal();

        PostResponseDto postResponseDto;
        try {
            System.out.println("Inside try start");
            postResponseDto = postService.getPost(id, authenticatedUser);
            System.out.println("Inside try end");
        } catch (RuntimeException e) {
            System.out.println(e.getMessage());
            System.out.println("Inside catch start");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        return ResponseEntity.ok(postResponseDto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PostResponseDto> updatePost(@PathVariable Long id, @RequestBody UpdatePostDto updatePostDto) {
        PostResponseDto updatedPost = postService.updatePost(id, updatePostDto);

        return ResponseEntity.ok(updatedPost);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id) {
        // Get the currently authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User authenticatedUser = (User) authentication.getPrincipal();

        // Call the service to delete the post and check if the user is allowed to do so
        boolean isDeleted = postService.deletePost(id, authenticatedUser);

        if (isDeleted) {
            // Return HTTP status 204 (No Content) if the deletion was successful
            return ResponseEntity.noContent().build();
        } else {
            // Return HTTP status 403 (Forbidden) if the user does not have permission
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @GetMapping("/my-posts")
    public ResponseEntity<List<PostResponseDto>> getPostsByAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User authenticatedUser = (User) authentication.getPrincipal();

        List<PostResponseDto> posts = postService.getPostsByUser(authenticatedUser.getId());
        return ResponseEntity.ok(posts); // Return 200 OK
    }
}
