package com.jomariabejo.connectly_api.controller;

import com.jomariabejo.connectly_api.exception.PostNotFoundException;
import com.jomariabejo.connectly_api.model.Like;
import com.jomariabejo.connectly_api.model.Post;
import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.service.AuthenticationService;
import com.jomariabejo.connectly_api.service.PostLikeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller slice tests for {@link PostLikeController}.
 *
 * <p>{@code toggleLikeUsesSingleSegmentPath} is a regression guard. The toggle method used to
 * declare {@code @PostMapping("/{postId}/likes/toggle")} under a class-level
 * {@code @RequestMapping("/{postId}")}; the two concatenated into
 * {@code /{postId}/{postId}/likes/toggle}, and Spring Boot 3's {@code PathPatternParser} rejects
 * capturing the same variable twice -- which aborted application startup entirely. Reintroducing a
 * duplicate segment fails this class at context load, not at assertion time.
 *
 * <p>See {@link ControllerSliceTest} for what the slice does and does not cover.
 */
@ControllerSliceTest(PostLikeController.class)
@DisplayName("PostLikeController")
class PostLikeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PostLikeService postLikeService;

    @MockitoBean
    private AuthenticationService authenticationService;

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

    @Test
    @DisplayName("POST /{postId}/likes/toggle reports true when the post becomes liked")
    void togglesLikeOn() throws Exception {
        when(authenticationService.getAuthenticatedUser()).thenReturn(user);
        when(postLikeService.togglePostLike(10L, user)).thenReturn(true);

        mockMvc.perform(post("/10/likes/toggle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Post liked successfully."))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @DisplayName("POST /{postId}/likes/toggle reports false when the like is removed")
    void togglesLikeOff() throws Exception {
        when(authenticationService.getAuthenticatedUser()).thenReturn(user);
        when(postLikeService.togglePostLike(10L, user)).thenReturn(false);

        mockMvc.perform(post("/10/likes/toggle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Post unliked successfully."))
                .andExpect(jsonPath("$.data").value(false));
    }

    @Test
    @DisplayName("the toggle route takes {postId} exactly once -- the old doubled URL is gone")
    void toggleLikeUsesSingleSegmentPath() throws Exception {
        when(authenticationService.getAuthenticatedUser()).thenReturn(user);
        when(postLikeService.togglePostLike(10L, user)).thenReturn(true);

        mockMvc.perform(post("/10/likes/toggle")).andExpect(status().isOk());

        // The doubled path must not reach a handler. It now answers a clean 404 -- the handler for
        // Spring's own NoResourceFoundException preserves that status instead of letting the
        // catch-all report 500.
        mockMvc.perform(post("/10/10/likes/toggle"))
                .andExpect(status().isNotFound());
        verify(postLikeService, times(1)).togglePostLike(anyLong(), any(User.class));
    }

    @Test
    @DisplayName("POST /{postId}/likes/toggle answers 404 for an unknown post")
    void reportsMissingPostOnToggle() throws Exception {
        when(authenticationService.getAuthenticatedUser()).thenReturn(user);
        when(postLikeService.togglePostLike(anyLong(), any(User.class)))
                .thenThrow(new PostNotFoundException(99L));

        mockMvc.perform(post("/99/likes/toggle"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Post not found"));
    }

    @Test
    @DisplayName("GET /{postId}/likes/count returns the tally")
    void countsLikes() throws Exception {
        when(postLikeService.countLikesByPost(10L)).thenReturn(3L);

        mockMvc.perform(get("/10/likes/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Total likes retrieved."))
                .andExpect(jsonPath("$.data").value(3));
    }

    @Test
    @DisplayName("GET /{postId}/likes/count answers 400 for a non-numeric postId")
    void rejectsNonNumericPostId() throws Exception {
        // "abc" cannot bind to the Long {postId}, so Spring raises a type-mismatch error before
        // the handler runs. The advice maps it to 400 -- without that mapping the catch-all used
        // to report the typo as a 500.
        mockMvc.perform(get("/abc/likes/count"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
        verifyNoInteractions(postLikeService);
    }

    @Test
    @DisplayName("GET /{postId}/likes/count answers 404 for a private or unknown post")
    void refusesCountForHiddenPost() throws Exception {
        when(postLikeService.countLikesByPost(anyLong())).thenThrow(new PostNotFoundException(99L));

        mockMvc.perform(get("/99/likes/count"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /{postId}/likes/my-likes returns every like by the caller, ignoring {postId}")
    void listsOwnLikes() throws Exception {
        Like like = new Like(user, post);
        like.setId(77L);

        when(authenticationService.getAuthenticatedUser()).thenReturn(user);
        when(postLikeService.getLikesByUser(user)).thenReturn(List.of(like));

        // The {postId} in the path is inherited from the class-level mapping and never read, so an
        // unrelated value still returns the caller's full like list.
        mockMvc.perform(get("/999/likes/my-likes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User likes retrieved successfully."))
                .andExpect(jsonPath("$.data[0].id").value(77))
                .andExpect(jsonPath("$.data[0].postId").value(10));
    }
}
