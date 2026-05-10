package com.jomariabejo.connectly_api.controller;

import com.jomariabejo.connectly_api.dto.PaginationDto;
import com.jomariabejo.connectly_api.dto.UserFilterDto;
import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.service.AuthenticationService;
import com.jomariabejo.connectly_api.service.UserService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequestMapping("/users")
@RestController
public class UserController {
    private final UserService userService;
    private final AuthenticationService authenticationService;

    public UserController(UserService userService, AuthenticationService authenticationService) {
        this.userService = userService;
        this.authenticationService = authenticationService;
    }

    @GetMapping("/me")
    public ResponseEntity<User> authenticatedUser() {
        User currentUser = authenticationService.getAuthenticatedUser();
        return ResponseEntity.ok(currentUser);
    }

    @GetMapping("/")
    public ResponseEntity<List<User>> allUsers() {
        List<User> users = userService.allUsers();

        return ResponseEntity.ok(users);
    }

    // Pagination endpoints
    @GetMapping("/paginated")
    public ResponseEntity<PaginationDto<User>> getAllUsersPaginated(
            @PageableDefault(size = 10, page = 0, sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName) {

        UserFilterDto filterDto = new UserFilterDto(username, email, firstName, lastName);
        PaginationDto<User> result;

        if (hasFilters(filterDto)) {
            result = userService.getAllUsersWithFilters(filterDto, pageable);
        } else {
            result = userService.getAllUsersPaginated(pageable);
        }

        return ResponseEntity.ok(result);
    }

    private boolean hasFilters(UserFilterDto filterDto) {
        return filterDto.getUsername() != null || filterDto.getEmail() != null ||
               filterDto.getFirstName() != null || filterDto.getLastName() != null;
    }
}