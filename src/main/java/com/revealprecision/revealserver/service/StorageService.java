package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.exceptions.FileFormatException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class StorageService {


  public static final String BATCH_LOCATION_PATH = "batch.location";
  public static final String BATCH_TEMPLATE_PATH = "batch.template";
  public static final String MATADATA_IMPORT_PATH = "batch.metaImport";
  private final Environment environment;


  public String saveCSV(MultipartFile file) {
    if (!file.getContentType().equals("text/csv") && !(file.getContentType()
        .equals("application/vnd.ms-excel"))) {
      throw new FileFormatException("Wrong file format. You can upload only .csv file!");
    }
    String path = environment.getProperty(BATCH_LOCATION_PATH) + file.getOriginalFilename();
    Path filePath = Paths.get(path);
    try {
      file.transferTo(filePath);
    } catch (IOException e) {
      e.printStackTrace();
      log.error("Error transferring storage file at path {}",filePath,e);
    }
    return path;
  }

  public String saveXlsx(MultipartFile file) {
    if (!file.getContentType()
        .equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        && !(file.getContentType()
        .equals("application/vnd.ms-excel"))) {
      throw new FileFormatException("Wrong file format. You can upload only .xlsx file!");
    }
    String path = environment.getProperty(MATADATA_IMPORT_PATH) + file.getOriginalFilename();
    Path filePath = Paths.get(path);
    try {
      file.transferTo(filePath);
    } catch (IOException e) {
      e.printStackTrace();
      log.error("Error transferring storage file at path {}",filePath,e);
    }
    return path;
  }

  public String saveJSON(MultipartFile file) {
    if (!MediaType.APPLICATION_JSON_VALUE.equals(file.getContentType())) {
      throw new FileFormatException("Wrong file format. You can upload only .json file!");
    }
    String path = environment.getProperty(BATCH_LOCATION_PATH) + file.getOriginalFilename();
    Path filePath = Paths.get(path);
    try {
      file.transferTo(filePath);
    } catch (IOException e) {
      e.printStackTrace();
      log.error("Error transferring storage file at path {}",filePath,e);
    }
    return path;
  }

  public void deleteFile(String path) throws IOException {
    File file = new File(path);
    file.delete();
  }

  public ByteArrayResource downloadTemplate(String fileName) throws IOException {
    Path path = Paths.get(environment.getProperty(BATCH_TEMPLATE_PATH) + fileName);
    ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));

    return resource;
  }
}
