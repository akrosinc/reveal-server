package com.revealprecision.revealserver.integration.mail;

import java.util.List;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Profile("Email")
public class EmailService {

  private final JavaMailSender mailSender;

  public void sendEmail(List<String> to,String subject, String body) throws MessagingException {
    MimeMessage message = mailSender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(message, true);
    String[] to_ = to.toArray(new String[]{});
    helper.setTo(to_);
    helper.setSubject(subject);
    helper.setText(body,true);

    mailSender.send(message);
  }

}
