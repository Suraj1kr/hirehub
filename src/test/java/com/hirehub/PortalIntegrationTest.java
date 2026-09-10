package com.hirehub;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hirehub.dao.*;
import com.hirehub.domain.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles(resolver = PortalIntegrationTest.ProfileResolver.class)
class PortalIntegrationTest {
  public static class ProfileResolver
      implements org.springframework.test.context.ActiveProfilesResolver {
    public String[] resolve(Class<?> testClass) {
      return new String[] {System.getProperty("spring.profiles.active", "demo")};
    }
  }

  @Autowired MockMvc mvc;
  @Autowired UserDao users;
  @Autowired JobDao jobs;
  @Autowired ApplicationDao applications;
  @Autowired PasswordEncoder encoder;
  @Autowired ObjectMapper json;
  private static final String PASSWORD = "Test-password-123";

  @BeforeEach
  void reset() {
    applications.deleteAll();
    jobs.deleteAll();
    users.deleteAll();
    users.save(
        new UserAccount(
            "Candidate", "user@example.test", encoder.encode(PASSWORD), UserAccount.Role.USER));
  }

  String registration(String email) {
    return "{\"name\":\"Candidate\",\"email\":\""
        + email
        + "\",\"password\":\""
        + PASSWORD
        + "\",\"role\":\"ADMIN\"}";
  }

  Job job() {
    Job j = new Job();
    j.update("Java Developer", "Example Co", "Remote", "Build Java services.", true);
    return jobs.save(j);
  }

