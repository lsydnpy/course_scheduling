package com.xy.course_scheduling.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xy.course_scheduling.custom.annotations.OperLogAnn;
import com.xy.course_scheduling.entity.CourseAdjust;
import com.xy.course_scheduling.entity.Result;
import com.xy.course_scheduling.service.CourseAdjustService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 调课申请表 前端控制器
 * </p>
 *
 * @author xy
 * @since 2026-03-13
 */
@RestController
@RequestMapping("/courseAdjust")
@Tag(name = "调课申请管理")
@Slf4j
public class CourseAdjustController {

    @Resource
    private CourseAdjustService courseAdjustService;

    @PostMapping
    @OperLogAnn(title = "添加调课申请", businessType = OperLogAnn.BusinessType.INSERT)
    @Operation(summary = "添加调课申请")
    public Result<CourseAdjust> save(@RequestBody CourseAdjust courseAdjust) {
        boolean save = courseAdjustService.submitAdjust(courseAdjust);
        if (save) {
            return Result.ok(courseAdjust);
        }
        return Result.fail("添加失败");
    }

    @PutMapping("/approve")
    @OperLogAnn(title = "审批调课申请", businessType = OperLogAnn.BusinessType.UPDATE)
    @Operation(summary = "审批调课申请")
    public Result<String> approve(@RequestBody ApproveRequest request) {
        boolean result = courseAdjustService.approveAdjust(
                request.getAdjustId(),
                request.isApproved(),
                request.getRemark(),
                request.getApproverUsername(),
                request.getApproverName()
        );
        if (result) {
            return Result.ok("审批成功");
        }
        return Result.fail("审批失败");
    }

    @PutMapping("/cancel/{id}")
    @OperLogAnn(title = "撤销调课申请", businessType = OperLogAnn.BusinessType.UPDATE)
    @Operation(summary = "撤销调课申请")
    public Result<String> cancel(@PathVariable Integer id) {
        boolean result = courseAdjustService.cancelAdjust(id);
        if (result) {
            return Result.ok("撤销成功");
        }
        return Result.fail("撤销失败");
    }

    @DeleteMapping("/{id}")
    @OperLogAnn(title = "删除调课申请", businessType = OperLogAnn.BusinessType.DELETE)
    @Operation(summary = "删除调课申请")
    public Result<String> delete(@PathVariable Integer id) {
        boolean remove = courseAdjustService.removeById(id);
        if (remove) {
            return Result.ok("删除成功");
        }
        return Result.fail("删除失败");
    }

    @GetMapping("/{id}")
    @OperLogAnn(title = "查询调课申请", businessType = OperLogAnn.BusinessType.SELECT)
    @Operation(summary = "查询调课申请")
    public Result<CourseAdjust> getById(@PathVariable Integer id) {
        return Result.ok(courseAdjustService.getById(id));
    }

    @GetMapping("/list")
    @OperLogAnn(title = "查询调课申请列表", businessType = OperLogAnn.BusinessType.SELECT)
    @Operation(summary = "查询调课申请列表")
    public Result<List<CourseAdjust>> list(CourseAdjust courseAdjust, Page<CourseAdjust> page) {
        LambdaQueryWrapper<CourseAdjust> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ObjectUtils.isNotEmpty(courseAdjust.getSemesterId()), CourseAdjust::getSemesterId, courseAdjust.getSemesterId());
        wrapper.eq(ObjectUtils.isNotEmpty(courseAdjust.getStatus()), CourseAdjust::getStatus, courseAdjust.getStatus());
        wrapper.eq(ObjectUtils.isNotEmpty(courseAdjust.getApplicantName()), CourseAdjust::getApplicantName, courseAdjust.getApplicantName());
        wrapper.eq(ObjectUtils.isNotEmpty(courseAdjust.getTeacherId()), CourseAdjust::getTeacherId, courseAdjust.getTeacherId());
        wrapper.orderByDesc(CourseAdjust::getApplyTime);

        IPage<CourseAdjust> pageData = courseAdjustService.page(page, wrapper);
        return Result.ok(pageData);
    }

    @GetMapping("/my-list")
    @OperLogAnn(title = "查询我的调课申请列表", businessType = OperLogAnn.BusinessType.SELECT)
    @Operation(summary = "查询我的调课申请列表")
    public Result<List<CourseAdjust>> myList(CourseAdjust courseAdjust, Page<CourseAdjust> page, @RequestParam String username) {
        LambdaQueryWrapper<CourseAdjust> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ObjectUtils.isNotEmpty(username), CourseAdjust::getApplicantUsername, username);
        wrapper.eq(ObjectUtils.isNotEmpty(courseAdjust.getSemesterId()), CourseAdjust::getSemesterId, courseAdjust.getSemesterId());
        wrapper.eq(ObjectUtils.isNotEmpty(courseAdjust.getStatus()), CourseAdjust::getStatus, courseAdjust.getStatus());
        wrapper.orderByDesc(CourseAdjust::getApplyTime);

        IPage<CourseAdjust> pageData = courseAdjustService.page(page, wrapper);
        return Result.ok(pageData);
    }

    @GetMapping("/stats")
    @OperLogAnn(title = "查询调课申请统计", businessType = OperLogAnn.BusinessType.SELECT)
    @Operation(summary = "查询调课申请统计")
    public Result<AdjustStats> stats(@RequestParam Integer semesterId) {
        LambdaQueryWrapper<CourseAdjust> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CourseAdjust::getSemesterId, semesterId);

        List<CourseAdjust> all = courseAdjustService.list(wrapper);
        AdjustStats stats = new AdjustStats();
        stats.setTotal(all.size());
        stats.setPending(all.stream().filter(a -> "PENDING".equals(a.getStatus())).count());
        stats.setApproved(all.stream().filter(a -> "APPROVED".equals(a.getStatus())).count());
        stats.setRejected(all.stream().filter(a -> "REJECTED".equals(a.getStatus())).count());
        stats.setCancelled(all.stream().filter(a -> "CANCELLED".equals(a.getStatus())).count());

        return Result.ok(stats);
    }

    public static class ApproveRequest {
        private Integer adjustId;
        private boolean approved;
        private String remark;
        private String approverUsername;
        private String approverName;

        public Integer getAdjustId() {
            return adjustId;
        }

        public void setAdjustId(Integer adjustId) {
            this.adjustId = adjustId;
        }

        public boolean isApproved() {
            return approved;
        }

        public void setApproved(boolean approved) {
            this.approved = approved;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }

        public String getApproverUsername() {
            return approverUsername;
        }

        public void setApproverUsername(String approverUsername) {
            this.approverUsername = approverUsername;
        }

        public String getApproverName() {
            return approverName;
        }

        public void setApproverName(String approverName) {
            this.approverName = approverName;
        }
    }

    public static class AdjustStats {
        private Integer total;
        private Long pending;
        private Long approved;
        private Long rejected;
        private Long cancelled;

        public Integer getTotal() {
            return total;
        }

        public void setTotal(Integer total) {
            this.total = total;
        }

        public Long getPending() {
            return pending;
        }

        public void setPending(Long pending) {
            this.pending = pending;
        }

        public Long getApproved() {
            return approved;
        }

        public void setApproved(Long approved) {
            this.approved = approved;
        }

        public Long getRejected() {
            return rejected;
        }

        public void setRejected(Long rejected) {
            this.rejected = rejected;
        }

        public Long getCancelled() {
            return cancelled;
        }

        public void setCancelled(Long cancelled) {
            this.cancelled = cancelled;
        }
    }
}
