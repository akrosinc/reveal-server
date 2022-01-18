package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.exceptions.FileFormatException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StorageService {

  public static final String TEMPLATE_BASE_PATH = "src/main/resources/template/";

  public String saveCSV(MultipartFile file) {
    if (!file.getContentType().equals("text/csv")) {
      throw new FileFormatException("Wrong file format. You can upload only .csv file!");
    }
    String path = "src/main/resources/batch/" + file.getOriginalFilename();
    Path filePath = Paths.get(path);
    try {
      file.transferTo(filePath);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return path;
  }

  public String saveJSON(MultipartFile file) {
    if (!MediaType.APPLICATION_JSON_VALUE.equals(file.getContentType())) {
      throw new FileFormatException("Wrong file format. You can upload only .json file!");
    }
    String path = "src/main/resources/batch/" + file.getOriginalFilename();
    Path filePath = Paths.get(path);
    try {
      file.transferTo(filePath);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return path;
  }

  public void deleteFile(String path) throws IOException {
    File file = new File(path);
    file.delete();
  }

  public ByteArrayResource downloadTemplate(String fileName) throws IOException {
    Path path = Paths.get(TEMPLATE_BASE_PATH + fileName);
    ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));

    return resource;
  }
}
