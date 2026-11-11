package com.olp.controller;
import com.olp.entity.User;
import com.olp.service.FinancialGovernanceService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.*;
@RestController
@RequestMapping("/api/payments/governance")
public class FinancialGovernanceController {
    private final FinancialGovernanceService service;
    private final JdbcTemplate jdbc;
    public FinancialGovernanceController(FinancialGovernanceService service,JdbcTemplate jdbc){this.service=service;this.jdbc=jdbc;}
    public record Policy(Integer financialRetentionDays,Integer adminAuditRetentionDays,BigDecimal withholdingRate,BigDecimal commissionTaxRate){}
    @GetMapping("/policy") public Map<String,Object> policy(@AuthenticationPrincipal User user){service.admin(user);return service.policy();}
    @PutMapping("/policy") public Map<String,Object> update(@AuthenticationPrincipal User user,@RequestBody Policy request){return request.commissionTaxRate()==null?service.update(user,request.financialRetentionDays(),request.adminAuditRetentionDays(),request.withholdingRate()):service.update(user,request.financialRetentionDays(),request.adminAuditRetentionDays(),request.withholdingRate(),request.commissionTaxRate());}
    @GetMapping("/ledger") public Map<String,Object> ledger(@AuthenticationPrincipal User user,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="50") int size){
        service.admin(user);if(page<0 || size<1 || size>200 || page>1000000)throw new IllegalArgumentException("Choose page >=0 and size 1–200.");
        var rows=jdbc.queryForList("SELECT id,table_name,operation,database_actor,recorded_at,old_record::text AS old_record,new_record::text AS new_record FROM financial_ledger ORDER BY id DESC LIMIT ? OFFSET ?",size,(long)page*size);
        return Map.of("page",page,"size",size,"total",jdbc.queryForObject("SELECT count(*) FROM financial_ledger",Long.class),"records",rows);
    }
}
