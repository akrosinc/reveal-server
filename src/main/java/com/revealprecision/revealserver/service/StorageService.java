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

  public String saveCSV(MultipartFile file) {
    if (!file.getOriginalFilename().endsWith(".csv")) {
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
    //TODO: combine methods for saving file? should we be checking for file extension or MIME type or both?
    if (!file.getContentType().endsWith(MediaType.APPLICATION_JSON_VALUE)) {
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

  public ByteArrayResource downloadTemplate() throws IOException {
    Path path = Paths.get("src/main/resources/template/AkrosTemplate.xlsx");
    ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));

    return resource;
  }
}
