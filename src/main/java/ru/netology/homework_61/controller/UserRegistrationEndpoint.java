package ru.netology.homework_61.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.netology.homework_61.service.UserAlreadyExistsException;
import ru.netology.homework_61.service.UserManagementService;

@Controller
@RequestMapping
@Profile("test")
public class UserRegistrationEndpoint {
    private final UserManagementService umService;

    public UserRegistrationEndpoint(UserManagementService umService) {
        this.umService = umService;
    }

    // It is convenient for testing to add new users via API because of passwords hashing.
    @PostMapping("/registration")
    public ResponseEntity<Object> registration(@RequestBody UserCredentials credentials) {
        try {
            umService.registerNewUser(credentials.getLogin(), credentials.getPassword());

            return new ResponseEntity<>(HttpStatus.OK);
        } catch (UserAlreadyExistsException e) {
            return new ResponseEntity<>(HttpStatus.ALREADY_REPORTED);
        }
    }


}
