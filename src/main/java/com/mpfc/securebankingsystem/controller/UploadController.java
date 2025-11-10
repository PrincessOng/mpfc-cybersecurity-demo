package com.mpfc.securebankingsystem.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.mpfc.securebankingsystem.entity.FileEncrypted;
import com.mpfc.securebankingsystem.repo.EncryptedFileRepository;
import com.mpfc.securebankingsystem.service.FileService;
import com.mpfc.securebankingsystem.service.IncidentService;

@Controller
@RequestMapping("/upload")
public class UploadController {

    private final FileService fileService;
    private final EncryptedFileRepository repo;
    private final IncidentService incidentService;

    public UploadController(FileService fileService, EncryptedFileRepository repo, IncidentService incidentService) {
        this.fileService = fileService;
        this.repo = repo;
        this.incidentService = incidentService;
    }

    @GetMapping
    public String uploadForm(@RequestParam(name="page", defaultValue="0") int page, Model model) {
        Page<FileEncrypted> filesPage = repo.findAll(PageRequest.of(page, 10, Sort.by(Sort.Direction.DESC,"uploadedAt")));
        model.addAttribute("filesPage", filesPage);
        model.addAttribute("currentPage", page);
        return "upload";
    }

    @PostMapping
    public String handleUpload(@AuthenticationPrincipal User user,
                               @RequestParam("file") MultipartFile file,
                               @RequestParam(name="page", defaultValue="0") int page,
                               Model model) {
        try {
            fileService.uploadAndEncrypt(user.getUsername(), file);
            model.addAttribute("success", "File uploaded and encrypted successfully.");
        } catch (Exception ex) {
            incidentService.recordIncident(user.getUsername(), "UPLOAD_FAILED", ex.getMessage());
            model.addAttribute("error", "Upload failed: " + ex.getMessage());
        }
        Page<FileEncrypted> filesPage = repo.findAll(PageRequest.of(page, 10, Sort.by(Sort.Direction.DESC,"uploadedAt")));
        model.addAttribute("filesPage", filesPage);
        model.addAttribute("currentPage", page);
        return "upload";
    }
}