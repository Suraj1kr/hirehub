CREATE TABLE users (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 name VARCHAR(100) NOT NULL,
 email VARCHAR(254) NOT NULL UNIQUE,
 password_hash VARCHAR(100) NOT NULL,
 role VARCHAR(20) NOT NULL
);
CREATE TABLE jobs (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 title VARCHAR(150) NOT NULL,
 company VARCHAR(150) NOT NULL,
 location VARCHAR(150) NOT NULL,
 description VARCHAR(10000) NOT NULL,
 active BOOLEAN NOT NULL,
 created_at TIMESTAMP NOT NULL,
 version BIGINT NOT NULL DEFAULT 0
);
CREATE TABLE applications (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT NOT NULL,
 job_id BIGINT NOT NULL,
 cover_letter VARCHAR(5000) NOT NULL,
 status VARCHAR(30) NOT NULL,
 created_at TIMESTAMP NOT NULL,
 version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT uq_application UNIQUE(user_id, job_id),
 CONSTRAINT fk_application_user FOREIGN KEY(user_id) REFERENCES users(id),
 CONSTRAINT fk_application_job FOREIGN KEY(job_id) REFERENCES jobs(id)
);
CREATE INDEX idx_jobs_active ON jobs(active);
CREATE INDEX idx_applications_job ON applications(job_id);
