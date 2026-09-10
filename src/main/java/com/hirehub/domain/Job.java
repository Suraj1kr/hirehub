package com.hirehub.domain;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="jobs")
public class Job {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,length=150) private String title;
 @Column(nullable=false,length=150) private String company;
 @Column(nullable=false,length=150) private String location;
 @Column(nullable=false,length=10000) private String description;
 @Column(nullable=false) private boolean active=true;
 @Column(nullable=false) private Instant createdAt=Instant.now();
 @Version private long version;
 public Job() {}
 public void update(String title,String company,String location,String description,boolean active){this.title=title;this.company=company;this.location=location;this.description=description;this.active=active;}
 public void close(){active=false;}
 public Long getId(){return id;} public String getTitle(){return title;} public String getCompany(){return company;} public String getLocation(){return location;}
 public String getDescription(){return description;} public boolean isActive(){return active;} public Instant getCreatedAt(){return createdAt;}
}
