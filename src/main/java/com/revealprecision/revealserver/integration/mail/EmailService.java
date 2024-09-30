package com.revealprecision.revealserver.integration.mail;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Qualifier("Email")
public class EmailService {

  private final JavaMailSender mailSender;

  public void sendEmail(List<String> to,String subject, String body) {
    SimpleMailMessage message = new SimpleMailMessage();

    String[] to_ = to.toArray(new String[]{});
    message.setTo(to_);
    message.setSubject(subject);
    message.setText(body);
    mailSender.send(message);
  }

}
