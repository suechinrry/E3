package com.visitor.controller;

import com.visitor.ai.DeepSeekClient;
import com.visitor.ai.RiskVectorStore;
import com.visitor.common.Result;
import com.visitor.entity.Appointment;
import com.visitor.entity.RiskAssessment;
import com.visitor.service.AppointmentService;
import com.visitor.service.RiskAssessmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Tag(name = "AI风险预警")
@RestController
@RequestMapping("/admin/risk")
public class RiskController {

    private final RiskAssessmentService riskAssessmentService;
    private final AppointmentService appointmentService;
    private final RiskVectorStore riskVectorStore;
    private final DeepSeekClient deepSeekClient;

    public RiskController(RiskAssessmentService riskAssessmentService, AppointmentService appointmentService,
                          RiskVectorStore riskVectorStore, DeepSeekClient deepSeekClient) {
        this.riskAssessmentService = riskAssessmentService;
        this.appointmentService = appointmentService;
        this.riskVectorStore = riskVectorStore;
        this.deepSeekClient = deepSeekClient;
    }

    @Operation(summary = "查看某预约的风险评估结果")
    @GetMapping("/appointment/{appointmentId}")
    public Result<?> getByAppointment(@PathVariable Integer appointmentId) {
        RiskAssessment ra = riskAssessmentService.lambdaQuery().eq(RiskAssessment::getAppointmentId, appointmentId).one();
        if (ra == null) return Result.success(Map.of("assessed", false));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("assessed", true); data.put("riskLevel", ra.getRiskLevel());
        data.put("riskScore", ra.getRiskScore()); data.put("reason", ra.getReason());
        data.put("similarCases", ra.getSimilarCases()); data.put("createTime", ra.getCreateTime());
        return Result.success(data);
    }

    @GetMapping("/high") public Result<?> highRiskList() { return Result.success(riskAssessmentService.lambdaQuery().eq(RiskAssessment::getRiskLevel, "high").orderByDesc(RiskAssessment::getId).list()); }
    @GetMapping("/all") public Result<?> allList() { return Result.success(riskAssessmentService.lambdaQuery().orderByDesc(RiskAssessment::getId).list()); }

    @PostMapping("/appointment/{appointmentId}/reassess")
    public Result<Void> reassess(@PathVariable Integer appointmentId) {
        riskAssessmentService.lambdaUpdate().eq(RiskAssessment::getAppointmentId, appointmentId).remove();
        // 清除全部风险缓存（含该预约相关的所有键），保证重新评估基于最新知识库
        deepSeekClient.clearAllRiskCache();
        appointmentService.assessRiskIfNeeded(appointmentId);
        return Result.success("已触发重新评估", null);
    }

    @GetMapping("/store/status") public Result<?> storeStatus() {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("rejectedRecords", riskVectorStore.size()); d.put("phoneMatchWeight", riskVectorStore.getPhoneMatchWeight());
        d.put("nameMatchWeight", riskVectorStore.getNameMatchWeight()); d.put("companyFuzzyWeight", riskVectorStore.getCompanyFuzzyWeight());
        return Result.success(d);
    }

    @PutMapping("/store/weights")
    public Result<Void> updateWeights(@RequestBody Map<String, Double> body) {
        riskVectorStore.setWeights(body.getOrDefault("phoneMatchWeight",0.5), body.getOrDefault("nameMatchWeight",0.2), body.getOrDefault("companyFuzzyWeight",0.1));
        return Result.success("权重已更新", null);
    }

    @PostMapping("/store/rebuild") public Result<Void> rebuildStore() { riskVectorStore.reload(); deepSeekClient.clearAllRiskCache(); return Result.success("向量库已重建", null); }

    @GetMapping("/store/lookup/phone") public Result<?> lookupByPhone(@RequestParam String phone) { return Result.success(riskVectorStore.findByPhone(phone).stream().map(d -> Map.of("appointmentId",d.appointmentId(),"visitorName",d.visitorName(),"visitorPhone",d.visitorPhone(),"company",d.company(),"purpose",d.purpose())).collect(java.util.stream.Collectors.toList())); }
    @GetMapping("/store/lookup/name") public Result<?> lookupByName(@RequestParam String name) { return Result.success(riskVectorStore.findByName(name).stream().map(d -> Map.of("appointmentId",d.appointmentId(),"visitorName",d.visitorName(),"visitorPhone",d.visitorPhone(),"company",d.company(),"purpose",d.purpose())).collect(java.util.stream.Collectors.toList())); }
}
