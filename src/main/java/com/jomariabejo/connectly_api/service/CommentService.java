package com.jomariabejo.connectly_api.service;

import com.jomariabejo.connectly_api.dto.comment.CommentResponseDto;
import com.jomariabejo.connectly_api.dto.comment.CreateCommentDto;
import com.jomariabejo.connectly_api.dto.comment.UpdateCommentDto;
import com.jomariabejo.connectly_api.exception.CommentNotFoundException;
import com.jomariabejo.connectly_api.exception.PostNotFoundException;
import com.jomariabejo.connectly_api.exception.UnauthorizedAccessException;
import com.jomariabejo.connectly_api.mapper.CommentMapper;
import com.jomariabejo.connectly_api.model.Comment;
import com.jomariabejo.connectly_api.model.Post;
import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.repository.CommentRepository;
import com.jomariabejo.connectly_api.repository.PostRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CommentService {

    private final PostService postService;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;

    public CommentService(
            CommentRepository commentRepository,
            CommentMapper commentMapper,
            PostService postService,
            PostRepository postRepository
    ) {
        this.commentRepository = commentRepository;
        this.commentMapper = commentMapper;
        this.postService = postService;
        this.postRepository = postRepository;
    }

    @Transactional
    public CreateCommentDto addCommentToPost(Long postId, CreateCommentDto createCommentDto, User authenticatedUser) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));

        Comment comment = commentMapper.commentDtoTocomment(createCommentDto);
        if (comment == null) {
            throw new IllegalArgumentException("Failed to map comment DTO to entity.");
        }

        comment.setUser(authenticatedUser);
        comment.setPost(post);

        Comment savedComment = commentRepository.save(comment);
        return commentMapper.commentToCreateCommentDto(savedComment);
    }

    public CommentResponseDto getComment(Long postId, Long commentId, User authenticatedUser) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException("Comment not found with id: " + commentId));

        return commentMapper.commentToCommentResponseDto(comment);
    }

    public List<CommentResponseDto> getPostComments(Long postId) {
        return commentRepository.findByPostId(postId).stream()
                .map(commentMapper::commentToCommentResponseDto)
                .collect(Collectors.toList());
    }

    public CommentResponseDto updateComment(Long commentId, UpdateCommentDto updateCommentDto, User authenticatedUser) {
        Comment existingComment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException("Comment not found with id: " + commentId));

        if (!existingComment.getUser().equals(authenticatedUser)) {
            throw new UnauthorizedAccessException("You are not authorized to update this comment.");
        }

        existingComment.setText(updateCommentDto.getText());
        commentRepository.save(existingComment);
        return commentMapper.commentToCommentResponseDto(existingComment);
    }

    public void deleteComment(Long postId, Long commentId, User authenticatedUser) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException("Comment not found with id: " + commentId));

        if (!comment.getUser().equals(authenticatedUser)) {
            throw new UnauthorizedAccessException("You are not authorized to delete this comment.");
        }

        commentRepository.delete(comment);
    }

    public boolean verifyCommentOwnership(Long commentId, User authenticatedUser) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException("Comment not found with id: " + commentId));

        return comment.getUser().equals(authenticatedUser);
    }
}
