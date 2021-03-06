package com.lv.services;

import com.lv.dto.UploadFileResponse;
import com.lv.exceptions.FileStorageException;
import com.lv.exceptions.MyFileNotFoundException;
import com.lv.models.DBFile;
import com.lv.repository.DBFileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DBFileStorageService {

    @Autowired
    private DBFileRepository dbFileRepository;

    @Autowired
    private FileStorageService fileStorageService;

    public DBFile storeFile(MultipartFile file) {
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());

        try {
            if(fileName.contains("..")) {
                throw new FileStorageException("Sorry! Filename contains invalid path sequence " + fileName);
            }

            DBFile dbFile = new DBFile(fileName, file.getContentType(), file.getBytes().length);

            // Store file in dir "/upload"
            fileStorageService.storeFile(file);

            return dbFileRepository.save(dbFile);
        } catch (IOException ex) {
            throw new FileStorageException("Could not store file " + fileName + ". Please try again!", ex);
        }
    }

    public DBFile getFile(String fileId) {
        return dbFileRepository.findById(fileId)
                .orElseThrow(() -> new MyFileNotFoundException("File not found with id " + fileId));
    }

    public List<UploadFileResponse> getAllFiles() {
        List<UploadFileResponse> dbFiles = new ArrayList<>();

        return dbFileRepository.findAll().stream()
                .map(file -> {
                    UploadFileResponse uploadFileResponse = new UploadFileResponse(
                            file.getId(), file.getFileName(), getFileUri(file.getId()),
                            file.getFileType(), file.getSize());
                    dbFiles.add(uploadFileResponse);
                    return uploadFileResponse;
                })
                .collect(Collectors.toList());
    }

    public String getFileUri(String fileId) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/download/")
                .path(fileId)
                .toUriString();
    }

    public List<UploadFileResponse> updateFile(String  id, String newName) {
        Path path = fileStorageService.getLocation();

        DBFile file = dbFileRepository.findById(id).get();
        Resource oldPath = fileStorageService.loadFileAsResource(file.getFileName());

        File oldFileName = null;
        try {
            oldFileName = new File(oldPath.getURI());
            oldFileName.renameTo(new File(path + "/" + newName));
        } catch (IOException e) {
            e.printStackTrace();
        }

        file.setFileName(newName);
        dbFileRepository.save(file);

        return getAllFiles();
    }

    public List<UploadFileResponse> removeFile(String  id) {
        DBFile file = dbFileRepository.findById(id).get();
        Resource path = fileStorageService.loadFileAsResource(file.getFileName());

        File fileName = null;
        try {
            fileName = new File(path.getURI());
        } catch (IOException e) {
            e.printStackTrace();
        }
        fileName.delete();

        dbFileRepository.delete(file);
        return getAllFiles();
    }
}
