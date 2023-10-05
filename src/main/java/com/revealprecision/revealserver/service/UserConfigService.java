package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.exceptions.DuplicateCreationException;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.persistence.domain.UserConfig;
import com.revealprecision.revealserver.persistence.repository.UserConfigRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserConfigService {

  private final UserConfigRepository userConfigRepository;

  private final StorageService storageService;

  public List<UserConfig> getAll(){
    return userConfigRepository.findAllByReceived(false);
  }

  public UserConfig create(String username) throws DuplicateCreationException{

    Optional<UserConfig> firstByUsername = userConfigRepository.findFirstByUsername(username);

    if (firstByUsername.isPresent()){
      throw new DuplicateCreationException("user config already exists for this user");
    }

    return userConfigRepository.save(UserConfig.builder()
        .createdAt(LocalDateTime.now())
        .username(username)
        .received(false)
        .build());
  }

  public UserConfig update(String username, MultipartFile file){

    Optional<UserConfig> firstByUsername = userConfigRepository.findFirstByUsername(username);


    if (firstByUsername.isEmpty()){
      throw new NotFoundException("Cannot update, user config does not exist for user: ".concat(username));
    } else {

      UserConfig userConfig = firstByUsername.get();

      userConfig.setReceivedAt(LocalDateTime.now());
      userConfig.setReceived(true);
      String filename = username.concat("-").concat(userConfig.getCreatedAt().format(
              DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))).concat("-")
          .concat(userConfig.getReceivedAt().format( DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
      userConfig.setFilename(filename);

      storageService.saveBlob(file,filename);

      return userConfigRepository.save(userConfig);
    }
  }
}
