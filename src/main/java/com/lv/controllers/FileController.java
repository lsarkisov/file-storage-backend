package com.lv.controllers;

import com.lv.dto.UploadFileResponse;
import com.lv.services.DBFileStorageService;
import com.lv.models.DBFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/file")
@CrossOrigin
public class FileController {

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    @Autowired
    private DBFileStorageService DBFileStorageService;

    @GetMapping("/all/files")
    public List<UploadFileResponse> fetchAllFiles() {
        return DBFileStorageService.getAllFiles();
    }

    @PostMapping("/upload")
    public List<UploadFileResponse> uploadFile(@RequestParam("file") MultipartFile file) {
        DBFile dbFile = DBFileStorageService.storeFile(file);

        new UploadFileResponse(dbFile.getId(), dbFile.getFileName(), DBFileStorageService.getFileUri(dbFile.getId()),
                file.getContentType(), file.getSize());
        return DBFileStorageService.getAllFiles();
    }

    @PostMapping("/upload/multiple")
    public List<UploadFileResponse> uploadMultipleFiles(@RequestParam("files") MultipartFile[] files) {
        Arrays.asList(files)
                .stream()
                .map(file -> uploadFile(file))
                .collect(Collectors.toList());

        return fetchAllFiles();
    }

    @PutMapping("/update")
    public List<UploadFileResponse> updateFile(@RequestParam("fileId") String  id, @RequestParam("newName") String  newName) {
        return DBFileStorageService.updateFile(id, newName);
    }

    @DeleteMapping("/delete")
    public List<UploadFileResponse> deleteFile(@RequestParam("fileId") String id) {
        return DBFileStorageService.removeFile(id);
    }

}

