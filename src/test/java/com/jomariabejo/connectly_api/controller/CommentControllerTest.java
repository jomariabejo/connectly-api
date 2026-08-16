package com.jomariabejo.connectly_api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jomariabejo.connectly_api.dto.CommentFilterDto;
import com.jomariabejo.connectly_api.dto.PaginationDto;
import com.jomariabejo.connectly_api.dto.comment.CommentResponseDto;
import com.jomariabejo.connectly_api.dto.comment.CreateCommentDto;
import com.jomariabejo.connectly_api.dto.comment.UpdateCommentDto;
import com.jomariabejo.connectly_api.exception.CommentNotFoundException;
import com.jomariabejo.connectly_api.exception.PostNotFoundException;
import com.jomariabejo.connectly_api.exception.UnauthorizedAccessException;
import com.jomariabejo.connectly_api.model.Comment;
import com.jomariabejo.connectly_api.model.Post;
import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.service.AuthenticationService;
import com.jomariabejo.connectly_api.service.CommentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller slice tests for {@link CommentController}, whose routes are nested under
 * {@code /posts/{postId}/comments}.
 *
 * <p>See {@link ControllerSliceTest} for what the slice does and does not cover.
 */
@ControllerSliceTest(CommentController.class)
@DisplayName("CommentController")
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CommentService commentService;

    @MockitoBean
    private AuthenticationService authenticationService;

    private User author;
    private Comment comment;

    @BeforeEach
    void setUp() {
        author = new User("author", "hashed", "author@example.com");
        author.setId(1L);

        Post post = new Post();
        post.setId(10L);

        comment = new Comment();
        comment.setId(100L);
        comment.setText("Nice post!");
        comment.setPost(post);
        comment.setUser(author);

        when(authenticationService.getAuthenticatedUser()).thenReturn(author);
    }

    @Test
    @DisplayName("POST /posts/{postId}/comments answers 201 with the created comment")
    void addsComment() throws Exception {
        CreateCommentDto request = new CreateCommentDto();
        request.setText("Nice post!");
        when(commentService.addCommentToPost(eq(10L), any(CreateCommentDto.class), eq(author)))
                .thenReturn(request);

        mockMvc.perform(post("/posts/10/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value("Nice post!"));
    }

    @Test
    @DisplayName("POST /posts/{postId}/comments answers 404 when the post does not exist")
    void reportsMissingPost() throws Exception {
        when(commentService.addCommentToPost(anyLong(), any(CreateCommentDto.class), any(User.class)))
                .thenThrow(new PostNotFoundException(99L));

        mockMvc.perform(post("/posts/99/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Nice post!\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Post not found"));
    }

    @Test
    @DisplayName("GET /posts/{postId}/comments/{commentId} returns the comment")
    void returnsComment() throws Exception {
        when(commentService.getComment(10L, 100L, author)).thenReturn(new CommentResponseDto(comment));

        mockMvc.perform(get("/posts/10/comments/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.text").value("Nice post!"));
    }

    @Test
    @DisplayName("GET /posts/{postId}/comments/{commentId} answers 404 for an unknown comment")
    void reportsMissingComment() throws Exception {
        when(commentService.getComment(anyLong(), anyLong(), any(User.class)))
                .thenThrow(new CommentNotFoundException("Comment not found with id: 404"));

        mockMvc.perform(get("/posts/10/comments/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Comment not found"));
    }

    @Test
    @DisplayName("PUT /posts/{postId}/comments/{commentId} returns the edited comment")
    void updatesComment() throws Exception {
        UpdateCommentDto request = new UpdateCommentDto();
        request.setText("Edited");

        comment.setText("Edited");
        when(commentService.updateComment(eq(100L), any(UpdateCommentDto.class), eq(author)))
                .thenReturn(new CommentResponseDto(comment));

        mockMvc.perform(put("/posts/10/comments/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Edited"));
    }

    @Test
    @DisplayName("PUT /posts/{postId}/comments/{commentId} answers 403 for a non-author")
    void rejectsEditFromNonAuthor() throws Exception {
        when(commentService.updateComment(anyLong(), any(UpdateCommentDto.class), any(User.class)))
                .thenThrow(new UnauthorizedAccessException("You are not authorized to update this comment."));

        mockMvc.perform(put("/posts/10/comments/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Edited\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("DELETE /posts/{postId}/comments/{commentId} answers 204")
    void deletesComment() throws Exception {
        mockMvc.perform(delete("/posts/10/comments/100"))
                .andExpect(status().isNoContent());

        verify(commentService).deleteComment(10L, 100L, author);
    }

    @Test
    @DisplayName("DELETE /posts/{postId}/comments/{commentId} answers 403 for a non-author")
    void rejectsDeleteFromNonAuthor() throws Exception {
        org.mockito.Mockito.doThrow(new UnauthorizedAccessException("You are not authorized to delete this comment."))
                .when(commentService).deleteComment(anyLong(), anyLong(), any(User.class));

        mockMvc.perform(delete("/posts/10/comments/100"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /posts/{postId}/comments applies the createdAt,desc page-10 default")
    void appliesPageableDefaults() throws Exception {
        when(commentService.getPostCommentsPaginated(eq(10L), any(Pageable.class)))
                .thenReturn(new PaginationDto<>(List.of(new CommentResponseDto(comment)), 0, 10, 1, 1));

        mockMvc.perform(get("/posts/10/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(100))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.hasNext").value(false));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(commentService).getPostCommentsPaginated(eq(10L), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(10);
        assertThat(pageable.getValue().getSort().getOrderFor("createdAt"))
                .isNotNull()
                .extracting(Sort.Order::getDirection)
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    @DisplayName("GET /posts/{postId}/comments switches to the filtered query when a filter is supplied")
    void switchesToFilteredQuery() throws Exception {
        when(commentService.getPostCommentsWithFilters(eq(10L), any(CommentFilterDto.class), any(Pageable.class)))
                .thenReturn(new PaginationDto<>(List.of(new CommentResponseDto(comment)), 0, 10, 1, 1));

        mockMvc.perform(get("/posts/10/comments").param("content", "Nice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        ArgumentCaptor<CommentFilterDto> filter = ArgumentCaptor.forClass(CommentFilterDto.class);
        verify(commentService).getPostCommentsWithFilters(eq(10L), filter.capture(), any(Pageable.class));
        assertThat(filter.getValue().getContent()).isEqualTo("Nice");
        verify(commentService, never()).getPostCommentsPaginated(anyLong(), any());
    }

    @Test
    @DisplayName("POST /posts/{postId}/comments answers 400 with a Validation failed body when the text is blank")
    void rejectsBlankTextOnCreate() throws Exception {
        mockMvc.perform(post("/posts/10/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.message").value("text: Comment is required"));

        verify(commentService, never()).addCommentToPost(anyLong(), any(CreateCommentDto.class), any(User.class));
    }

    @Test
    @DisplayName("PUT /posts/{postId}/comments/{commentId} answers 400 when the text is blank")
    void rejectsBlankTextOnUpdate() throws Exception {
        // UpdateCommentDto's @NotBlank carries no custom message, and the default one is
        // locale-dependent -- so only the error title is pinned here, not the detail text.
        mockMvc.perform(put("/posts/10/comments/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"));

        verify(commentService, never()).updateComment(anyLong(), any(UpdateCommentDto.class), any(User.class));
    }

    // The four tests below drive GlobalExceptionHandler.handleSpringErrorResponse through real
    // routes: Spring raises the exception, the advice reads the status back off it (or defaults
    // to 400) and answers with the status's reason phrase as the error title.

    @Test
    @DisplayName("POST /posts/{postId}/comments answers 400 for a malformed JSON body")
    void rejectsMalformedJsonBody() throws Exception {
        mockMvc.perform(post("/posts/10/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));

        verify(commentService, never()).addCommentToPost(anyLong(), any(CreateCommentDto.class), any(User.class));
    }

    @Test
    @DisplayName("POST /posts/{postId}/comments answers 400 when postId is not numeric")
    void rejectsNonNumericPostId() throws Exception {
        mockMvc.perform(post("/posts/abc/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Nice post!\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verify(commentService, never()).addCommentToPost(anyLong(), any(CreateCommentDto.class), any(User.class));
    }

    @Test
    @DisplayName("POST /posts/{postId}/comments answers 415 for a text/plain body")
    void rejectsUnsupportedContentType() throws Exception {
        mockMvc.perform(post("/posts/10/comments")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("Nice post!"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.error").value("Unsupported Media Type"));

        verify(commentService, never()).addCommentToPost(anyLong(), any(CreateCommentDto.class), any(User.class));
    }

    @Test
    @DisplayName("PATCH /posts/{postId}/comments answers 405 -- the collection route only supports GET and POST")
    void rejectsUnsupportedMethod() throws Exception {
        mockMvc.perform(patch("/posts/10/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Nice post!\"}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.error").value("Method Not Allowed"));
    }
}
