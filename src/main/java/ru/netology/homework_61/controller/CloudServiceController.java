package ru.netology.homework_61.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.netology.homework_61.service.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
    ) throws IOException, CloudServiceException {

        service.uploadFile(authToken, fileName, file);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("/file")
    public ResponseEntity<Object> deleteFile(
            @RequestHeader("auth-token") String authToken,
            @RequestParam("filename") String fileName
    ) throws IOException, CloudServiceException {

        service.deleteFile(authToken, fileName);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/file")
    public ResponseEntity<Object> downloadFile(
            @RequestHeader("auth-token") String authToken,
            @RequestParam("filename") String fileName
    ) throws CloudServiceException {

        var file = service.downloadFile(authToken, fileName);

        return ResponseEntity.ok()
                .body(file);
    }

    @PutMapping("/file")
    public ResponseEntity<Object> renameFile(
            @RequestHeader("auth-token") String authToken,
            @RequestParam("filename") String fileName,
            @RequestBody FileRenameRequestBody body
    ) throws CloudServiceException {

        service.renameFile(authToken, fileName, body.getNewName());

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/list")
    public ResponseEntity<List<FilesListResponseElement>> getAllFiles(
            @RequestHeader("auth-token") String authToken,
            @RequestParam("limit") int limit
    ) throws CloudServiceException {

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
    }

    @ExceptionHandler(AuthorizationException.class)
    public ResponseEntity<ErrorResponse> respondWithUnauthorized(AuthorizationException e) {
        return new ResponseEntity<>(
                new ErrorResponse("Unauthorized"),
                HttpStatus.UNAUTHORIZED
        );
    }

    @ExceptionHandler(FileAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> fileAlreadyExists(FileAlreadyExistsException e) {
        return new ResponseEntity<>(
                new ErrorResponse(String.format("File '%s' already exists", e.getFileName())),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<ErrorResponse> fileNotFound(FileNotFoundException e) {
        return new ResponseEntity<>(
                new ErrorResponse(String.format("File '%s' does not exist", e.getFileName())),
                HttpStatus.BAD_REQUEST
        );
    }
}
