package com.olp.controller;
import com.olp.entity.User;
import com.olp.service.PaymentPolicyService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.Map;
@RestController
@RequestMapping("/api/payments/policy")
public class PaymentPolicyController {
    private final PaymentPolicyService policies;
    public PaymentPolicyController(PaymentPolicyService policies){this.policies=policies;}
    public record Update(BigDecimal commissionRate,BigDecimal taxRate,BigDecimal defaultPrice){}
    public record CourseUpdate(BigDecimal price,BigDecimal taxRate){}
    @org.springframework.beans.factory.annotation.Autowired private com.olp.repository.CourseRepository courses;
    @PutMapping("/courses/{id}") public Map<String,Object> course(@AuthenticationPrincipal User user,@PathVariable Integer id,@RequestBody CourseUpdate request){
        if(!courses.existsById(id))throw new IllegalArgumentException("Course not found.");
        return policies.updateCourse(user,id,request.price(),request.taxRate());
    }
    @GetMapping("/courses/{id}") public Map<String,Object> quote(@PathVariable Integer id){
        var course=courses.findById(id).orElseThrow(() -> new IllegalArgumentException("Course not found."));
        return Map.of("amount",policies.price(course),"currency","INR","taxInclusive",true);
    }
    @GetMapping("/catalog") public java.util.List<Map<String,Object>> catalog(){return courses.findAll().stream().filter(c -> c.getStatus()==com.olp.entity.CourseStatus.PUBLISHED).map(c -> Map.<String,Object>of("courseId",c.getId(),"amount",policies.price(c),"currency","INR","taxInclusive",true)).toList();}
    @GetMapping public Map<String,Object> current(){return policies.current();}
    @PutMapping public Map<String,Object> update(@AuthenticationPrincipal User user,@RequestBody Update request){return policies.update(user,request.commissionRate(),request.taxRate(),request.defaultPrice());}
}
