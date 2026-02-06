package ru.netology.homework_61.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.netology.homework_61.service.CloudServiceService;
import ru.netology.homework_61.service.UserCredentials;

import java.io.IOException;

@Controller
@RequestMapping
public class CloudServiceController {
    private final CloudServiceService service;

    public CloudServiceController(CloudServiceService service) {
        this.service = service;
    }

//    // It is convenient for testing to add new users via API because of passwords hashing.
//    @PostMapping("/registration")
//    public ResponseEntity<Object> registration(@RequestBody UserCredentials credentials) {
//        return service.registerNewUser(credentials);
//    }

    @PostMapping("/login")
    public ResponseEntity<Object> login(@RequestBody UserCredentials credentials) {
        return service.login(credentials);
    }

    @PostMapping("/logout")
    public ResponseEntity<Object> logout(@RequestHeader("auth-token") String authToken) {
        return service.logout(authToken);
    }

    @PostMapping("/file")
    public ResponseEntity<Object> uploadFile(
            @RequestHeader("auth-token") String authToken,
            @RequestParam("filename") String fileName,
            @RequestPart("file") MultipartFile file
    ) throws IOException {
        return service.uploadFile(authToken, fileName, file);
    }

    @DeleteMapping("/file")
    public ResponseEntity<Object> deleteFile(@RequestHeader("auth-token") String authToken, @RequestParam("filename") String fileName) throws IOException {
        return service.deleteFile(authToken, fileName);
    }

    @GetMapping("/file")
    public ResponseEntity<Object> downloadFile(@RequestHeader("auth-token") String authToken, @RequestParam("filename") String fileName) throws Exception {
        return service.downloadFile(authToken, fileName);
    }

    @PutMapping("/file")
    public ResponseEntity<Object> renameFile(
            @RequestHeader("auth-token") String authToken,
            @RequestParam("filename") String fileName,
            @RequestBody FileRenameRequestBody body
    ) throws IOException {
        return service.renameFile(authToken, fileName, body.getNewName());
    }

    @GetMapping("/list")
    public ResponseEntity<Object> getAllFiles(@RequestHeader("auth-token") String authToken, @RequestParam("limit") int limit) {
        return service.getAllFiles(authToken, limit);
    }
}
