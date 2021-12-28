package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.exceptions.FileFormatException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
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

  public void deleteCSV(String path) throws IOException {
    File file = new File(path);
    file.delete();
  }
}