  @Test
  void registrationHashesPasswordNormalizesEmailAndCannotEscalateRole() throws Exception {
    mvc.perform(
            post("/api/auth/register")
                .with(csrf())
                .contentType("application/json")
                .content(registration("NEW@example.test")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.role").value("USER"))
        .andExpect(jsonPath("$.passwordHash").doesNotExist());
    var u = users.findByEmail("new@example.test").orElseThrow();
    assertThat(u.getPasswordHash()).isNotEqualTo(PASSWORD);
    assertThat(encoder.matches(PASSWORD, u.getPasswordHash())).isTrue();
    mvc.perform(
            post("/api/auth/register")
                .with(csrf())
                .contentType("application/json")
                .content(registration("new@example.test")))
        .andExpect(status().isConflict());
  }

  @Test
  void invalidInputAndMissingCsrfAreRejected() throws Exception {
    mvc.perform(
            post("/api/auth/register")
                .contentType("application/json")
                .content(registration("new@example.test")))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/auth/register")
                .with(csrf())
                .contentType("application/json")
                .content("{\"name\":\"\",\"email\":\"bad\",\"password\":\"short\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void loginLogoutAndSessionFixationProtection() throws Exception {
    mvc.perform(
            post("/login")
                .with(csrf())
                .param("username", "user@example.test")
                .param("password", "wrong"))
        .andExpect(unauthenticated())
        .andExpect(redirectedUrl("/login?error"));
    var old = new MockHttpSession();
    String oldId = old.getId();
    var result =
        mvc.perform(
                post("/login")
                    .session(old)
                    .with(csrf())
                    .param("username", "user@example.test")
                    .param("password", PASSWORD))
            .andExpect(authenticated().withUsername("user@example.test"))
            .andReturn();
    var session = (MockHttpSession) result.getRequest().getSession(false);
    assertThat(session.getId()).isNotEqualTo(oldId);
    mvc.perform(get("/api/me").session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("user@example.test"));
    mvc.perform(post("/logout").session(session).with(csrf())).andExpect(unauthenticated());
    assertThat(session.isInvalid()).isTrue();
    mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
  }

  @Test
  void roleChecksProtectAdminEndpoints() throws Exception {
    for (String path :
        new String[] {"/api/admin/jobs", "/api/admin/users", "/api/admin/applications"}) {
      mvc.perform(get(path)).andExpect(status().isUnauthorized());
      mvc.perform(get(path).with(user("user@example.test").roles("USER")))
          .andExpect(status().isForbidden());
      mvc.perform(get(path).with(user("admin@example.test").roles("ADMIN")))
          .andExpect(status().isOk());
    }
    mvc.perform(
            post("/api/admin/jobs")
                .with(user("user@example.test"))
                .with(csrf())
                .contentType("application/json")
                .content("{}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void applicationLifecyclePreventsDuplicatesAndCrossUserWithdrawal() throws Exception {
    long id = job().getId();
    String body = "{\"coverLetter\":\"I build reliable Java applications.\"}";
    var result =
        mvc.perform(
                post("/api/applications/job/" + id)
                    .with(user("user@example.test"))
                    .with(csrf())
                    .contentType("application/json")
                    .content(body))
            .andExpect(status().isCreated())
            .andReturn();
    long applicationId =
        json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    mvc.perform(
            post("/api/applications/job/" + id)
                .with(user("user@example.test"))
                .with(csrf())
                .contentType("application/json")
                .content(body))
        .andExpect(status().isConflict());
    mvc.perform(get("/api/applications").with(user("other@example.test")))
        .andExpect(jsonPath("$.totalElements").value(0));
    mvc.perform(
            post("/api/applications/" + applicationId + "/withdraw")
                .with(user("other@example.test"))
                .with(csrf()))
        .andExpect(status().isNotFound());
    mvc.perform(
            patch("/api/admin/applications/" + applicationId + "/status")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .contentType("application/json")
                .content("{\"status\":\"REVIEWING\"}"))
        .andExpect(status().isNoContent());
    mvc.perform(
            post("/api/applications/" + applicationId + "/withdraw")
                .with(user("user@example.test"))
                .with(csrf()))
        .andExpect(status().isNoContent());
    mvc.perform(
            patch("/api/admin/applications/" + applicationId + "/status")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .contentType("application/json")
                .content("{\"status\":\"ACCEPTED\"}"))
        .andExpect(status().isConflict());
  }

  @Test
  void adminCanCreateEditCloseAndClosedJobsRejectApplications() throws Exception {
    String body =
        "{\"title\":\"Java"
            + " Engineer\",\"company\":\"Example\",\"location\":\"Remote\",\"description\":\"Build"
            + " services\",\"active\":true}";
    var result =
        mvc.perform(
                post("/api/admin/jobs")
                    .with(user("admin").roles("ADMIN"))
                    .with(csrf())
                    .contentType("application/json")
                    .content(body))
            .andExpect(status().isCreated())
            .andReturn();
    long id = json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    mvc.perform(
            put("/api/admin/jobs/" + id)
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .contentType("application/json")
                .content(body.replace("Java Engineer", "Senior Java Engineer")))
        .andExpect(jsonPath("$.title").value("Senior Java Engineer"));
    mvc.perform(get("/api/jobs").param("q", "Senior"))
        .andExpect(jsonPath("$.totalElements").value(1));
    mvc.perform(delete("/api/admin/jobs/" + id).with(user("admin").roles("ADMIN")).with(csrf()))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/jobs")).andExpect(jsonPath("$.totalElements").value(0));
    mvc.perform(
            post("/api/applications/job/" + id)
                .with(user("user@example.test"))
                .with(csrf())
                .contentType("application/json")
                .content("{\"coverLetter\":\"Interested in this role\"}"))
        .andExpect(status().isConflict());
    mvc.perform(get("/api/jobs/999999")).andExpect(status().isNotFound());
  }

  @Test
  void thymeleafPagesRenderWithDataAndCsrf() throws Exception {
    Job j = job();
    applications.save(
        new JobApplication(
            users.findByEmail("user@example.test").orElseThrow(), j, "My experience"));
    for (String path : new String[] {"/jobs", "/jobs/" + j.getId(), "/login", "/register"})
      mvc.perform(get(path)).andExpect(status().isOk());
    mvc.perform(get("/login"))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
    mvc.perform(get("/applications").with(user("user@example.test"))).andExpect(status().isOk());
    for (String path :
        new String[] {
          "/admin/jobs",
          "/admin/jobs/new",
          "/admin/jobs/" + j.getId() + "/edit",
          "/admin/applications",
          "/admin/users"
        }) mvc.perform(get(path).with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
  }

  @Test
  void mvcRegistrationAndJobValidation() throws Exception {
    mvc.perform(
            post("/register")
                .with(csrf())
                .param("name", "New User")
                .param("email", "mvc@example.test")
                .param("password", PASSWORD))
        .andExpect(redirectedUrl("/login?registered"));
    mvc.perform(
            post("/admin/jobs/save")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .param("title", "")
                .param("company", "Co")
                .param("location", "Remote")
                .param("description", "Description"))
        .andExpect(status().isOk())
        .andExpect(view().name("admin/form"));
  }
}
