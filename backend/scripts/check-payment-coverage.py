"""Enforce payment backend line coverage from the full JaCoCo XML report."""
from pathlib import Path
import xml.etree.ElementTree as ET
names = set("PaymentService RefundService WalletService InvoiceService CouponService CurrencyService ReconciliationService InstructorPayoutService PaymentWebhookService PaymentWebhookSignature PaymentEventActions PaymentAuditService SimulatedPaymentMethods PaymentPolicyService PaymentSubscriptionService PaymentMaintenanceService PaymentMaintenanceJobs FraudDetectionService RazorpayGatewayClient RefundPolicy ExceptionalRefundService OrderExpirationService RoyaltyRefundService BankAccountService FinancialGovernanceService FinancialDocumentService PaymentEvidenceService PaymentSettlementService StripePaymentService UpiQrCodeService PaymentController PaymentWebhookController PaymentAuditController PaymentSimulationController PaymentPolicyController PaymentSubscriptionController PaymentMaintenanceController PaymentEvidenceController FinancialDocumentController FinancialGovernanceController BankAccountController InvoiceController RefundController WalletController InstructorPayoutController CouponController CurrencyController ReconciliationController UpiQrController StripePaymentController PaymentAdminAuditConfig PaymentAdminResponseAudit PaymentAccessEvidenceConfig PaymentScheduling PaymentAccess SecurityConfig".split())
root = ET.parse(Path(__file__).resolve().parents[1] / "target/site/jacoco/jacoco.xml").getroot()
covered = missed = 0
found = set()
for cls in root.findall(".//class"):
    name = cls.get("name").split("/")[-1].split("$")[0]
    if name not in names:
        continue
    found.add(name)
    for counter in cls.findall("counter"):
        if counter.get("type") == "LINE":
            covered += int(counter.get("covered"))
            missed += int(counter.get("missed"))
assert names <= found, f"Missing coverage classes: {names - found}"
coverage = covered / (covered + missed)
print(f"Payment backend line coverage: {coverage:.2%} ({covered}/{covered+missed})")
assert coverage >= .70, "Payment coverage must be at least 70%"
