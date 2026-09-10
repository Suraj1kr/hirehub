package com.hirehub.web;
import com.hirehub.service.PortalService;
import com.hirehub.web.Forms.*;
import com.hirehub.domain.JobApplication;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.dao.DataIntegrityViolationException;
import java.security.Principal;
@Controller
public class PageController {
 private final PortalService service;public PageController(PortalService service){this.service=service;}
 @GetMapping("/") String home(){return "redirect:/jobs";}
 @GetMapping("/login") String login(){return "login";}
 @GetMapping("/register") String register(){return "register";}
 @PostMapping("/register") String register(@Valid @ModelAttribute Registration form,BindingResult result,Model model){if(result.hasErrors()){model.addAttribute("message","Enter your name, valid email, and a 12–72 character printable ASCII password.");return "register";}try{service.register(form);}catch(ResponseStatusException|DataIntegrityViolationException e){model.addAttribute("message","Email is already registered.");return "register";}return "redirect:/login?registered";}
 @GetMapping("/jobs") String jobs(@RequestParam(defaultValue="")String q,@RequestParam(defaultValue="0")int page,Model m){m.addAttribute("jobs",service.jobs(q,page));m.addAttribute("q",q);return "jobs";}
 @GetMapping("/jobs/{id}") String job(@PathVariable long id,Model m){m.addAttribute("job",service.job(id));return "job";}
 @PostMapping("/applications/job/{id}") String apply(@PathVariable long id,@Valid @ModelAttribute ApplicationInput form,BindingResult result,Principal p,RedirectAttributes r){if(result.hasErrors()){r.addFlashAttribute("message","Cover letter must contain 1–5000 characters.");return "redirect:/jobs/"+id;}service.apply(id,p.getName(),form);r.addFlashAttribute("message","Application submitted.");return "redirect:/applications";}
 @GetMapping("/applications") String mine(Principal p,@RequestParam(defaultValue="0")int page,Model m){m.addAttribute("applications",service.mine(p.getName(),page));return "applications";}
 @PostMapping("/applications/{id}/withdraw") String withdraw(@PathVariable long id,Principal p){service.withdraw(id,p.getName());return "redirect:/applications";}
 @GetMapping("/admin/jobs") String adminJobs(@RequestParam(defaultValue="0")int page,Model m){m.addAttribute("jobs",service.allJobs(page));return "admin/jobs";}
 @GetMapping("/admin/jobs/new") String newJob(Model m){m.addAttribute("job",new JobInput("","","","",true));return "admin/form";}
 @GetMapping("/admin/jobs/{id}/edit") String edit(@PathVariable long id,Model m){m.addAttribute("job",service.job(id));m.addAttribute("id",id);return "admin/form";}
 @PostMapping("/admin/jobs/save") String save(@RequestParam(required=false)Long id,@Valid @ModelAttribute("job")JobInput form,BindingResult result,Model m){if(result.hasErrors()){m.addAttribute("id",id);m.addAttribute("message","Complete every field within the indicated limits.");return "admin/form";}service.saveJob(id,form);return "redirect:/admin/jobs";}
 @PostMapping("/admin/jobs/{id}/close") String close(@PathVariable long id){service.closeJob(id);return "redirect:/admin/jobs";}
 @GetMapping("/admin/applications") String all(@RequestParam(defaultValue="0")int page,Model m){m.addAttribute("applications",service.allApplications(page));return "admin/applications";}
 @PostMapping("/admin/applications/{id}/status") String status(@PathVariable long id,@RequestParam JobApplication.Status status){service.status(id,status);return "redirect:/admin/applications";}
 @GetMapping("/admin/users") String users(@RequestParam(defaultValue="0")int page,Model m){m.addAttribute("users",service.allUsers(page));return "admin/users";}
 @ExceptionHandler(ResponseStatusException.class) org.springframework.web.servlet.ModelAndView business(ResponseStatusException e){var view=new org.springframework.web.servlet.ModelAndView("error");view.setStatus(e.getStatusCode());view.addObject("message",e.getReason());return view;}
}
