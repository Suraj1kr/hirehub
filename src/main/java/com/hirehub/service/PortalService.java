package com.hirehub.service;
import com.hirehub.dao.*;
import com.hirehub.domain.*;
import com.hirehub.web.Forms.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.Locale;
@Service @Transactional
public class PortalService {
 private final UserDao users; private final JobDao jobs; private final ApplicationDao applications; private final PasswordEncoder encoder;
 public PortalService(UserDao users,JobDao jobs,ApplicationDao applications,PasswordEncoder encoder){this.users=users;this.jobs=jobs;this.applications=applications;this.encoder=encoder;}
 public UserAccount register(Registration input){String email=input.email().trim().toLowerCase(Locale.ROOT);if(users.existsByEmail(email))throw error(HttpStatus.CONFLICT,"Email is already registered");return users.saveAndFlush(new UserAccount(input.name().trim(),email,encoder.encode(input.password()),UserAccount.Role.USER));}
 public static ResponseStatusException error(HttpStatus status,String message){return new ResponseStatusException(status,message);}
 public Pageable page(int page){return PageRequest.of(Math.max(0,page),20,Sort.by(Sort.Direction.DESC,"id"));}
 @Transactional(readOnly=true) public Page<Job> jobs(String query,int page){return jobs.search(query==null?"":query,page(page));}
 @Transactional(readOnly=true) public Job job(long id){return jobs.findById(id).orElseThrow(()->error(HttpStatus.NOT_FOUND,"Job not found"));}
 private Job lockedJob(long id){return jobs.lockById(id).orElseThrow(()->error(HttpStatus.NOT_FOUND,"Job not found"));}
 @PreAuthorize("hasRole('ADMIN')") @Transactional(readOnly=true) public Page<Job> allJobs(int page){return jobs.findAll(page(page));}
 @PreAuthorize("hasRole('ADMIN')") public Job saveJob(Long id,JobInput input){Job j=id==null?new Job():lockedJob(id);j.update(input.title().trim(),input.company().trim(),input.location().trim(),input.description().trim(),input.active());return jobs.save(j);}
 @PreAuthorize("hasRole('ADMIN')") public void closeJob(long id){lockedJob(id).close();}
 @PreAuthorize("hasRole('USER')") public JobApplication apply(long id,String email,ApplicationInput input){Job j=lockedJob(id);if(!j.isActive())throw error(HttpStatus.CONFLICT,"This job is closed");UserAccount u=account(email);if(applications.existsByUserIdAndJobId(u.getId(),id))throw error(HttpStatus.CONFLICT,"You have already applied for this job");return applications.saveAndFlush(new JobApplication(u,j,input.coverLetter().trim()));}
 @Transactional(readOnly=true) public UserAccount account(String email){return users.findByEmail(email).orElseThrow(()->error(HttpStatus.NOT_FOUND,"Account not found"));}
 @PreAuthorize("hasRole('USER')") @Transactional(readOnly=true) public Page<JobApplication> mine(String email,int page){return applications.findByUserEmail(email,page(page));}
 @PreAuthorize("hasRole('ADMIN')") @Transactional(readOnly=true) public Page<JobApplication> allApplications(int page){return applications.findAll(page(page));}
 @PreAuthorize("hasRole('ADMIN')") @Transactional(readOnly=true) public Page<UserAccount> allUsers(int page){return users.findAll(page(page));}
 @PreAuthorize("hasRole('USER')") public void withdraw(long id,String email){JobApplication a=application(id);if(!a.getUser().getEmail().equals(email))throw error(HttpStatus.NOT_FOUND,"Application not found");if(a.getStatus()!=JobApplication.Status.SUBMITTED && a.getStatus()!=JobApplication.Status.REVIEWING)throw error(HttpStatus.CONFLICT,"Only pending applications can be withdrawn");a.setStatus(JobApplication.Status.WITHDRAWN);}
 @PreAuthorize("hasRole('ADMIN')") public void status(long id,JobApplication.Status status){JobApplication a=application(id);if(a.getStatus()!=JobApplication.Status.SUBMITTED && a.getStatus()!=JobApplication.Status.REVIEWING)throw error(HttpStatus.CONFLICT,"Application is already final");if(status!=JobApplication.Status.REVIEWING && status!=JobApplication.Status.ACCEPTED && status!=JobApplication.Status.REJECTED)throw error(HttpStatus.BAD_REQUEST,"Choose REVIEWING, ACCEPTED or REJECTED");a.setStatus(status);}
 private JobApplication application(long id){return applications.findById(id).orElseThrow(()->error(HttpStatus.NOT_FOUND,"Application not found"));}
}
