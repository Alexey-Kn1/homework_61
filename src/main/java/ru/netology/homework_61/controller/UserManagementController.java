package ru.netology.homework_61.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.netology.homework_61.service.PasswordMismatchException;
import ru.netology.homework_61.service.UserAlreadyExistsException;
import ru.netology.homework_61.service.UserManagementService;
import ru.netology.homework_61.service.UserNotFoundException;

@Controller
@RequestMapping
public class UserManagementController {
    private final UserManagementService umService;

    public UserManagementController(UserManagementService umService) {
        this.umService = umService;
    }

    @PostMapping("/login")
    public ResponseEntity<SuccessfulLoginResponse> login(@RequestBody UserCredentials credentials) {
        try {
            var token = umService.login(credentials.getLogin(), credentials.getPassword());

            return new ResponseEntity<>(
                    new SuccessfulLoginResponse(token),
                    HttpStatus.OK
            );
        } catch (UserNotFoundException | PasswordMismatchException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Object> logout(@RequestHeader("auth-token") String authToken) {
        umService.logout(authToken);

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
