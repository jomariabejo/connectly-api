package com.jomariabejo.connectly_api.service;

import com.jomariabejo.connectly_api.dto.PaginationDto;
import com.jomariabejo.connectly_api.dto.PostFilterDto;
import com.jomariabejo.connectly_api.dto.post.CreatePostDto;
import com.jomariabejo.connectly_api.dto.post.PostResponseDto;
import com.jomariabejo.connectly_api.dto.post.UpdatePostDto;
import com.jomariabejo.connectly_api.exception.UnauthorizedAccessException;
import com.jomariabejo.connectly_api.mapper.PostMapper;
import com.jomariabejo.connectly_api.model.Post;
import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
 * Unit tests for {@link PostService}. Pure Mockito -- no Spring context and no database.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PostService")
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostMapper postMapper;

    @InjectMocks
    private PostService postService;

    private User author;
    private User stranger;
    private Post post;

    @BeforeEach
    void setUp() {
        author = new User("author", "secret", "author@example.com");
        author.setId(1L);

        stranger = new User("stranger", "secret", "stranger@example.com");
        stranger.setId(2L);

        post = new Post();
        post.setId(10L);
        post.setTitle("A title");
        post.setContent("Some content");
        post.setCreatedBy(author);
    }

    @Nested
    @DisplayName("createPost")
    class CreatePost {

        @Test
        @DisplayName("stamps the authenticated user as the author before saving")
        void assignsAuthorAndSaves() {
            CreatePostDto request = new CreatePostDto();
            request.setTitle("A title");
            request.setContent("Some content");

            Post mapped = new Post();
            when(postMapper.postDtoToPost(request)).thenReturn(mapped);
            when(postRepository.save(mapped)).thenReturn(post);
            when(postMapper.postToCreatePostDto(post)).thenReturn(request);

            CreatePostDto result = postService.createPost(request, author);

            ArgumentCaptor<Post> saved = ArgumentCaptor.forClass(Post.class);
            verify(postRepository).save(saved.capture());
            assertThat(saved.getValue().getCreatedBy())
                    .as("the author must come from the token, never from the request body")
                    .isEqualTo(author);
            assertThat(result).isEqualTo(request);
        }

        @Test
        @DisplayName("fails loudly when the mapper yields nothing")
        void throwsWhenMappingReturnsNull() {
            CreatePostDto request = new CreatePostDto();
            when(postMapper.postDtoToPost(request)).thenReturn(null);

            assertThatThrownBy(() -> postService.createPost(request, author))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Post mapping returned null");

            verify(postRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getPost")
    class GetPost {

        @Test
        @DisplayName("returns the post to its author")
        void returnsPostForOwner() {
            PostResponseDto expected = new PostResponseDto(post);
            when(postRepository.findById(10L)).thenReturn(Optional.of(post));
            when(postMapper.postToPostResponseDto(post)).thenReturn(expected);

            assertThat(postService.getPost(10L, author)).isEqualTo(expected);
        }

        @Test
        @DisplayName("throws when the post does not exist")
        void throwsWhenMissing() {
            when(postRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> postService.getPost(99L, author))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Post with ID 99 not found");
        }

        @Test
        @DisplayName("throws when the caller is not the author")
        void throwsForNonOwner() {
            when(postRepository.findById(10L)).thenReturn(Optional.of(post));

            assertThatThrownBy(() -> postService.getPost(10L, stranger))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("is not authorized to view this post");
        }
    }

    @Nested
    @DisplayName("updatePost")
    class UpdatePost {

        @Test
        @DisplayName("applies every mutable field for the author")
        void updatesFieldsForOwner() {
            UpdatePostDto request = new UpdatePostDto();
            request.setTitle("New title");
            request.setContent("New content");
            request.setPostType("text");
            request.setPrivacy("private");
            request.setMetadata("{}");

            when(postRepository.findById(10L)).thenReturn(Optional.of(post));
            when(postRepository.save(post)).thenReturn(post);

            PostResponseDto result = postService.updatePost(10L, request, author);

            assertThat(post.getTitle()).isEqualTo("New title");
            assertThat(post.getContent()).isEqualTo("New content");
            assertThat(post.getPostType()).isEqualTo("text");
            assertThat(post.getPrivacy()).isEqualTo("private");
            assertThat(result.getTitle()).isEqualTo("New title");
        }

        @Test
        @DisplayName("rejects a non-author with UnauthorizedAccessException")
        void rejectsNonOwner() {
            when(postRepository.findById(10L)).thenReturn(Optional.of(post));

            assertThatThrownBy(() -> postService.updatePost(10L, new UpdatePostDto(), stranger))
                    .isInstanceOf(UnauthorizedAccessException.class);

            verify(postRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws a bare RuntimeException when the post is missing")
        void throwsWhenMissing() {
            when(postRepository.findById(99L)).thenReturn(Optional.empty());

            // Note: a plain RuntimeException, so GlobalExceptionHandler maps this to 500, not 404.
            assertThatThrownBy(() -> postService.updatePost(99L, new UpdatePostDto(), author))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Post not found");
        }
    }

    @Nested
    @DisplayName("deletePost")
    class DeletePost {

        @Test
        @DisplayName("deletes and reports true for the author")
        void deletesForOwner() {
            when(postRepository.findById(10L)).thenReturn(Optional.of(post));

            assertThat(postService.deletePost(10L, author)).isTrue();
            verify(postRepository).delete(post);
        }

        @Test
        @DisplayName("reports false without deleting for a non-author")
        void refusesNonOwner() {
            when(postRepository.findById(10L)).thenReturn(Optional.of(post));

            assertThat(postService.deletePost(10L, stranger)).isFalse();
            verify(postRepository, never()).delete(any());
        }

        @Test
        @DisplayName("reports false when the post does not exist")
        void reportsFalseWhenMissing() {
            when(postRepository.findById(99L)).thenReturn(Optional.empty());

            assertThat(postService.deletePost(99L, author)).isFalse();
            verify(postRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("pagination")
    class Pagination {

        @Test
        @DisplayName("maps a Page into the PaginationDto envelope")
        void mapsPageMetadata() {
            Pageable pageable = PageRequest.of(1, 2);
            when(postRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(post), pageable, 5));

            PaginationDto<PostResponseDto> result = postService.getAllPostsPaginated(pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getPageNumber()).isEqualTo(1);
            assertThat(result.getPageSize()).isEqualTo(2);
            assertThat(result.getTotalElements()).isEqualTo(5);
            assertThat(result.getTotalPages()).isEqualTo(3);
            assertThat(result.isHasNext()).isTrue();
            assertThat(result.isHasPrevious()).isTrue();
        }

        @Test
        @DisplayName("passes every filter field through to the repository query")
        void forwardsFilters() {
            Pageable pageable = PageRequest.of(0, 10);
            PostFilterDto filter = new PostFilterDto("title", "content", "text", "public", 1L);
            when(postRepository.findWithFilters("title", "content", "text", "public", 1L, pageable))
                    .thenReturn(new PageImpl<>(List.of(post), pageable, 1));

            PaginationDto<PostResponseDto> result = postService.getAllPostsWithFilters(filter, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            verify(postRepository).findWithFilters("title", "content", "text", "public", 1L, pageable);
        }

        @Test
        @DisplayName("scopes a user page to that user's id")
        void scopesUserPage() {
            Pageable pageable = PageRequest.of(0, 10);
            when(postRepository.findByCreatedById(1L, pageable))
                    .thenReturn(new PageImpl<>(List.of(post), pageable, 1));

            PaginationDto<PostResponseDto> result = postService.getUserPostsPaginated(1L, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.isHasNext()).isFalse();
        }
    }
}
