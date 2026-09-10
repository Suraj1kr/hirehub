package com.hirehub.config;

import com.hirehub.dao.UserDao;
import com.hirehub.domain.UserAccount;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap implements CommandLineRunner {
  private final UserDao users;
  private final PasswordEncoder encoder;
  private final String email, password;

  public AdminBootstrap(
      UserDao users,
      PasswordEncoder encoder,
      @Value("${hirehub.admin.email:}") String email,
      @Value("${hirehub.admin.password:}") String password) {
    this.users = users;
    this.encoder = encoder;
    this.email = email;
    this.password = password;
  }

  public void run(String... args) {
    if (email.isBlank() && password.isBlank()) return;
    if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
        || !password.matches("[\\x20-\\x7E]{12,72}"))
      throw new IllegalStateException(
          "Provide a valid ADMIN_EMAIL and a 12-72 character ASCII ADMIN_PASSWORD");
    String normalized = email.trim().toLowerCase(Locale.ROOT);
    if (users.existsByEmail(normalized)) {
      if (users.findByEmail(normalized).orElseThrow().getRole() != UserAccount.Role.ADMIN)
        throw new IllegalStateException("Admin email belongs to a regular user");
      return;
    }
    users.save(
        new UserAccount(
            "Administrator", normalized, encoder.encode(password), UserAccount.Role.ADMIN));
  }
}
