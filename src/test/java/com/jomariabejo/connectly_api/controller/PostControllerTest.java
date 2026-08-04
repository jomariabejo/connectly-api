package com.jomariabejo.connectly_api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jomariabejo.connectly_api.dto.PaginationDto;
import com.jomariabejo.connectly_api.dto.PostFilterDto;
import com.jomariabejo.connectly_api.dto.post.CreatePostDto;
import com.jomariabejo.connectly_api.dto.post.PostResponseDto;
import com.jomariabejo.connectly_api.dto.post.UpdatePostDto;
import com.jomariabejo.connectly_api.exception.UnauthorizedAccessException;
import com.jomariabejo.connectly_api.model.Post;
import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.service.AuthenticationService;
import com.jomariabejo.connectly_api.service.PostService;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller slice tests for {@link PostController}: routing, status codes, JSON shape and
 * {@code @PageableDefault} binding. The JWT filter chain is switched off -- see
 * {@link AuthenticationControllerTest} for the rationale.
 */
@ControllerSliceTest(PostController.class)
@DisplayName("PostController")
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PostService postService;

    @MockitoBean
    private AuthenticationService authenticationService;

    private User author;
    private Post post;

    @BeforeEach
    void setUp() {
        author = new User("author", "hashed", "author@example.com");
        author.setId(1L);

        post = new Post();
        post.setId(10L);
        post.setTitle("My First Post");
        post.setContent("This is my first post");
        post.setPostType("text");
        post.setPrivacy("public");
        post.setCreatedBy(author);

        when(authenticationService.getAuthenticatedUser()).thenReturn(author);
    }

    private CreatePostDto createRequest() {
        CreatePostDto dto = new CreatePostDto();
        dto.setTitle("My First Post");
        dto.setContent("This is my first post");
        dto.setPostType("text");
        dto.setPrivacy("public");
        return dto;
    }

    @Test
    @DisplayName("POST /posts answers 201 with the created post")
    void createsPost() throws Exception {
        CreatePostDto request = createRequest();
        when(postService.createPost(any(CreatePostDto.class), eq(author))).thenReturn(request);

        mockMvc.perform(post("/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("My First Post"))
                .andExpect(jsonPath("$.postType").value("text"));
    }

    @Test
    @DisplayName("POST /posts answers 400 when the title is shorter than 5 characters")
    void rejectsShortTitle() throws Exception {
        String invalid = objectMapper.writeValueAsString(Map.of(
                "title", "abc",
                "content", "This is my first post"));

        mockMvc.perform(post("/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.message").value("title: Title must be between 5 and 100 characters"));

        verify(postService, never()).createPost(any(), any());
    }

    @Test
    @DisplayName("GET /posts/{id} returns the post to its author")
    void returnsPost() throws Exception {
        when(postService.getPost(10L, author)).thenReturn(new PostResponseDto(post));

        mockMvc.perform(get("/posts/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.title").value("My First Post"));
    }

    @Test
    @DisplayName("GET /posts/{id} answers 403 when the service refuses -- missing and unowned look alike")
    void hidesMissingAndUnownedAlike() throws Exception {
        when(postService.getPost(anyLong(), any(User.class)))
                .thenThrow(new RuntimeException("Post with ID 99 not found"));

        mockMvc.perform(get("/posts/99"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /posts/{id} returns the updated post")
    void updatesPost() throws Exception {
        UpdatePostDto request = new UpdatePostDto();
        request.setTitle("Updated title");
        request.setContent("Updated content");

        post.setTitle("Updated title");
        when(postService.updatePost(eq(10L), any(UpdatePostDto.class), eq(author)))
                .thenReturn(new PostResponseDto(post));

        mockMvc.perform(put("/posts/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated title"));
    }

    @Test
    @DisplayName("PUT /posts/{id} answers 403 when the caller is not the author")
    void rejectsUpdateFromNonAuthor() throws Exception {
        when(postService.updatePost(anyLong(), any(UpdatePostDto.class), any(User.class)))
                .thenThrow(new UnauthorizedAccessException("not authorized"));

        mockMvc.perform(put("/posts/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated title\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("DELETE /posts/{id} answers 204 on success")
    void deletesPost() throws Exception {
        when(postService.deletePost(10L, author)).thenReturn(true);

        mockMvc.perform(delete("/posts/10"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /posts/{id} answers 403 when the service refuses")
    void refusesDelete() throws Exception {
        when(postService.deletePost(99L, author)).thenReturn(false);

        mockMvc.perform(delete("/posts/99"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /posts/my-posts returns the caller's posts unpaginated")
    void returnsOwnPosts() throws Exception {
        when(postService.getPostsByUser(1L)).thenReturn(List.of(new PostResponseDto(post)));

        mockMvc.perform(get("/posts/my-posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(10));
    }

    @Test
    @DisplayName("GET /posts applies the createdAt,desc page-10 default when no paging is supplied")
    void appliesPageableDefaults() throws Exception {
        when(postService.getAllPostsPaginated(any(Pageable.class)))
                .thenReturn(new PaginationDto<>(List.of(new PostResponseDto(post)), 0, 10, 1, 1));

        mockMvc.perform(get("/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.hasPrevious").value(false));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(postService).getAllPostsPaginated(pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(10);
        assertThat(pageable.getValue().getSort().getOrderFor("createdAt"))
                .isNotNull()
                .extracting(Sort.Order::getDirection)
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    @DisplayName("GET /posts binds explicit page, size and sort parameters")
    void bindsExplicitPaging() throws Exception {
        when(postService.getAllPostsPaginated(any(Pageable.class)))
                .thenReturn(new PaginationDto<>(List.of(), 1, 5, 12, 3));

        mockMvc.perform(get("/posts")
                        .param("page", "1")
                        .param("size", "5")
                        .param("sort", "title,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageNumber").value(1))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.hasPrevious").value(true));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(postService).getAllPostsPaginated(pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(5);
        assertThat(pageable.getValue().getSort().getOrderFor("title"))
                .isNotNull()
                .extracting(Sort.Order::getDirection)
                .isEqualTo(Sort.Direction.ASC);
    }

    @Test
    @DisplayName("GET /posts switches to the filtered query as soon as a filter is supplied")
    void switchesToFilteredQuery() throws Exception {
        when(postService.getAllPostsWithFilters(any(PostFilterDto.class), any(Pageable.class)))
                .thenReturn(new PaginationDto<>(List.of(new PostResponseDto(post)), 0, 10, 1, 1));

        mockMvc.perform(get("/posts").param("privacy", "public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        ArgumentCaptor<PostFilterDto> filter = ArgumentCaptor.forClass(PostFilterDto.class);
        verify(postService).getAllPostsWithFilters(filter.capture(), any(Pageable.class));
        assertThat(filter.getValue().getPrivacy()).isEqualTo("public");
        verify(postService, never()).getAllPostsPaginated(any());
    }

    @Test
    @DisplayName("GET /posts/my-posts/paginated scopes the page to the caller's id")
    void scopesOwnPageToCaller() throws Exception {
        when(postService.getUserPostsPaginated(eq(1L), any(Pageable.class)))
                .thenReturn(new PaginationDto<>(List.of(new PostResponseDto(post)), 0, 10, 1, 1));

        mockMvc.perform(get("/posts/my-posts/paginated"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10));

        verify(postService).getUserPostsPaginated(eq(1L), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /posts/user/{userId}/paginated scopes the page to the requested user")
    void scopesPageToRequestedUser() throws Exception {
        when(postService.getUserPostsPaginated(eq(7L), any(Pageable.class)))
                .thenReturn(new PaginationDto<>(List.of(), 0, 10, 0, 0));

        mockMvc.perform(get("/posts/user/7/paginated"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        verify(postService).getUserPostsPaginated(eq(7L), any(Pageable.class));
    }
}
