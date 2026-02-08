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
public class FilesController {
    private final FilesService filesService;

    public FilesController(FilesService filesService) {
        this.filesService = filesService;
    }

    @PostMapping("/file")
    public ResponseEntity<Object> uploadFile(
            @RequestHeader("auth-token") String authToken,
            @RequestParam("filename") String fileName,
            @RequestPart("file") MultipartFile file
    ) throws IOException, CloudServiceException {

        filesService.uploadFile(authToken, fileName, file);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("/file")
    public ResponseEntity<Object> deleteFile(
            @RequestHeader("auth-token") String authToken,
            @RequestParam("filename") String fileName
    ) throws IOException, CloudServiceException {

        filesService.deleteFile(authToken, fileName);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/file")
    public ResponseEntity<Object> downloadFile(
            @RequestHeader("auth-token") String authToken,
            @RequestParam("filename") String fileName
    ) throws CloudServiceException {

        var file = filesService.downloadFile(authToken, fileName);

        return ResponseEntity.ok()
                .body(file);
    }

    @PutMapping("/file")
    public ResponseEntity<Object> renameFile(
            @RequestHeader("auth-token") String authToken,
            @RequestParam("filename") String fileName,
            @RequestBody FileRenameRequestBody body
    ) throws CloudServiceException {

        filesService.renameFile(authToken, fileName, body.getNewName());

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/list")
    public ResponseEntity<List<FilesListResponseElement>> getAllFiles(
            @RequestHeader("auth-token") String authToken,
            @RequestParam("limit") int limit
    ) throws CloudServiceException {

        var filesData = filesService.getAllFiles(authToken, limit);

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
