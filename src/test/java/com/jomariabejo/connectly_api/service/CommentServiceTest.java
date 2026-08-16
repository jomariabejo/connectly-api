package com.jomariabejo.connectly_api.service;

import com.jomariabejo.connectly_api.dto.CommentFilterDto;
import com.jomariabejo.connectly_api.dto.PaginationDto;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CommentService}. Pure Mockito -- no Spring context and no database.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CommentService")
class CommentServiceTest {

    @Mock
    private PostService postService;

    @Mock
    private PostRepository postRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CommentMapper commentMapper;

    @InjectMocks
    private CommentService commentService;

    private User author;
    private User stranger;
    private Post post;
    private Comment comment;

    @BeforeEach
    void setUp() {
        author = new User("author", "secret", "author@example.com");
        author.setId(1L);

        stranger = new User("stranger", "secret", "stranger@example.com");
        stranger.setId(2L);

        post = new Post();
        post.setId(10L);

        comment = new Comment();
        comment.setId(100L);
        comment.setText("Nice post!");
        comment.setPost(post);
        comment.setUser(author);
    }

    @Nested
    @DisplayName("addCommentToPost")
    class AddComment {

        @Test
        @DisplayName("links the comment to the post and the authenticated user")
        void linksPostAndAuthor() {
            CreateCommentDto request = new CreateCommentDto();
            request.setText("Nice post!");

            Comment mapped = new Comment();
            when(postRepository.findById(10L)).thenReturn(Optional.of(post));
            when(commentMapper.commentDtoTocomment(request)).thenReturn(mapped);
            when(commentRepository.save(mapped)).thenReturn(comment);
            when(commentMapper.commentToCreateCommentDto(comment)).thenReturn(request);

            CreateCommentDto result = commentService.addCommentToPost(10L, request, author);

            assertThat(mapped.getUser()).isEqualTo(author);
            assertThat(mapped.getPost()).isEqualTo(post);
            assertThat(result).isEqualTo(request);
        }

