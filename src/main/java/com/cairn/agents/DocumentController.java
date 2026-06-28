package com.cairn.agents;

import java.io.IOException;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * WHY: Provides a REST endpoint for users to upload documents (PDF, TXT, etc.). These documents are
 * ingested into the RAG pipeline to ground the domain agents.
 */
@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

  private final DocumentIngestionService ingestionService;

  public DocumentController(DocumentIngestionService ingestionService) {
    this.ingestionService = ingestionService;
  }

  @PostMapping("/upload")
  public ResponseEntity<String> uploadDocument(@RequestParam("file") MultipartFile file) {
    if (file.isEmpty()) {
      return ResponseEntity.badRequest().body("Please select a file to upload.");
    }

    try {
      Resource resource =
          new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
              return file.getOriginalFilename();
            }
          };

      int chunks = ingestionService.ingest(resource);
      return ResponseEntity.ok(
          "Successfully ingested document: "
              + file.getOriginalFilename()
              + " into "
              + chunks
              + " chunks.");
    } catch (IOException e) {
      return ResponseEntity.internalServerError().body("Failed to process file: " + e.getMessage());
    } catch (Exception e) {
      return ResponseEntity.internalServerError()
          .body("Failed to ingest document: " + e.getMessage());
    }
  }
}
