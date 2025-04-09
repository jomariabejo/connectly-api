package com.jomariabejo.connectly_api.service;

import com.jomariabejo.connectly_api.dto.post.CreatePostDto;
import com.jomariabejo.connectly_api.dto.post.PostResponseDto;
import com.jomariabejo.connectly_api.dto.post.UpdatePostDto;
import com.jomariabejo.connectly_api.mapper.PostMapper;
import com.jomariabejo.connectly_api.model.Post;
import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.repository.PostRepository;
import com.jomariabejo.connectly_api.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final PostMapper postMapper;

    public PostService(PostRepository postRepository, PostMapper postMapper) {
        this.postRepository = postRepository;
        this.postMapper = postMapper;
    }

    @Transactional
    public CreatePostDto createPost(CreatePostDto createPostDto, User authenticatedUser) {
        // Map the DTO to a Post entity
        Post post = postMapper.postDtoToPost(createPostDto);

        // Log the post to check if it's being created
        if (post == null) {
            throw new RuntimeException("Post mapping returned null");
        }

        // Associate the post with the authenticated user
        post.setCreatedBy(authenticatedUser);

        // Debug the post object before saving
        System.out.println("Post before save: " + post);

        // Save the post and debug the saved post
        Post savedPost = postRepository.save(post);
        System.out.println("Post saved: " + savedPost);

        // Return the post response as a DTO
        return postMapper.postToCreatePostDto(savedPost);
    }


    public PostResponseDto getPost(Long id, User authenticatedUser) {
        // Fetch the post from the repository using findById
        Optional<Post> optionalPost = postRepository.findById(id);
        System.out.println("Post id is " + optionalPost.get().getId());

        // Log the result to help debugging
        System.out.println("Fetching post with ID: " + id);

        if (optionalPost.isEmpty()) {
            throw new RuntimeException("Post with ID " + id + " not found");
        }

        Post post = optionalPost.get();  // Safely retrieve the post

        System.out.println("Authenticated user is " + authenticatedUser);
        System.out.println("Post Author        is " + post.getCreatedBy());

        // Check if the authenticated user is the creator of the post
        if (authenticatedUser.equals(post.getCreatedBy())) {
            System.out.println("Inside out");  // Log when the user is authorized
            System.out.println("Id is " + post.getId());
            System.out.println("Post Author is " + post.getCreatedBy());
            System.out.println("Post Title is " + post.getTitle());
            System.out.println("Post Content is " + post.getContent());
            System.out.println("Post Author is " + post.getCreatedBy());
            System.out.println("Post Title is " + post.getTitle());
            System.out.println("Post Content is " + post.getContent());

            return new PostResponseDto(post);
        } else {
            System.out.println("Inside in");  // Log when the user is not authorized
            throw new RuntimeException("User " + authenticatedUser.getUsername() + " is not authorized to view this post");
        }
    }


    public List<PostResponseDto> getPostsByUser(Long userId) {
        List<Post> postList = postRepository.findByCreatedById(userId);

        return postList.stream()
                .map(post -> new PostResponseDto(post))  // Assuming a constructor in PostResponseDto
                .collect(Collectors.toList());           // Collect into a list
    }

    public PostResponseDto updatePost(Long id, UpdatePostDto updatePostDto) {
        // Find the existing post
        Post existingPost = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        // Update the fields with the new values
        existingPost.setTitle(updatePostDto.getTitle());
        existingPost.setContent(updatePostDto.getContent());
        existingPost.setMetadata(updatePostDto.getMetadata());
        existingPost.setPostType(updatePostDto.getPostType());
        existingPost.setPrivacy(updatePostDto.getPrivacy());

        // Save the updated post back to the repository
        Post updatedPost = postRepository.save(existingPost);

        // Convert the updated post to a PostResponseDto and return it
        return new PostResponseDto(updatedPost);
    }

    public boolean deletePost(Long id, User authenticatedUser) {
        // Find the post by ID
        Optional<Post> postOptional = postRepository.findById(id);

        if (postOptional.isPresent()) {
            Post post = postOptional.get();

            // Check if the authenticated user is the owner of the post or is an admin
            if (post.getCreatedBy().equals(authenticatedUser) || authenticatedUser.getRoles().contains("ADMIN")) {
                // If the user is the owner or an admin, delete the post
                postRepository.delete(post);
                return true;
            }
        }

        // If post is not found or user is not authorized, return false
        return false;
    }
}
