package w4cash.print;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({ "", "/api" })
class PrintJobController {

    private final PrintJobRepository repository;
    private final TicketPrintService ticketPrintService;

    PrintJobController(PrintJobRepository repository, TicketPrintService ticketPrintService) {
        this.repository = repository;
        this.ticketPrintService = ticketPrintService;
    }

    @GetMapping("/print-jobs")
    List<PrintJob> all(
            @RequestParam(required = false) String tableId,
            @RequestParam(required = false) String personName,
            @RequestParam(required = false) String success,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int clampedSize = (size == 20 || size == 50 || size == 100) ? size : 20;
        Boolean successFilter = "ok".equalsIgnoreCase(success) ? Boolean.TRUE
                              : "fail".equalsIgnoreCase(success) ? Boolean.FALSE
                              : null;
        return repository.find(tableId, personName, successFilter, Math.max(page, 0), clampedSize);
    }

    @PostMapping("/print-jobs/{id}/reprint")
    ResponseEntity<String> reprint(@PathVariable long id) {
        return repository.findById(id)
                .map(job -> ticketPrintService.reprint(job)
                        ? ResponseEntity.ok("ok")
                        : ResponseEntity.<String>internalServerError().body("print failed"))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/print-jobs/{id}")
    ResponseEntity<Void> delete(@PathVariable long id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
