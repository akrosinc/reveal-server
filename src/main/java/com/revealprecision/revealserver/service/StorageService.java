package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.exceptions.FileFormatException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class StorageService {


  private final Environment environment;


  public String saveCSV(MultipartFile file) {
    if (!file.getContentType().equals("text/csv")) {
      throw new FileFormatException("Wrong file format. You can upload only .csv file!");
    }
    String path = environment.getProperty("batch.location") + file.getOriginalFilename();
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
    String path = environment.getProperty("batch.location") + file.getOriginalFilename();
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
    Path path = Paths.get(environment.getProperty("batch.template") + fileName);
    ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));

    return resource;
  }
}
