package ru.netology.homework_61.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.netology.homework_61.service.*;

import java.io.IOException;
import java.util.ArrayList;

@Controller
@RequestMapping
public class CloudServiceController {
    private final CloudServiceService service;

    public CloudServiceController(CloudServiceService service) {
        this.service = service;
    }

    // It is convenient for testing to add new users via API because of passwords hashing.
    @PostMapping("/registration")
    public ResponseEntity<Object> registration(@RequestBody UserCredentials credentials) {
        try {
            service.registerNewUser(credentials.getLogin(), credentials.getPassword());

            return new ResponseEntity<>(HttpStatus.OK);
        } catch (UserAlreadyExistsException e) {
            return new ResponseEntity<>(HttpStatus.ALREADY_REPORTED);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<SuccessfulLoginResponse> login(@RequestBody UserCredentials credentials) {
        try {
            var token = service.login(credentials.getLogin(), credentials.getPassword());

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
        service.logout(authToken);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/file")
    public ResponseEntity<Object> uploadFile(
            @RequestHeader("auth-token") String authToken,
            @RequestParam("filename") String fileName,
            @RequestPart("file") MultipartFile file
    ) throws IOException {
        try {
            service.uploadFile(authToken, fileName, file);

            return new ResponseEntity<>(HttpStatus.OK);
        } catch (AuthorizationException e) {
            return new ResponseEntity<>(
                    new ErrorResponse("Unauthorized"),
                    HttpStatus.UNAUTHORIZED
            );
        }
    }

    @DeleteMapping("/file")
    public ResponseEntity<Object> deleteFile(@RequestHeader("auth-token") String authToken, @RequestParam("filename") String fileName) throws IOException {
        try {
            service.deleteFile(authToken, fileName);

            return new ResponseEntity<>(HttpStatus.OK);
        } catch (FileNotFoundException e) {
            return new ResponseEntity<>(
                    new ErrorResponse(String.format("File '%s' does not exist", e.getFileName())),
                    HttpStatus.BAD_REQUEST
            );
        } catch (AuthorizationException e) {
            return new ResponseEntity<>(
                    new ErrorResponse("Unauthorized"),
                    HttpStatus.UNAUTHORIZED
            );
        }
    }

    @GetMapping("/file")
    public ResponseEntity<Object> downloadFile(@RequestHeader("auth-token") String authToken, @RequestParam("filename") String fileName) throws Exception {
        try {
            var file = service.downloadFile(authToken, fileName);

            return ResponseEntity.ok()
                    .body(file);
        } catch (AuthorizationException e) {
            return new ResponseEntity<>(
                    new ErrorResponse("Unauthorized"),
                    HttpStatus.UNAUTHORIZED
            );
        } catch (FileNotFoundException e) {
            return new ResponseEntity<>(
                    new ErrorResponse(String.format("File '%s' does not exist", e.getFileName())),
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    @PutMapping("/file")
    public ResponseEntity<Object> renameFile(
            @RequestHeader("auth-token") String authToken,
            @RequestParam("filename") String fileName,
            @RequestBody FileRenameRequestBody body
    ) {
        try {
            service.renameFile(authToken, fileName, body.getNewName());
        } catch (AuthorizationException e) {
            return new ResponseEntity<>(
                    new ErrorResponse("Unauthorized"),
                    HttpStatus.UNAUTHORIZED
            );
        } catch (FileNotFoundException e) {
            return new ResponseEntity<>(
                    new ErrorResponse(String.format("File '%s' does not exist", e.getFileName())),
                    HttpStatus.BAD_REQUEST
            );
        } catch (FileAlreadyExistsException e) {
            return new ResponseEntity<>(
                    new ErrorResponse(String.format("File '%s' already exists", e.getFileName())),
                    HttpStatus.BAD_REQUEST
            );
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/list")
    public ResponseEntity<Object> getAllFiles(@RequestHeader("auth-token") String authToken, @RequestParam("limit") int limit) {
        try {
            var filesData = service.getAllFiles(authToken, limit);

            var res = new ArrayList<FilesListResponseElement>(filesData.size());

            for (var element : filesData) {
                res.add(
                        new FilesListResponseElement(
                                element.getName(),
                                element.getSize()
                        )
                );
            }

            return new ResponseEntity<>(res, HttpStatus.OK);
        } catch (AuthorizationException e) {
            return new ResponseEntity<>(
                    new ErrorResponse("Unauthorized"),
                    HttpStatus.UNAUTHORIZED
            );
        }
    }
}
