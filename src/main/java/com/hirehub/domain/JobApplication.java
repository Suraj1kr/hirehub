package com.hirehub.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
    name = "applications",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "job_id"}))
public class JobApplication {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private UserAccount user;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "job_id")
  private Job job;

  @Column(nullable = false, length = 5000)
  private String coverLetter;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private Status status = Status.SUBMITTED;

  @Column(nullable = false)
  @org.hibernate.annotations.JdbcTypeCode(java.sql.Types.TIMESTAMP)
  private Instant createdAt = Instant.now();

  @Version private long version;

  public enum Status {
    SUBMITTED,
    REVIEWING,
    ACCEPTED,
    REJECTED,
    WITHDRAWN
  }

  protected JobApplication() {}

  public JobApplication(UserAccount user, Job job, String coverLetter) {
    this.user = user;
    this.job = job;
    this.coverLetter = coverLetter;
  }

  public Long getId() {
    return id;
  }

  public UserAccount getUser() {
    return user;
  }

  public Job getJob() {
    return job;
  }

  public String getCoverLetter() {
    return coverLetter;
  }

  public Status getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setStatus(Status status) {
    this.status = status;
  }
}