        @Test
        @DisplayName("throws PostNotFoundException when the post is missing")
        void throwsWhenPostMissing() {
            when(postRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.addCommentToPost(99L, new CreateCommentDto(), author))
                    .isInstanceOf(PostNotFoundException.class);

            verify(commentRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws IllegalArgumentException when the mapper yields nothing")
        void throwsWhenMappingReturnsNull() {
            CreateCommentDto request = new CreateCommentDto();
            when(postRepository.findById(10L)).thenReturn(Optional.of(post));
            when(commentMapper.commentDtoTocomment(request)).thenReturn(null);

            assertThatThrownBy(() -> commentService.addCommentToPost(10L, request, author))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Failed to map comment DTO to entity.");
        }
    }

    @Nested
    @DisplayName("getComment")
    class GetComment {

        @Test
        @DisplayName("returns the comment")
        void returnsComment() {
            CommentResponseDto expected = new CommentResponseDto(comment);
            when(commentRepository.findById(100L)).thenReturn(Optional.of(comment));
            when(commentMapper.commentToCommentResponseDto(comment)).thenReturn(expected);

            assertThat(commentService.getComment(10L, 100L, author)).isEqualTo(expected);
        }

        @Test
        @DisplayName("readable by any authenticated user, not just the author")
        void readableByNonAuthor() {
            CommentResponseDto expected = new CommentResponseDto(comment);
            when(commentRepository.findById(100L)).thenReturn(Optional.of(comment));
            when(commentMapper.commentToCommentResponseDto(comment)).thenReturn(expected);

            // Comments are public within their post -- only editing and deleting are author-only.
            assertThat(commentService.getComment(10L, 100L, stranger)).isEqualTo(expected);
        }

        @Test
        @DisplayName("rejects a comment that belongs to a different post")
        void rejectsPostIdMismatch() {
            when(commentRepository.findById(100L)).thenReturn(Optional.of(comment));

            // The postId used to be accepted and ignored, so /posts/999/comments/100 happily
            // returned a comment belonging to post 10.
            assertThatThrownBy(() -> commentService.getComment(999L, 100L, author))
                    .isInstanceOf(CommentNotFoundException.class)
                    .hasMessageContaining("does not belong to post 999");
        }

        @Test
        @DisplayName("throws CommentNotFoundException when the comment is missing")
        void throwsWhenMissing() {
            when(commentRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.getComment(10L, 404L, author))
                    .isInstanceOf(CommentNotFoundException.class)
                    .hasMessageContaining("404");
        }
    }

    @Nested
    @DisplayName("updateComment")
    class UpdateComment {

        @Test
        @DisplayName("rewrites the text for the comment's author")
        void updatesTextForOwner() {
            UpdateCommentDto request = new UpdateCommentDto();
            request.setText("Edited");

            when(commentRepository.findById(100L)).thenReturn(Optional.of(comment));
            when(commentMapper.commentToCommentResponseDto(comment)).thenReturn(new CommentResponseDto(comment));

            commentService.updateComment(100L, request, author);

            assertThat(comment.getText()).isEqualTo("Edited");
            verify(commentRepository).save(comment);
        }

        @Test
        @DisplayName("rejects a non-author with UnauthorizedAccessException")
        void rejectsNonOwner() {
            UpdateCommentDto request = new UpdateCommentDto();
            request.setText("Edited");
            when(commentRepository.findById(100L)).thenReturn(Optional.of(comment));

            assertThatThrownBy(() -> commentService.updateComment(100L, request, stranger))
                    .isInstanceOf(UnauthorizedAccessException.class)
                    .hasMessage("You are not authorized to update this comment.");

            assertThat(comment.getText()).isEqualTo("Nice post!");
            verify(commentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteComment")
    class DeleteComment {

        @Test
        @DisplayName("deletes for the comment's author")
        void deletesForOwner() {
            when(commentRepository.findById(100L)).thenReturn(Optional.of(comment));

            commentService.deleteComment(10L, 100L, author);

            verify(commentRepository).delete(comment);
        }

        @Test
        @DisplayName("rejects a non-author with UnauthorizedAccessException")
        void rejectsNonOwner() {
            when(commentRepository.findById(100L)).thenReturn(Optional.of(comment));

            assertThatThrownBy(() -> commentService.deleteComment(10L, 100L, stranger))
                    .isInstanceOf(UnauthorizedAccessException.class)
                    .hasMessage("You are not authorized to delete this comment.");

            verify(commentRepository, never()).delete(any());
        }

        @Test
        @DisplayName("throws CommentNotFoundException when the comment is missing")
        void throwsWhenMissing() {
            when(commentRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.deleteComment(10L, 404L, author))
                    .isInstanceOf(CommentNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("verifyCommentOwnership")
    class VerifyOwnership {

        @Test
        @DisplayName("true for the author, false for anyone else")
        void reportsOwnership() {
            when(commentRepository.findById(100L)).thenReturn(Optional.of(comment));

            assertThat(commentService.verifyCommentOwnership(100L, author)).isTrue();
            assertThat(commentService.verifyCommentOwnership(100L, stranger)).isFalse();
        }
    }

    @Nested
    @DisplayName("pagination")
    class Pagination {

        @Test
        @DisplayName("maps a Page into the PaginationDto envelope")
        void mapsPageMetadata() {
            Pageable pageable = PageRequest.of(0, 10);
            when(commentRepository.findByPostId(10L, pageable))
                    .thenReturn(new PageImpl<>(List.of(comment), pageable, 1));
            when(commentMapper.commentToCommentResponseDto(comment)).thenReturn(new CommentResponseDto(comment));

            PaginationDto<CommentResponseDto> result = commentService.getPostCommentsPaginated(10L, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getPageNumber()).isZero();
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.isHasNext()).isFalse();
            assertThat(result.isHasPrevious()).isFalse();
        }

        @Test
        @DisplayName("maps an empty Page into an empty envelope without touching the mapper")
        void mapsEmptyPage() {
            Pageable pageable = PageRequest.of(0, 10);
            when(commentRepository.findByPostId(10L, pageable))
                    .thenReturn(new PageImpl<>(List.<Comment>of(), pageable, 0));

            PaginationDto<CommentResponseDto> result = commentService.getPostCommentsPaginated(10L, pageable);

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
            assertThat(result.getTotalPages()).isZero();
            assertThat(result.isHasNext()).isFalse();
            assertThat(result.isHasPrevious()).isFalse();
            // No stubbing of commentMapper here: with nothing to map, strict stubs would flag it.
        }

        @Test
        @DisplayName("passes the filter fields through to the repository query")
        void forwardsFilters() {
            Pageable pageable = PageRequest.of(0, 10);
            CommentFilterDto filter = new CommentFilterDto("Nice", 1L, 10L);
            when(commentRepository.findPostCommentsWithFilters(10L, "Nice", 1L, pageable))
                    .thenReturn(new PageImpl<>(List.of(comment), pageable, 1));
            when(commentMapper.commentToCommentResponseDto(comment)).thenReturn(new CommentResponseDto(comment));

            PaginationDto<CommentResponseDto> result =
                    commentService.getPostCommentsWithFilters(10L, filter, pageable);

            assertThat(result.getContent()).hasSize(1);
            verify(commentRepository).findPostCommentsWithFilters(10L, "Nice", 1L, pageable);
        }
    }
}
