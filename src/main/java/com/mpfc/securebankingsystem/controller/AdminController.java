package com.mpfc.securebankingsystem.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.mpfc.securebankingsystem.entity.FileEncrypted;
import com.mpfc.securebankingsystem.repo.AuditLogRepository;
import com.mpfc.securebankingsystem.repo.EncryptedFileRepository;
import com.mpfc.securebankingsystem.repo.IncidentRepository;
import com.mpfc.securebankingsystem.service.FileService;
import com.mpfc.securebankingsystem.service.IncidentService;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final EncryptedFileRepository fileRepo;
    private final AuditLogRepository auditRepo;
    private final IncidentRepository incidentRepo;
    private final IncidentService incidentService;
    private final FileService fileService;

    public AdminController(EncryptedFileRepository fileRepo, AuditLogRepository auditRepo, IncidentRepository incidentRepo, IncidentService incidentService, FileService fileService) {
        this.fileRepo = fileRepo;
        this.auditRepo = auditRepo;
        this.incidentRepo = incidentRepo;
        this.incidentService = incidentService;
        this.fileService = fileService;
    }

    @GetMapping
    public String admin(Model model,
                        @RequestParam(name="fpage", defaultValue="0") int fpage,
                        @RequestParam(name="apage", defaultValue="0") int apage,
                        @RequestParam(name="ipage", defaultValue="0") int ipage) {

        Page<FileEncrypted> filesPage = fileRepo.findAll(PageRequest.of(fpage, 10, Sort.by(Sort.Direction.DESC,"uploadedAt")));
        Page<com.mpfc.securebankingsystem.entity.AuditLog> auditsPage = auditRepo.findAll(PageRequest.of(apage, 10, Sort.by(Sort.Direction.DESC,"timestamp")));
        Page<com.mpfc.securebankingsystem.entity.Incident> incidentsPage = incidentRepo.findAll(PageRequest.of(ipage, 10, Sort.by(Sort.Direction.DESC,"timestamp")));

        model.addAttribute("filesPage", filesPage);
        model.addAttribute("auditsPage", auditsPage);
        model.addAttribute("incidentsPage", incidentsPage);

        model.addAttribute("fpage", fpage);
        model.addAttribute("apage", apage);
        model.addAttribute("ipage", ipage);
        return "admin";
    }

    @PostMapping("/incidents/{id}/ack")
    public String ackIncident(@PathVariable Long id,
                              @RequestParam(name="ipage", defaultValue="0") int ipage,
                              @RequestParam(name="fpage", defaultValue="0") int fpage,
                              @RequestParam(name="apage", defaultValue="0") int apage) {
        incidentService.acknowledge(id);
        return "redirect:/admin?fpage=" + fpage + "&apage=" + apage + "&ipage=" + ipage;
    }

    @GetMapping("/files/{id}/decrypt")
    public ResponseEntity<byte[]> decrypt(@AuthenticationPrincipal User user, @PathVariable Long id) throws Exception {
        byte[] data = fileService.decryptForAdmin(user.getUsername(), id);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"decrypted-" + id + ".csv\"")
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(data);
    }
}