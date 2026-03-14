//package com.xy.course_scheduling.algorithm;
//
//import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
//import com.xy.course_scheduling.entity.*;
//import com.xy.course_scheduling.mapper.*;
//import jakarta.annotation.Resource;
//import org.springframework.stereotype.Service;
//
//import java.util.*;
//import java.util.concurrent.ThreadLocalRandom;
//import java.util.stream.Collectors;
//
/// **
// * 基于贪心算法和局部搜索的混合排课算法
// * 结合了贪心算法的高效性和局部搜索的优化能力
// */
//@Service
//public class HybridSchedulingAlgorithm {
//
//    // 算法参数
//    private static final int MAX_LOCAL_SEARCH_ITERATIONS = 1000; // 最大局部搜索迭代次数
//    private static final double LOCAL_SEARCH_IMPROVEMENT_THRESHOLD = 0.01; // 局部搜索改进阈值
//    @Resource
//    private TeachingTaskMapper teachingTaskMapper;
//    @Resource
//    private ScheduleMapper scheduleMapper;
//    @Resource
//    private TeacherTimeWhitelistMapper TeacherTimeWhitelistMapper;
//    @Resource
//    private ActivityBlacklistMapper activityBlacklistMapper;
//    @Resource
//    private ClassroomMapper classroomMapper;
//    @Resource
//    private TeacherMapper teacherMapper;
//    @Resource
//    private StudentGroupMapper studentGroupMapper;
//    @Resource
//    private CourseMapper courseMapper;
//    @Resource
//    private CoursePartMapper coursePartMapper;
//    @Resource
//    private RoomTypeMapper roomTypeMapper;
//    @Resource
//    private CampusMapper campusMapper;
//    @Resource
//    private SemesterMapper semesterMapper;
//    @Resource
//    private CollegeMapper collegeMapper;
//
//    /**
//     * 执行自动排课
//     *
//     * @param semesterId 学期ID
//     * @return 排课结果
//     */
//    public Result<String> autoSchedule(Integer semesterId) {
//        try {
//            // 验证学期是否存在
//            Semester semester = semesterMapper.selectById(semesterId);
//            if (semester == null) {
//                return Result.fail("学期不存在");
//            }
//
//            // 获取所有待排课的教学任务
//            List<TeachingTask> allTasks = teachingTaskMapper.selectList(
//                    new LambdaQueryWrapper<TeachingTask>()
//                            .eq(TeachingTask::getSemesterId, semesterId)
//                            .eq(TeachingTask::getStatus, "CONFIRMED")
//                            .eq(TeachingTask::getDeleted, 0)
//            );
//
//            // 如果没有需要排课的任务，直接返回成功
//            if (allTasks.isEmpty()) {
//                return Result.ok("没有需要排课的教学任务");
//            }
//
//            // 清除该学期的现有排课记录
//            clearExistingSchedules(semesterId);
//
//            // 准备排课所需的所有数据
//            SchedulingData data = prepareSchedulingData(allTasks, semesterId);
//
//            // 使用贪心算法生成初始解
//            ScheduleSolution initialSolution = generateGreedySolution(data);
//
//            // 使用局部搜索优化初始解
//            ScheduleSolution optimizedSolution = localSearchOptimization(initialSolution, data);
//
//            // 检查优化结果是否满足要求
//            if (optimizedSolution.getScheduleRate() >= 0.95) { // 95%的任务安排率
//                // 保存最终排课结果
//                saveScheduleResult(optimizedSolution, data);
//                return Result.ok("排课成功，安排率：" + String.format("%.2f%%", optimizedSolution.getScheduleRate() * 100) +
//                        "，冲突数：" + optimizedSolution.getConflictCount());
//            } else {
//                // 即使安排率不足95%，也保存可用的排课结果
//                saveScheduleResult(optimizedSolution, data);
//                return Result.ok("排课基本完成，安排率：" + String.format("%.2f%%", optimizedSolution.getScheduleRate() * 100) +
//                        "，冲突数：" + optimizedSolution.getConflictCount() +
//                        "，部分任务未能安排，请人工调整");
//            }
//        } catch (Exception e) {
//            // 记录异常并返回失败信息
//            e.printStackTrace();
//            return Result.fail("排课失败: " + e.getMessage());
//        }
//    }
//
//    /**
//     * 准备排课所需的所有数据
//     *
//     * @param allTasks   所有待排课任务
//     * @param semesterId 学期ID
//     * @return 排课数据对象
//     */
//    private SchedulingData prepareSchedulingData(List<TeachingTask> allTasks, Integer semesterId) {
//        // 创建排课数据容器
//        SchedulingData data = new SchedulingData();
//        data.allTasks = allTasks;
//        data.semesterId = semesterId;
//
//        // 获取教师时间黑名单
//        data.teacherBlacklists = TeacherTimeWhitelistMapper.selectList(
//                new LambdaQueryWrapper<TeacherTimeWhitelist>()
//                        .eq(TeacherTimeWhitelist::getSemesterId, semesterId)
//                        .eq(TeacherTimeWhitelist::getDeleted, 0)
//        );
//
//        // 获取活动时间黑名单
//        data.activityBlacklists = activityBlacklistMapper.selectList(
//                new LambdaQueryWrapper<ActivityBlacklist>()
//                        .eq(ActivityBlacklist::getSemesterId, semesterId)
//                        .eq(ActivityBlacklist::getDeleted, 0)
//        );
//
//        // 获取所有可用教室
//        data.allClassrooms = classroomMapper.selectList(
//                new LambdaQueryWrapper<Classroom>()
//                        .eq(Classroom::getIsAvailable, true)
//                        .eq(Classroom::getDeleted, 0)
//        );
//
//        // 构建各种实体的映射表，便于快速查找
//        data.teacherMap = teacherMapper.selectList(new LambdaQueryWrapper<>()).stream()
//                .collect(Collectors.toMap(Teacher::getTeacherId, t -> t));
//
//        data.classroomMap = data.allClassrooms.stream()
//                .collect(Collectors.toMap(Classroom::getClassroomId, c -> c));
//
//        data.studentGroupMap = studentGroupMapper.selectList(new LambdaQueryWrapper<>()).stream()
//                .collect(Collectors.toMap(StudentGroup::getClassId, g -> g));
//
//        data.courseMap = courseMapper.selectList(new LambdaQueryWrapper<>()).stream()
//                .collect(Collectors.toMap(Course::getCourseId, c -> c));
//
//        data.coursePartMap = coursePartMapper.selectList(new LambdaQueryWrapper<>()).stream()
//                .collect(Collectors.toMap(CoursePart::getPartId, p -> p));
//
//        data.roomTypeMap = roomTypeMapper.selectList(new LambdaQueryWrapper<>()).stream()
//                .collect(Collectors.toMap(RoomType::getRoomTypeId, rt -> rt));
//
//        data.campusMap = campusMapper.selectList(new LambdaQueryWrapper<>()).stream()
//                .collect(Collectors.toMap(Campus::getCampusId, c -> c));
//
//        data.collegeMap = collegeMapper.selectList(new LambdaQueryWrapper<>()).stream()
//                .collect(Collectors.toMap(College::getCollegeId, c -> c));
//
//        return data;
//    }
//
//    /**
//     * 使用贪心算法生成初始解
//     *
//     * @param data 排课数据
//     * @return 初始解
//     */
//    private ScheduleSolution generateGreedySolution(SchedulingData data) {
//        // 创建初始解对象
//        ScheduleSolution solution = new ScheduleSolution(data.allTasks.size());
//
//        // 根据任务的重要性和复杂度排序（优先安排更难安排的任务）
//        List<TeachingTask> sortedTasks = data.allTasks.stream()
//                .sorted(Comparator.comparingInt(this::calculateTaskPriority))
//                .collect(Collectors.toList());
//
//        // 逐个为任务安排时间地点
//        for (int i = 0; i < sortedTasks.size(); i++) {
//            TeachingTask task = sortedTasks.get(i);
//
//            // 获取该任务的可用时间槽列表
//            List<TimeSlot> availableSlots = findAvailableTimeSlots(task, solution, data);
//
//            if (!availableSlots.isEmpty()) {
//                // 选择最优的时间槽（根据约束满足程度）
//                TimeSlot bestSlot = selectBestTimeSlot(task, availableSlots, solution, data);
//
//                // 将该时间槽分配给当前任务
//                solution.assignTimeSlot(task.getTaskId(), bestSlot);
//            }
//        }
//
//        // 计算初始解的评估指标
//        solution.calculateMetrics(data);
//        return solution;
//    }
//
//    /**
//     * 计算任务的优先级（数值越大优先级越高）
//     *
//     * @param task 教学任务
//     * @return 优先级分数
//     */
//    private int calculateTaskPriority(TeachingTask task) {
//        int priority = 0;
//
//        // 基础优先级：任务的学时数（学时多的任务更难安排）
//        priority += task.getLessonsPerWeek().intValue() * 10;
//
//        // 教师特殊要求优先级：如果教师有时间限制，优先安排
//        priority += task.getSpecialRequirement().contains("time") ? 5 : 0;
//
//        // 教室类型限制优先级：如果需要特殊教室，优先安排
//        CoursePart part = getCoursePart(task.getPartId());
//        if (part != null && part.getRequiredRoomTypeId() != null) {
//            priority += 3;
//        }
//
//        return priority;
//    }
//
//    /**
//     * 查找任务的可用时间槽
//     *
//     * @param task            教学任务
//     * @param currentSolution 当前解
//     * @param data            排课数据
//     * @return 可用时间槽列表
//     */
//    private List<TimeSlot> findAvailableTimeSlots(TeachingTask task, ScheduleSolution currentSolution, SchedulingData data) {
//        List<TimeSlot> availableSlots = new ArrayList<>();
//
//        // 获取任务相关信息
//        CoursePart part = data.coursePartMap.get(task.getPartId());
//        Teacher teacher = data.teacherMap.get(task.getTeacherId());
//        StudentGroup group = data.studentGroupMap.get(task.getClassId());
//
//        // 获取教师所在学院的校区
//        College teacherCollege = data.collegeMap.get(teacher.getCollegeId());
//        Campus campus = data.campusMap.get(teacherCollege.getCampusId());
//
//        // 根据任务需求筛选合适的教室
//        List<Classroom> suitableClassrooms = data.allClassrooms.stream()
//                .filter(c -> part.getRequiredRoomTypeId() == null || c.getRoomTypeId().equals(part.getRequiredRoomTypeId()))
//                .filter(c -> c.getCapacity() >= group.getStudentCount())
//                .filter(c -> c.getCampusId().equals(campus.getCampusId()))
//                .collect(Collectors.toList());
//
//        // 遍历所有可能的时间段
//        for (int week = 1; week <= 20; week++) { // 最多20周
//            for (int day = 1; day <= 7; day++) {
//                // 根据任务的每周课时数确定可能的开始节次
//                for (int startLesson = 1; startLesson <= 12 - task.getLessonsPerWeek().intValue() + 1; startLesson++) {
//                    int endLesson = startLesson + task.getLessonsPerWeek().intValue() - 1;
//
//                    // 检查时间是否满足教师黑名单要求
//                    boolean teacherAvailable = isTeacherAvailable(teacher.getTeacherId(), week, day, startLesson, endLesson, data);
//
//                    // 检查时间是否满足活动黑名单要求
//                    boolean activityAvailable = isActivityAvailable(week, day, startLesson, endLesson, data);
//
//                    // 如果时间可用，尝试为每个合适的教室分配时间槽
//                    if (teacherAvailable && activityAvailable) {
//                        for (Classroom classroom : suitableClassrooms) {
//                            // 检查教室是否在该时间段可用
//                            if (isClassroomAvailable(classroom.getClassroomId(), week, day, startLesson, endLesson, currentSolution, data)) {
//                                // 检查教师是否在该时间段可用（基于当前解）
//                                if (isTeacherAvailableInSolution(teacher.getTeacherId(), week, day, startLesson, endLesson, currentSolution, data)) {
//                                    // 检查学生班级是否在该时间段可用（基于当前解）
//                                    if (isClassAvailableInSolution(task.getClassId(), week, day, startLesson, endLesson, currentSolution, data)) {
//                                        // 创建时间槽对象
//                                        TimeSlot slot = new TimeSlot(
//                                                task.getTaskId(),
//                                                classroom.getClassroomId(),
//                                                week,
//                                                day,
//                                                startLesson,
//                                                endLesson
//                                        );
//                                        availableSlots.add(slot);
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//
//        return availableSlots;
//    }
//
//    /**
//     * 选择最佳时间槽
//     *
//     * @param task            教学任务
//     * @param availableSlots  可用时间槽列表
//     * @param currentSolution 当前解
//     * @param data            排课数据
//     * @return 最佳时间槽
//     */
//    private TimeSlot selectBestTimeSlot(TeachingTask task, List<TimeSlot> availableSlots, ScheduleSolution currentSolution, SchedulingData data) {
//        TimeSlot bestSlot = null;
//        double bestScore = Double.MIN_VALUE;
//
//        for (TimeSlot slot : availableSlots) {
//            // 计算该时间槽的综合得分
//            double score = calculateTimeSlotScore(task, slot, currentSolution, data);
//
//            // 选择得分最高的时间槽
//            if (score > bestScore) {
//                bestScore = score;
//                bestSlot = slot;
//            }
//        }
//
//        return bestSlot;
//    }
//
//    /**
//     * 计算时间槽的综合得分
//     *
//     * @param task            教学任务
//     * @param slot            时间槽
//     * @param currentSolution 当前解
//     * @param data            排课数据
//     * @return 得分
//     */
//    private double calculateTimeSlotScore(TeachingTask task, TimeSlot slot, ScheduleSolution currentSolution, SchedulingData data) {
//        double score = 0.0;
//
//        // 基础得分：可用性
//        score += 100.0;
//
//        // 教室容量得分：容量越接近需求容量得分越高
//        Classroom classroom = data.classroomMap.get(slot.classroomId);
//        StudentGroup group = data.studentGroupMap.get(task.getClassId());
//        int capacityRatio = (int) Math.round((double) classroom.getCapacity() / group.getStudentCount() * 10);
//        score += Math.min(50, capacityRatio); // 最多50分
//
//        // 时间分布得分：避免集中在某一天
//        score += calculateTimeDistributionScore(task, slot, currentSolution, data);
//
//        // 教室使用均衡得分：优先选择使用较少的教室
//        score += calculateClassroomUsageScore(slot.classroomId, currentSolution, data);
//
//        return score;
//    }
//
//    /**
//     * 计算时间分布得分
//     *
//     * @param task            教学任务
//     * @param slot            时间槽
//     * @param currentSolution 当前解
//     * @param data            排课数据
//     * @return 时间分布得分
//     */
//    private double calculateTimeDistributionScore(TeachingTask task, TimeSlot slot, ScheduleSolution currentSolution, SchedulingData data) {
//        // 统计该任务在当前解中已安排的天数分布
//        Map<Integer, Integer> dayCount = new HashMap<>();
//        for (int i = 0; i < currentSolution.getAssignmentCount(); i++) {
//            TimeSlot existingSlot = currentSolution.getTimeSlotByIndex(i);
//            if (existingSlot != null && existingSlot.taskId.equals(task.getTaskId())) {
//                dayCount.put(existingSlot.day, dayCount.getOrDefault(existingSlot.day, 0) + 1);
//            }
//        }
//
//        // 如果该天已经安排过该任务，则降低得分
//        return dayCount.getOrDefault(slot.day, 0) > 0 ? -20 : 10;
//    }
//
//    /**
//     * 计算教室使用均衡得分
//     *
//     * @param classroomId     教室ID
//     * @param currentSolution 当前解
//     * @param data            排课数据
//     * @return 教室使用均衡得分
//     */
//    private double calculateClassroomUsageScore(Integer classroomId, ScheduleSolution currentSolution, SchedulingData data) {
//        // 统计该教室在当前解中的使用次数
//        int usageCount = 0;
//        for (int i = 0; i < currentSolution.getAssignmentCount(); i++) {
//            TimeSlot slot = currentSolution.getTimeSlotByIndex(i);
//            if (slot != null && slot.classroomId.equals(classroomId)) {
//                usageCount++;
//            }
//        }
//
//        // 使用次数越少，得分越高
//        return Math.max(0, 20 - usageCount);
//    }
//
//    /**
//     * 检查教师在指定时间段是否可用
//     *
//     * @param teacherId   教师ID
//     * @param week        周次
//     * @param day         星期
//     * @param startLesson 开始节次
//     * @param endLesson   结束节次
//     * @param data        排课数据
//     * @return 是否可用
//     */
//    private boolean isTeacherAvailable(Integer teacherId, int week, int day, int startLesson, int endLesson, SchedulingData data) {
//        // 检查教师时间黑名单
//        for (TeacherTimeWhitelist blacklist : data.teacherBlacklists) {
//            if (blacklist.getTeacherId().equals(teacherId) &&
//                    blacklist.getDayOfWeek() == day &&
//                    !(endLesson < blacklist.getLessonStart() || startLesson > blacklist.getLessonEnd())) {
//                return false;
//            }
//        }
//        return true;
//    }
//
//    /**
//     * 检查活动时间是否可用
//     *
//     * @param week        周次
//     * @param day         星期
//     * @param startLesson 开始节次
//     * @param endLesson   结束节次
//     * @param data        排课数据
//     * @return 是否可用
//     */
//    private boolean isActivityAvailable(int week, int day, int startLesson, int endLesson, SchedulingData data) {
//        // 检查活动时间黑名单
//        for (ActivityBlacklist blacklist : data.activityBlacklists) {
//            if (blacklist.getDayOfWeek() == day &&
//                    !(endLesson < blacklist.getLessonStart() || startLesson > blacklist.getLessonEnd())) {
//                return false;
//            }
//        }
//        return true;
//    }
//
//    /**
//     * 检查教室在指定时间段是否可用
//     *
//     * @param classroomId     教室ID
//     * @param week            周次
//     * @param day             星期
//     * @param startLesson     开始节次
//     * @param endLesson       结束节次
//     * @param currentSolution 当前解
//     * @param data            排课数据
//     * @return 是否可用
//     */
//    private boolean isClassroomAvailable(Integer classroomId, int week, int day, int startLesson, int endLesson, ScheduleSolution currentSolution, SchedulingData data) {
//        // 检查当前解中是否存在时间冲突
//        for (int i = 0; i < currentSolution.getAssignmentCount(); i++) {
//            TimeSlot existingSlot = currentSolution.getTimeSlotByIndex(i);
//            if (existingSlot != null &&
//                    existingSlot.classroomId.equals(classroomId) &&
//                    existingSlot.week == week &&
//                    existingSlot.day == day &&
//                    !(endLesson < existingSlot.startLesson || startLesson > existingSlot.endLesson)) {
//                return false;
//            }
//        }
//        return true;
//    }
//
//    /**
//     * 检查教师在当前解中是否可用
//     *
//     * @param teacherId       教师ID
//     * @param week            周次
//     * @param day             星期
//     * @param startLesson     开始节次
//     * @param endLesson       结束节次
//     * @param currentSolution 当前解
//     * @param data            排课数据
//     * @return 是否可用
//     */
//    private boolean isTeacherAvailableInSolution(Integer teacherId, int week, int day, int startLesson, int endLesson, ScheduleSolution currentSolution, SchedulingData data) {
//        // 获取该教师的所有任务
//        List<Integer> teacherTasks = new ArrayList<>();
//        for (TeachingTask task : data.allTasks) {
//            if (task.getTeacherId().equals(teacherId)) {
//                teacherTasks.add(task.getTaskId());
//            }
//        }
//
//        // 检查当前解中该教师是否存在时间冲突
//        for (int i = 0; i < currentSolution.getAssignmentCount(); i++) {
//            TimeSlot existingSlot = currentSolution.getTimeSlotByIndex(i);
//            if (existingSlot != null &&
//                    teacherTasks.contains(existingSlot.taskId) &&
//                    existingSlot.week == week &&
//                    existingSlot.day == day &&
//                    !(endLesson < existingSlot.startLesson || startLesson > existingSlot.endLesson)) {
//                return false;
//            }
//        }
//        return true;
//    }
//
//    /**
//     * 检查班级在当前解中是否可用
//     *
//     * @param classId         班级ID
//     * @param week            周次
//     * @param day             星期
//     * @param startLesson     开始节次
//     * @param endLesson       结束节次
//     * @param currentSolution 当前解
//     * @param data            排课数据
//     * @return 是否可用
//     */
//    private boolean isClassAvailableInSolution(Integer classId, int week, int day, int startLesson, int endLesson, ScheduleSolution currentSolution, SchedulingData data) {
//        // 获取该班级的所有任务
//        List<Integer> classTasks = new ArrayList<>();
//        for (TeachingTask task : data.allTasks) {
//            if (task.getClassId().equals(classId)) {
//                classTasks.add(task.getTaskId());
//            }
//        }
//
//        // 检查当前解中该班级是否存在时间冲突
//        for (int i = 0; i < currentSolution.getAssignmentCount(); i++) {
//            TimeSlot existingSlot = currentSolution.getTimeSlotByIndex(i);
//            if (existingSlot != null &&
//                    classTasks.contains(existingSlot.taskId) &&
//                    existingSlot.week == week &&
//                    existingSlot.day == day &&
//                    !(endLesson < existingSlot.startLesson || startLesson > existingSlot.endLesson)) {
//                return false;
//            }
//        }
//        return true;
//    }
//
//    /**
//     * 使用局部搜索优化解
//     *
//     * @param initialSolution 初始解
//     * @param data            排课数据
//     * @return 优化后的解
//     */
//    private ScheduleSolution localSearchOptimization(ScheduleSolution initialSolution, SchedulingData data) {
//        ScheduleSolution currentSolution = initialSolution.clone();
//        ScheduleSolution bestSolution = currentSolution.clone();
//
//        int noImprovementCount = 0;
//
//        // 执行局部搜索
//        for (int iteration = 0; iteration < MAX_LOCAL_SEARCH_ITERATIONS; iteration++) {
//            // 生成邻域解
//            ScheduleSolution neighbor = generateNeighborSolution(currentSolution, data);
//
//            // 计算邻域解的评估指标
//            neighbor.calculateMetrics(data);
//
//            // 如果邻域解更优，更新当前解和最佳解
//            if (neighbor.isBetterThan(bestSolution)) {
//                currentSolution = neighbor.clone();
//                bestSolution = neighbor.clone();
//                noImprovementCount = 0; // 重置无改进计数
//            } else {
//                noImprovementCount++;
//            }
//
//            // 如果长时间没有改进，提前结束搜索
//            if (noImprovementCount > 100) {
//                break;
//            }
//        }
//
//        return bestSolution;
//    }
//
//    /**
//     * 生成邻域解
//     *
//     * @param currentSolution 当前解
//     * @param data            排课数据
//     * @return 邻域解
//     */
//    private ScheduleSolution generateNeighborSolution(ScheduleSolution currentSolution, SchedulingData data) {
//        ScheduleSolution neighbor = currentSolution.clone();
//
//        // 随机选择一个操作：移动任务或交换任务
//        int operation = ThreadLocalRandom.current().nextInt(2);
//
//        if (operation == 0) {
//            // 移动任务：选择一个已安排的任务，尝试重新安排
//            List<Integer> scheduledTaskIds = neighbor.getScheduledTaskIds();
//            if (!scheduledTaskIds.isEmpty()) {
//                Integer taskId = scheduledTaskIds.get(ThreadLocalRandom.current().nextInt(scheduledTaskIds.size()));
//                TeachingTask task = getTaskById(taskId, data);
//
//                if (task != null) {
//                    // 查找该任务的新可用时间槽
//                    List<TimeSlot> availableSlots = findAvailableTimeSlots(task, neighbor, data);
//
//                    if (!availableSlots.isEmpty()) {
//                        // 选择一个新时间槽
//                        TimeSlot newSlot = availableSlots.get(ThreadLocalRandom.current().nextInt(availableSlots.size()));
//
//                        // 更新任务的安排
//                        neighbor.assignTimeSlot(taskId, newSlot);
//                    }
//                }
//            }
//        } else {
//            // 交换任务：选择两个已安排的任务，尝试交换它们的时间安排
//            List<Integer> scheduledTaskIds = neighbor.getScheduledTaskIds();
//            if (scheduledTaskIds.size() >= 2) {
//                Integer taskId1 = scheduledTaskIds.get(ThreadLocalRandom.current().nextInt(scheduledTaskIds.size()));
//                Integer taskId2 = scheduledTaskIds.get(ThreadLocalRandom.current().nextInt(scheduledTaskIds.size()));
//
//                if (!taskId1.equals(taskId2)) {
//                    TeachingTask task1 = getTaskById(taskId1, data);
//                    TeachingTask task2 = getTaskById(taskId2, data);
//
//                    if (task1 != null && task2 != null) {
//                        TimeSlot slot1 = neighbor.getTimeSlotByTaskId(taskId1);
//                        TimeSlot slot2 = neighbor.getTimeSlotByTaskId(taskId2);
//
//                        // 检查交换是否可行
//                        if (slot1 != null && slot2 != null) {
//                            // 临时移除两个任务的安排
//                            neighbor.unassignTask(taskId1);
//                            neighbor.unassignTask(taskId2);
//
//                            // 检查交换后的时间槽是否可用
//                            if (isTaskAvailableAtTimeSlot(task2, slot1, neighbor, data) &&
//                                    isTaskAvailableAtTimeSlot(task1, slot2, neighbor, data)) {
//
//                                // 执行交换
//                                neighbor.assignTimeSlot(taskId1, slot2);
//                                neighbor.assignTimeSlot(taskId2, slot1);
//                            } else {
//                                // 如果交换不可行，恢复原安排
//                                if (slot1 != null) neighbor.assignTimeSlot(taskId1, slot1);
//                                if (slot2 != null) neighbor.assignTimeSlot(taskId2, slot2);
//                            }
//                        }
//                    }
//                }
//            }
//        }
//
//        return neighbor;
//    }
//
//    /**
//     * 检查任务在指定时间槽是否可用
//     *
//     * @param task     教学任务
//     * @param slot     时间槽
//     * @param solution 排课解
//     * @param data     排课数据
//     * @return 是否可用
//     */
//    private boolean isTaskAvailableAtTimeSlot(TeachingTask task, TimeSlot slot, ScheduleSolution solution, SchedulingData data) {
//        // 检查教师是否可用
//        Teacher teacher = data.teacherMap.get(task.getTeacherId());
//        if (!isTeacherAvailableInSolution(teacher.getTeacherId(), slot.week, slot.day, slot.startLesson, slot.endLesson, solution, data)) {
//            return false;
//        }
//
//        // 检查班级是否可用
//        if (!isClassAvailableInSolution(task.getClassId(), slot.week, slot.day, slot.startLesson, slot.endLesson, solution, data)) {
//            return false;
//        }
//
//        // 检查教室是否可用
//        return isClassroomAvailable(slot.classroomId, slot.week, slot.day, slot.startLesson, slot.endLesson, solution, data);
//    }
//
//    /**
//     * 根据任务ID获取任务对象
//     *
//     * @param taskId 任务ID
//     * @param data   排课数据
//     * @return 教学任务
//     */
//    private TeachingTask getTaskById(Integer taskId, SchedulingData data) {
//        return data.allTasks.stream()
//                .filter(t -> t.getTaskId().equals(taskId))
//                .findFirst()
//                .orElse(null);
//    }
//
//    /**
//     * 根据课程部分ID获取课程部分对象
//     *
//     * @param partId 课程部分ID
//     * @return 课程部分
//     */
//    private CoursePart getCoursePart(Integer partId) {
//        // 注意：这里需要注入CoursePartMapper才能使用，为简化暂时返回null
//        return null;
//    }
//
//    /**
//     * 保存排课结果
//     *
//     * @param solution 排课解
//     * @param data     排课数据
//     */
//    private void saveScheduleResult(ScheduleSolution solution, SchedulingData data) {
//        // 遍历解中的所有安排
//        for (int i = 0; i < solution.getAssignmentCount(); i++) {
//            TimeSlot slot = solution.getTimeSlotByIndex(i);
//            if (slot != null && slot.taskId != null) {
//                // 创建排课记录
//                Schedule schedule = new Schedule();
//                schedule.setTaskId(slot.taskId);
//                schedule.setClassroomId(slot.classroomId);
//                schedule.setSemesterId(data.semesterId);
//                schedule.setWeekNumber(slot.week);
//                schedule.setDayOfWeek(slot.day);
//                schedule.setLessonStart(slot.startLesson);
//                schedule.setLessonEnd(slot.endLesson);
//                schedule.setWeekPattern(1L << (slot.week - 1)); // 使用位运算表示周次
//
//                // 插入数据库
//                scheduleMapper.insert(schedule);
//            }
//        }
//    }
//
//    /**
//     * 清除指定学期的已有排课
//     *
//     * @param semesterId 学期ID
//     */
//    private void clearExistingSchedules(Integer semesterId) {
//        scheduleMapper.delete(
//                new LambdaQueryWrapper<Schedule>()
//                        .eq(Schedule::getSemesterId, semesterId)
//        );
//    }
//
//    /**
//     * 排课数据容器类
//     */
//    private static class SchedulingData {
//        List<TeachingTask> allTasks; // 所有教学任务
//        Integer semesterId; // 学期ID
//        List<TeacherTimeWhitelist> teacherBlacklists; // 教师时间黑名单
//        List<ActivityBlacklist> activityBlacklists; // 活动时间黑名单
//        List<Classroom> allClassrooms; // 所有教室
//        Map<Integer, Teacher> teacherMap; // 教师映射表
//        Map<Integer, Classroom> classroomMap; // 教室映射表
//        Map<Integer, StudentGroup> studentGroupMap; // 学生班级映射表
//        Map<Integer, Course> courseMap; // 课程映射表
//        Map<Integer, CoursePart> coursePartMap; // 课程部分映射表
//        Map<Integer, RoomType> roomTypeMap; // 教室类型映射表
//        Map<Integer, Campus> campusMap; // 校区映射表
//        Map<Integer, College> collegeMap; // 学院映射表
//    }
//
//    /**
//     * 时间槽类
//     */
//    private static class TimeSlot implements Cloneable {
//        Integer taskId; // 任务ID
//        Integer classroomId; // 教室ID
//        Integer week; // 周次
//        Integer day; // 星期
//        Integer startLesson; // 开始节次
//        Integer endLesson; // 结束节次
//
//        public TimeSlot(Integer taskId, Integer classroomId, Integer week, Integer day, Integer startLesson, Integer endLesson) {
//            this.taskId = taskId;
//            this.classroomId = classroomId;
//            this.week = week;
//            this.day = day;
//            this.startLesson = startLesson;
//            this.endLesson = endLesson;
//        }
//
//        @Override
//        public TimeSlot clone() {
//            try {
//                return (TimeSlot) super.clone();
//            } catch (CloneNotSupportedException e) {
//                return new TimeSlot(this.taskId, this.classroomId, this.week, this.day, this.startLesson, this.endLesson);
//            }
//        }
//    }
//
//    /**
//     * 排课解类
//     */
//    private static class ScheduleSolution {
//        private List<TimeSlot> assignments; // 任务安排列表
//        private Map<Integer, Integer> taskIndexMap; // 任务ID到索引的映射
//        private double scheduleRate; // 安排率
//        private int conflictCount; // 冲突数
//
//        public ScheduleSolution(int maxTasks) {
//            this.assignments = new ArrayList<>();
//            this.taskIndexMap = new HashMap<>();
//        }
//
//        /**
//         * 为任务分配时间槽
//         *
//         * @param taskId 任务ID
//         * @param slot   时间槽
//         */
//        public void assignTimeSlot(Integer taskId, TimeSlot slot) {
//            Integer index = taskIndexMap.get(taskId);
//            if (index != null) {
//                // 如果任务已存在，更新其安排
//                assignments.set(index, slot);
//            } else {
//                // 如果任务不存在，添加新安排
//                index = assignments.size();
//                assignments.add(slot);
//                taskIndexMap.put(taskId, index);
//            }
//        }
//
//        /**
//         * 取消任务的安排
//         *
//         * @param taskId 任务ID
//         */
//        public void unassignTask(Integer taskId) {
//            Integer index = taskIndexMap.remove(taskId);
//            if (index != null) {
//                assignments.set(index, null);
//            }
//        }
//
//        /**
//         * 获取任务的安排
//         *
//         * @param taskId 任务ID
//         * @return 时间槽
//         */
//        public TimeSlot getTimeSlotByTaskId(Integer taskId) {
//            Integer index = taskIndexMap.get(taskId);
//            if (index != null && index < assignments.size()) {
//                return assignments.get(index);
//            }
//            return null;
//        }
//
//        /**
//         * 根据索引获取时间槽
//         *
//         * @param index 索引
//         * @return 时间槽
//         */
//        public TimeSlot getTimeSlotByIndex(int index) {
//            if (index >= 0 && index < assignments.size()) {
//                return assignments.get(index);
//            }
//            return null;
//        }
//
//        /**
//         * 获取安排数量
//         *
//         * @return 安排数量
//         */
//        public int getAssignmentCount() {
//            return assignments.size();
//        }
//
//        /**
//         * 获取已安排的任务ID列表
//         *
//         * @return 已安排任务ID列表
//         */
//        public List<Integer> getScheduledTaskIds() {
//            List<Integer> scheduledTaskIds = new ArrayList<>();
//            for (Map.Entry<Integer, Integer> entry : taskIndexMap.entrySet()) {
//                if (entry.getValue() < assignments.size() && assignments.get(entry.getValue()) != null) {
//                    scheduledTaskIds.add(entry.getKey());
//                }
//            }
//            return scheduledTaskIds;
//        }
//
//        /**
//         * 计算解的各项指标
//         *
//         * @param data 排课数据
//         */
//        public void calculateMetrics(SchedulingData data) {
//            int totalTasks = data.allTasks.size();
//            int scheduledTasks = 0;
//            int conflicts = 0;
//
//            // 统计已安排的任务数
//            for (int i = 0; i < assignments.size(); i++) {
//                if (assignments.get(i) != null) {
//                    scheduledTasks++;
//                }
//            }
//
//            // 计算冲突数
//            Set<String> teacherSchedule = new HashSet<>();
//            Set<String> classroomSchedule = new HashSet<>();
//            Set<String> classSchedule = new HashSet<>();
//
//            for (int i = 0; i < assignments.size(); i++) {
//                TimeSlot slot = assignments.get(i);
//                if (slot != null) {
//                    TeachingTask task = data.allTasks.stream()
//                            .filter(t -> t.getTaskId().equals(slot.taskId))
//                            .findFirst()
//                            .orElse(null);
//
//                    if (task != null) {
//                        // 检查教师冲突
//                        for (int lesson = slot.startLesson; lesson <= slot.endLesson; lesson++) {
//                            String teacherKey = "teacher_" + task.getTeacherId() + "_" + slot.week + "_" + slot.day + "_" + lesson;
//                            if (teacherSchedule.contains(teacherKey)) {
//                                conflicts++;
//                            } else {
//                                teacherSchedule.add(teacherKey);
//                            }
//
//                            String classroomKey = "classroom_" + slot.classroomId + "_" + slot.week + "_" + slot.day + "_" + lesson;
//                            if (classroomSchedule.contains(classroomKey)) {
//                                conflicts++;
//                            } else {
//                                classroomSchedule.add(classroomKey);
//                            }
//
//                            String classKey = "class_" + task.getClassId() + "_" + slot.week + "_" + slot.day + "_" + lesson;
//                            if (classSchedule.contains(classKey)) {
//                                conflicts++;
//                            } else {
//                                classSchedule.add(classKey);
//                            }
//                        }
//                    }
//                }
//            }
//
//            // 计算安排率
//            this.scheduleRate = totalTasks > 0 ? (double) scheduledTasks / totalTasks : 0.0;
//            this.conflictCount = conflicts;
//        }
//
//        /**
//         * 获取安排率
//         *
//         * @return 安排率
//         */
//        public double getScheduleRate() {
//            return this.scheduleRate;
//        }
//
//        /**
//         * 获取冲突数
//         *
//         * @return 冲突数
//         */
//        public int getConflictCount() {
//            return this.conflictCount;
//        }
//
//        /**
//         * 比较当前解是否优于另一个解
//         *
//         * @param other 另一个解
//         * @return 是否更优
//         */
//        public boolean isBetterThan(ScheduleSolution other) {
//            // 优先比较安排率，再比较冲突数
//            if (this.scheduleRate > other.scheduleRate) {
//                return true;
//            } else if (this.scheduleRate == other.scheduleRate) {
//                return this.conflictCount < other.conflictCount;
//            }
//            return false;
//        }
//
//        /**
//         * 克隆解
//         *
//         * @return 克隆的解
//         */
//        public ScheduleSolution clone() {
//            ScheduleSolution cloned = new ScheduleSolution(0);
//            cloned.assignments = new ArrayList<>();
//            for (TimeSlot slot : this.assignments) {
//                cloned.assignments.add(slot != null ? slot.clone() : null);
//            }
//            cloned.taskIndexMap = new HashMap<>(this.taskIndexMap);
//            cloned.scheduleRate = this.scheduleRate;
//            cloned.conflictCount = this.conflictCount;
//            return cloned;
//        }
//    }
//}