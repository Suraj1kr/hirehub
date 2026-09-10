package com.hirehub.web;

import com.hirehub.domain.*;
import com.hirehub.service.PortalService;
import com.hirehub.web.Forms.*;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.Instant;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ApiController {
  private final PortalService service;

  public ApiController(PortalService service) {
    this.service = service;
  }

  public record AccountView(Long id, String name, String email, String role) {
    static AccountView of(UserAccount u) {
      return new AccountView(u.getId(), u.getName(), u.getEmail(), u.getRole().name());
    }
  }

  public record ApplicationView(
      Long id,
      Long jobId,
      String jobTitle,
      String applicant,
      String email,
      String coverLetter,
      String status,
      Instant createdAt) {
    static ApplicationView of(JobApplication a) {
      return new ApplicationView(
          a.getId(),
          a.getJob().getId(),
          a.getJob().getTitle(),
          a.getUser().getName(),
          a.getUser().getEmail(),
          a.getCoverLetter(),
          a.getStatus().name(),
          a.getCreatedAt());
    }
  }

  @GetMapping("/csrf")
  public Map<String, String> csrf(CsrfToken token) {
    return Map.of(
        "token",
        token.getToken(),
        "headerName",
        token.getHeaderName(),
        "parameterName",
        token.getParameterName());
  }

  @PostMapping("/auth/register")
  @ResponseStatus(HttpStatus.CREATED)
  public AccountView register(@Valid @RequestBody Registration form) {
    return AccountView.of(service.register(form));
  }

  @GetMapping("/me")
  public AccountView me(Principal principal) {
    return AccountView.of(service.account(principal.getName()));
  }

  @GetMapping("/jobs")
  public Page<Job> jobs(
      @RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "0") int page) {
    return service.jobs(q, page);
  }

  @GetMapping("/jobs/{id}")
  public Job job(@PathVariable long id) {
    return service.job(id);
  }

  @PostMapping("/applications/job/{id}")
  @ResponseStatus(HttpStatus.CREATED)
  public ApplicationView apply(
      @PathVariable long id, @Valid @RequestBody ApplicationInput form, Principal p) {
    return ApplicationView.of(service.apply(id, p.getName(), form));
  }

  @GetMapping("/applications")
  public Page<ApplicationView> mine(Principal p, @RequestParam(defaultValue = "0") int page) {
    return service.mine(p.getName(), page).map(ApplicationView::of);
  }

  @PostMapping("/applications/{id}/withdraw")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void withdraw(@PathVariable long id, Principal p) {
    service.withdraw(id, p.getName());
  }

  @GetMapping("/admin/jobs")
  public Page<Job> allJobs(@RequestParam(defaultValue = "0") int page) {
    return service.allJobs(page);
  }

  @PostMapping("/admin/jobs")
  @ResponseStatus(HttpStatus.CREATED)
  public Job create(@Valid @RequestBody JobInput form) {
    return service.saveJob(null, form);
  }

  @PutMapping("/admin/jobs/{id}")
  public Job update(@PathVariable long id, @Valid @RequestBody JobInput form) {
    return service.saveJob(id, form);
  }

  @DeleteMapping("/admin/jobs/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void close(@PathVariable long id) {
    service.closeJob(id);
  }

  @GetMapping("/admin/applications")
  public Page<ApplicationView> applications(@RequestParam(defaultValue = "0") int page) {
    return service.allApplications(page).map(ApplicationView::of);
  }

  @PatchMapping("/admin/applications/{id}/status")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void status(@PathVariable long id, @Valid @RequestBody StatusInput form) {
    service.status(id, form.status());
  }

  @GetMapping("/admin/users")
  public Page<AccountView> users(@RequestParam(defaultValue = "0") int page) {
    return service.allUsers(page).map(AccountView::of);
  }
}
