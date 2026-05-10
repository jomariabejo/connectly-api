package com.jomariabejo.connectly_api.e2e;

import com.jomariabejo.connectly_api.dto.LoginResponse;
import com.jomariabejo.connectly_api.dto.LoginUserDto;
import com.jomariabejo.connectly_api.dto.RegisterUserDto;
import com.jomariabejo.connectly_api.model.User;
import com.jomariabejo.connectly_api.repository.UserRepository;
import com.jomariabejo.connectly_api.service.EmailService;
import com.jomariabejo.connectly_api.support.JwtTestInitializer;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Optional;
import java.util.UUID;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = JwtTestInitializer.class)
public class UserAuthE2eTest {
    private static final String PASSWORD = "Password123!";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private EmailService emailService;

    @MockBean
    private JavaMailSender javaMailSender;

    @Test
    public void shouldCompleteRegistrationVerificationAndLoginFlow() {
        String uniqueId = UUID.randomUUID().toString();
        String email = "e2e-user-" + uniqueId + "@example.com";
        String username = "e2e-user-" + uniqueId;

        RegisterUserDto registerUserDto = new RegisterUserDto();
        registerUserDto.setEmail(email);
        registerUserDto.setUsername(username);
        registerUserDto.setPassword(PASSWORD);

        ResponseEntity<User> registrationResponse = restTemplate.postForEntity(
                "/auth/registration",
                registerUserDto,
                User.class
        );

        Assert.assertEquals(HttpStatus.OK, registrationResponse.getStatusCode());

        Optional<User> createdUser = userRepository.findByEmail(email);
        Assert.assertTrue(createdUser.isPresent());
        Assert.assertNotNull(createdUser.get().getVerificationToken());

        ResponseEntity<String> verifyResponse = restTemplate.getForEntity(
                "/auth/verify?token=" + createdUser.get().getVerificationToken(),
                String.class
        );

        Assert.assertEquals(HttpStatus.OK, verifyResponse.getStatusCode());

        User verifiedUser = userRepository.findByEmail(email).orElseThrow();
        Assert.assertTrue(verifiedUser.isEnabled());

        LoginUserDto loginUserDto = new LoginUserDto();
        loginUserDto.setEmail(email);
        loginUserDto.setPassword(PASSWORD);

        ResponseEntity<LoginResponse> loginResponse = restTemplate.postForEntity(
                "/auth/login",
                loginUserDto,
                LoginResponse.class
        );

        Assert.assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        Assert.assertNotNull(loginResponse.getBody());
        Assert.assertNotNull(loginResponse.getBody().getToken());
    }
}
