package com.jomariabejo.connectly_api.service;

import com.jomariabejo.connectly_api.exception.PostNotFoundException;
import com.jomariabejo.connectly_api.model.Like;
import com.jomariabejo.connectly_api.model.Post;
import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.repository.LikeRespository;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PostLikeService}. Pure Mockito -- no Spring context and no database.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PostLikeService")
class PostLikeServiceTest {

    @Mock
    private LikeRespository likeRespository;

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private PostLikeService postLikeService;

    private User user;
    private Post post;

    @BeforeEach
    void setUp() {
        user = new User("someone", "hashed", "someone@example.com");
        user.setId(1L);

        post = new Post();
        post.setId(10L);
        post.setPrivacy("public");
    }

    @Nested
    @DisplayName("togglePostLike")
    class Toggle {

        @Test
        @DisplayName("creates a like and reports true when none exists")
        void likesWhenNotYetLiked() {
            when(postRepository.findById(10L)).thenReturn(Optional.of(post));
            when(likeRespository.findByUserAndPost(user, post)).thenReturn(Optional.empty());

            assertThat(postLikeService.togglePostLike(10L, user)).isTrue();

            ArgumentCaptor<Like> saved = ArgumentCaptor.forClass(Like.class);
            verify(likeRespository).save(saved.capture());
            assertThat(saved.getValue().getUser()).isEqualTo(user);
            assertThat(saved.getValue().getPost()).isEqualTo(post);
            verify(likeRespository, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("removes the like and reports false when one exists")
        void unlikesWhenAlreadyLiked() {
            Like existing = new Like(user, post);
            existing.setId(77L);
            when(postRepository.findById(10L)).thenReturn(Optional.of(post));
            when(likeRespository.findByUserAndPost(user, post)).thenReturn(Optional.of(existing));

            assertThat(postLikeService.togglePostLike(10L, user)).isFalse();

            verify(likeRespository).deleteById(77L);
            verify(likeRespository, never()).save(any());
        }

        @Test
        @DisplayName("throws PostNotFoundException for an unknown post")
        void throwsWhenPostMissing() {
            when(postRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> postLikeService.togglePostLike(99L, user))
                    .isInstanceOf(PostNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    @Nested
    @DisplayName("createPostLike")
    class CreateLike {

        @Test
        @DisplayName("saves a like and reports true the first time")
        void createsLike() {
            when(postRepository.findById(10L)).thenReturn(Optional.of(post));
            when(likeRespository.findByUserAndPost(user, post)).thenReturn(Optional.empty());

            assertThat(postLikeService.createPostLike(10L, user)).isTrue();
            verify(likeRespository).save(any(Like.class));
        }

        @Test
        @DisplayName("reports false without saving when the user already liked the post")
        void isIdempotent() {
            when(postRepository.findById(10L)).thenReturn(Optional.of(post));
            when(likeRespository.findByUserAndPost(user, post)).thenReturn(Optional.of(new Like(user, post)));

            assertThat(postLikeService.createPostLike(10L, user)).isFalse();
            verify(likeRespository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("countLikesByPost")
    class CountLikes {

        @Test
        @DisplayName("counts the likes on a public post")
        void countsPublicPost() {
            when(postRepository.findById(10L)).thenReturn(Optional.of(post));
            when(likeRespository.countByPost(post)).thenReturn(3L);

            assertThat(postLikeService.countLikesByPost(10L)).isEqualTo(3L);
        }

        @Test
        @DisplayName("throws PostNotFoundException for a private post rather than revealing the count")
        void refusesPrivatePost() {
            post.setPrivacy("private");
            when(postRepository.findById(10L)).thenReturn(Optional.of(post));

            assertThatThrownBy(() -> postLikeService.countLikesByPost(10L))
                    .isInstanceOf(PostNotFoundException.class);
        }

        @Test
        @DisplayName("throws PostNotFoundException for an unknown post")
        void throwsWhenPostMissing() {
            when(postRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> postLikeService.countLikesByPost(99L))
                    .isInstanceOf(PostNotFoundException.class);
        }

        @Test
        @DisplayName("throws PostNotFoundException, not NPE, when the post's privacy is null")
        void treatsNullPrivacyAsHidden() {
            // Regression guard: the check used to read privacy.equals("public"), which threw a
            // NullPointerException for a post with no privacy set. Flipping it to
            // "public".equals(privacy) makes a null-privacy post behave like any non-public one.
            post.setPrivacy(null);
            when(postRepository.findById(10L)).thenReturn(Optional.of(post));

            assertThatThrownBy(() -> postLikeService.countLikesByPost(10L))
                    .isInstanceOf(PostNotFoundException.class);
            verify(likeRespository, never()).countByPost(any());
        }
    }

    @Nested
    @DisplayName("isPostLikedByUser")
    class IsLiked {

        @Test
        @DisplayName("throws PostNotFoundException for an unknown post")
        void throwsWhenPostMissing() {
            when(postRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> postLikeService.isPostLikedByUser(99L, user))
                    .isInstanceOf(PostNotFoundException.class)
                    .hasMessageContaining("99");
            verify(likeRespository, never()).findByUserAndPost(any(), any());
        }

        @Test
        @DisplayName("reports true when the user's like exists")
        void trueWhenLiked() {
            when(postRepository.findById(10L)).thenReturn(Optional.of(post));
            when(likeRespository.findByUserAndPost(user, post)).thenReturn(Optional.of(new Like(user, post)));

            assertThat(postLikeService.isPostLikedByUser(10L, user)).isTrue();
        }

        @Test
        @DisplayName("reports false when the user has not liked the post")
        void falseWhenNotLiked() {
            when(postRepository.findById(10L)).thenReturn(Optional.of(post));
            when(likeRespository.findByUserAndPost(user, post)).thenReturn(Optional.empty());

            assertThat(postLikeService.isPostLikedByUser(10L, user)).isFalse();
        }
    }

    @Nested
    @DisplayName("getLikesByUser")
    class LikesByUser {

        @Test
        @DisplayName("returns every like the user has made")
        void returnsUserLikes() {
            Like like = new Like(user, post);
            when(likeRespository.findAllByUser(user)).thenReturn(List.of(like));

            assertThat(postLikeService.getLikesByUser(user)).containsExactly(like);
        }
    }
}
