package com.jomariabejo.connectly_api.service;

import com.jomariabejo.connectly_api.dto.post.CreatePostDto;
import com.jomariabejo.connectly_api.dto.post.PostResponseDto;
import com.jomariabejo.connectly_api.dto.post.UpdatePostDto;
import com.jomariabejo.connectly_api.exception.UnauthorizedAccessException;
import com.jomariabejo.connectly_api.mapper.PostMapper;
import com.jomariabejo.connectly_api.model.Post;
import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.repository.PostRepository;
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
//    https://www.reddit.com/r/SpringBoot/comments/efwu26/when_to_use_transactional_in_springboot_and_when/
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

        // Save the post and debug the saved post
        Post savedPost = postRepository.save(post);

        // Return the post response as a DTO
        return postMapper.postToCreatePostDto(savedPost);
    }


    public PostResponseDto getPost(Long id, User authenticatedUser) {
        // Fetch the post from the repository using findById
        Optional<Post> optionalPost = postRepository.findById(id);

        if (optionalPost.isEmpty()) {
            throw new RuntimeException("Post with ID " + id + " not found");
        }

        Post post = optionalPost.get();  // Safely retrieve the post


        // Check if the authenticated user is the creator of the post
        if (authenticatedUser.equals(post.getCreatedBy())) {
            return postMapper.postToPostResponseDto(post);
        } else {
            throw new RuntimeException("User " + authenticatedUser.getUsername() + " is not authorized to view this post");
        }
    }

    public List<PostResponseDto> getPostsByUser(Long userId) {
        List<Post> postList = postRepository.findByCreatedById(userId);

        return postList.stream()
                .map(post -> new PostResponseDto(post))  // Assuming a constructor in PostResponseDto
                .collect(Collectors.toList());           // Collect into a list
    }

    public PostResponseDto updatePost(Long id, UpdatePostDto updatePostDto, User authenticatedUser) {
        // Find the existing post
        Post existingPost = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        if (authenticatedUser.equals(existingPost.getCreatedBy())) {

            // Update the fields with the new values
            existingPost.setTitle(updatePostDto.getTitle());
            existingPost.setContent(updatePostDto.getContent());
            existingPost.setMetadata(updatePostDto.getMetadata());
            existingPost.setPostType(updatePostDto.getPostType());
            existingPost.setPrivacy(updatePostDto.getPrivacy());

            // Save the updated post back to the repository
            Post updatedPost = postRepository.save(existingPost);
            return new PostResponseDto(updatedPost);
        }
        else {
            throw new UnauthorizedAccessException("User " + authenticatedUser.getUsername() + " is not authorized to view this post");
        }
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
