import re

with open('src/main/kotlin/com/aquinofroilan/tessera/domain/finance/controller/FinancialReportController.kt', 'r') as f:
    content = f.read()

trial_balance_method = """
    @GetMapping("/trial-balance")
    @PreAuthorize("hasAuthority('journal:read')")
    fun getTrialBalance(
        @CurrentOrganizationId orgId: UUID,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) asOfDate: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) compareAsOfDate: LocalDate?,
    ): ResponseEntity<Any> {
        val report = financialReportService.getComparativeTrialBalance(orgId, asOfDate, compareAsOfDate)
        return ResponseEntity.ok(report)
    }
"""

content = re.sub(r'@GetMapping\("/trial-balance"\)\n\s+@PreAuthorize\("hasAuthority\(\'journal:read\'\)"\)\n\s+@GetMapping\("/cash-bank-summary"\)', '@GetMapping("/cash-bank-summary")', content)
# Wait, let's just use string replace.
