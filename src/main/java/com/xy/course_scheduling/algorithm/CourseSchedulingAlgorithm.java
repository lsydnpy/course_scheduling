package com.xy.course_scheduling.algorithm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xy.course_scheduling.entity.*;
import com.xy.course_scheduling.mapper.*;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 智能排课算法服务 - 支持失败课程重新排课，优化校区分配和教师跨校区冲突
 */
@Service
public class CourseSchedulingAlgorithm {

    @Resource
    private TeachingTaskMapper teachingTaskMapper;
    @Resource
    private ClassroomMapper classroomMapper;
    @Resource
    private TeacherTimeWhitelistMapper teacherTimeWhitelistMapper;
    @Resource
    private ScheduleMapper scheduleMapper;
    @Resource
    private ActivityBlacklistMapper activityBlacklistMapper;
    @Resource
    private SemesterMapper semesterMapper;
    @Resource
    private CourseMapper courseMapper;
    @Resource
    private TeacherMapper teacherMapper;
    @Resource
    private StudentGroupMapper studentGroupMapper;
    @Resource
    private CampusMapper campusMapper;
    @Resource
    private RoomTypeMapper roomTypeMapper;
    @Resource
    private CoursePartMapper coursePartMapper;

    /**
     * 排课算法核心方法 - 支持失败课程重新排课
     */
    public SchedulingResult generateSchedule(Integer semesterId) {
        try {
            // 获取所有待排课的教学任务
            List<TeachingTask> teachingTasks = teachingTaskMapper.selectList(
                    new LambdaQueryWrapper<TeachingTask>()
                            .eq(TeachingTask::getSemesterId, semesterId)
                            .eq(TeachingTask::getStatus, "CONFIRMED")
            );

            if (teachingTasks.isEmpty()) {
                return new SchedulingResult(SchedulingStatus.SUCCESS,
                        new ArrayList<>(), new ArrayList<>(), "没有需要排课的任务");
            }

            // 加载关联数据
            teachingTasks.forEach(task -> {
                task.setSemester(semesterMapper.selectById(task.getSemesterId()));
                task.setCourse(courseMapper.selectById(task.getCourseId()));
                task.setTeacher(teacherMapper.selectById(task.getTeacherId()));
                task.setStudentGroup(studentGroupMapper.selectById(task.getClassId()));
                task.setCampus(campusMapper.selectById(task.getCampusId()));
                task.setCoursePart(coursePartMapper.selectById(task.getPartId()));
                if (task.getCoursePart() != null) {
                    task.getCoursePart().setRequiredRoomType(
                            roomTypeMapper.selectById(task.getCoursePart().getRequiredRoomTypeId()));
                }
            });

            // 获取相关数据
            List<Classroom> classrooms = classroomMapper.selectList(
                    new LambdaQueryWrapper<Classroom>().eq(Classroom::getIsAvailable, true)
            );

            List<TeacherTimeWhitelist> teacherPreferences = teacherTimeWhitelistMapper
                    .selectList(new LambdaQueryWrapper<TeacherTimeWhitelist>()
                            .eq(TeacherTimeWhitelist::getSemesterId, semesterId));

            List<ActivityBlacklist> activityBlacklists = activityBlacklistMapper
                    .selectList(new LambdaQueryWrapper<ActivityBlacklist>()
                            .eq(ActivityBlacklist::getSemesterId, semesterId));

            Semester semester = teachingTasks.get(0).getSemester();

            // 获取现有的排课记录，用于冲突检测
            List<Schedule> existingSchedules = scheduleMapper.selectList(
                    new LambdaQueryWrapper<Schedule>().eq(Schedule::getSemesterId, semesterId)
            );

            // 初始化排课结果
            List<Schedule> scheduledClasses = new ArrayList<>();
            List<TeachingTask> failedTasks = new ArrayList<>();

            // 按教师分组，将同一教师的所有任务归类
            Map<Integer, List<TeachingTask>> teacherGroups = teachingTasks.stream()
                    .collect(Collectors.groupingBy(TeachingTask::getTeacherId));

            // 对每个教师的任务进行排序（优先安排难以排课的课程）
            List<TeachingTask> sortedTasks = new ArrayList<>();
            for (List<TeachingTask> teacherTasks : teacherGroups.values()) {
                // 按优先级排序
                teacherTasks.sort((t1, t2) -> {
                    int priority1 = calculatePriority(t1, teacherPreferences);
                    int priority2 = calculatePriority(t2, teacherPreferences);
                    return Integer.compare(priority2, priority1);
                });
                sortedTasks.addAll(teacherTasks);
            }

            // 逐个为每个教学任务排课
            for (TeachingTask task : sortedTasks) {
                List<Schedule> taskSchedules = scheduleTask(task, semester, classrooms,
                        teacherPreferences, activityBlacklists, scheduledClasses, teachingTasks);

                if (!taskSchedules.isEmpty()) {
                    scheduledClasses.addAll(taskSchedules);
                } else {
                    failedTasks.add(task);
                }
            }

            // 对失败的任务进行重新排课尝试
            List<TeachingTask> retryFailedTasks = new ArrayList<>();
            if (!failedTasks.isEmpty()) {
                System.out.println("第一次排课完成，有 " + failedTasks.size() + " 门课程排课失败，开始重新尝试排课...");

                // 尝试重新排课
                for (int retryCount = 1; retryCount <= 3; retryCount++) {
                    System.out.println("第 " + retryCount + " 次重新排课尝试...");

                    List<TeachingTask> currentRetryFailed = new ArrayList<>();
                    for (TeachingTask failedTask : failedTasks) {
                        List<Schedule> retrySchedules = scheduleTask(failedTask, semester, classrooms,
                                teacherPreferences, activityBlacklists, scheduledClasses, teachingTasks);

                        if (!retrySchedules.isEmpty()) {
                            scheduledClasses.addAll(retrySchedules);
                            System.out.println("任务 " + failedTask.getTaskId() + " 重新排课成功！");
                        } else {
                            currentRetryFailed.add(failedTask);
                        }
                    }

                    if (currentRetryFailed.isEmpty()) {
                        // 所有失败的任务都已重新排课成功
                        failedTasks.clear();
                        break;
                    } else {
                        // 更新失败列表，继续下一轮尝试
                        failedTasks = currentRetryFailed;

                        // 如果还有失败的任务，随机打乱失败任务的顺序，再次尝试
                        Collections.shuffle(failedTasks);
                    }
                }

                // 如果仍有失败的任务，尝试放宽约束
                if (!failedTasks.isEmpty()) {
                    System.out.println("仍有 " + failedTasks.size() + " 门课程排课失败，尝试放宽约束条件...");

                    for (TeachingTask failedTask : failedTasks) {
                        List<Schedule> relaxedSchedules = scheduleTaskWithRelaxedConstraints(
                                failedTask, semester, classrooms, teacherPreferences,
                                activityBlacklists, scheduledClasses, teachingTasks);

                        if (!relaxedSchedules.isEmpty()) {
                            scheduledClasses.addAll(relaxedSchedules);
                            System.out.println("任务 " + failedTask.getTaskId() + " 放宽约束后排课成功！");
                        } else {
                            retryFailedTasks.add(failedTask);
                        }
                    }
                } else {
                    retryFailedTasks.addAll(failedTasks);
                }

                // 如果仍有失败的任务，尝试拆分课时排课（4 节拆成 2+2，6 节拆成 2+2+2/2+4/4+2）
                if (!retryFailedTasks.isEmpty()) {
                    System.out.println("仍有 " + retryFailedTasks.size() + " 门课程排课失败，尝试拆分课时排课...");

                    List<TeachingTask> splitFailedTasks = new ArrayList<>();
                    for (TeachingTask failedTask : retryFailedTasks) {
                        List<Schedule> splitSchedules = scheduleTaskWithSplitting(
                                failedTask, semester, classrooms, teacherPreferences,
                                activityBlacklists, scheduledClasses, teachingTasks);

                        if (!splitSchedules.isEmpty()) {
                            scheduledClasses.addAll(splitSchedules);
                            System.out.println("任务 " + failedTask.getTaskId() + " 拆分课时后排课成功！");
                        } else {
                            splitFailedTasks.add(failedTask);
                        }
                    }
                    retryFailedTasks = splitFailedTasks;
                }
            }

            System.out.println("最终排课总数是：" + scheduledClasses.size());

            // 保存排课结果
            for (Schedule schedule : scheduledClasses) {
                schedule.setScheduleId(null); // 清除ID让数据库自动生成
                scheduleMapper.insert(schedule);
            }

            // 统计结果
            String message = String.format("成功排课%d门，失败%d门",
                    scheduledClasses.size(), retryFailedTasks.size());

            SchedulingStatus status = retryFailedTasks.isEmpty() ?
                    SchedulingStatus.SUCCESS :
                    (scheduledClasses.isEmpty() ? SchedulingStatus.FAILED : SchedulingStatus.PARTIAL_SUCCESS);

            return new SchedulingResult(status, scheduledClasses, retryFailedTasks, message);

        } catch (Exception e) {
            e.printStackTrace();
            return new SchedulingResult(SchedulingStatus.FAILED, new ArrayList<>(),
                    new ArrayList<>(), "排课过程中发生错误: " + e.getMessage());
        }
    }

    /**
     * 计算任务排课优先级
     */
    private int calculatePriority(TeachingTask task, List<TeacherTimeWhitelist> preferences) {
        int priority = 0;

        // 教师有时间偏好的任务优先级更高
        for (TeacherTimeWhitelist pref : preferences) {
            if (pref.getTeacherId().equals(task.getTeacherId())) {
                priority += 10; // 有时间偏好的任务优先级高
                break;
            }
        }

        // 实践课优先级更高（教室类型限制更多）
        if (task.getCoursePart() != null && task.getCoursePart().getRequiredRoomType() != null &&
                ("PRACTICE".equals(task.getCoursePart().getRequiredRoomType().getTypeName()) ||
                        task.getCoursePart().getRequiredRoomType().getTypeName().contains("实验") ||
                        task.getCoursePart().getRequiredRoomType().getTypeName().contains("实训"))) {
            priority += 5;
        }

        // 课时数较多的任务优先级更高
        priority += task.getTotalLessons() / 10;

        return priority;
    }

    /**
     * 为单个教学任务排课
     */
    private List<Schedule> scheduleTask(TeachingTask task, Semester semester,
                                        List<Classroom> allClassrooms,
                                        List<TeacherTimeWhitelist> teacherPreferences,
                                        List<ActivityBlacklist> activityBlacklists,
                                        List<Schedule> existingSchedules,
                                        List<TeachingTask> allTeachingTasks) {

        List<Schedule> schedules = new ArrayList<>();

        // 计算课程所需的总课时和周课时
        int totalLessons = task.getTotalLessons();
        BigDecimal lessonsPerWeek = task.getLessonsPerWeek();
        int weeks = semester.getTotalWeeks();

        // 确定排课策略（单双周、连续等）基于week_schedule_pattern字段
        SchedulingStrategy strategy = determineSchedulingStrategy(task, lessonsPerWeek);

        // 获取合适的教室列表 - 现在根据任务的校区进行筛选
        List<Classroom> suitableClassrooms = findSuitableClassrooms(
                task.getCoursePart() != null ? task.getCoursePart().getRequiredRoomType() : null,
                task.getStudentGroup(),
                allClassrooms,
                task.getCampusId() // 按任务指定的校区筛选
        );

        if (suitableClassrooms.isEmpty()) {
            System.out.println("无法找到合适的教室为任务: " + task.getTaskId() + " 在校区: " + task.getCampusId());
            return schedules;
        }

        // 按策略生成排课方案
        switch (strategy.type()) {
            case CONTINUOUS:
                schedules = scheduleConsistent(task, semester, suitableClassrooms,
                        teacherPreferences, activityBlacklists, existingSchedules,
                        allTeachingTasks, strategy);
                break;
            case ALTERNATING:
                schedules = scheduleAlternatingConsistent(task, semester, suitableClassrooms,
                        teacherPreferences, activityBlacklists, existingSchedules,
                        allTeachingTasks, strategy);
                break;
            case FLEXIBLE:
                schedules = scheduleFlexibleConsistent(task, semester, suitableClassrooms,
                        teacherPreferences, activityBlacklists, existingSchedules,
                        allTeachingTasks, strategy);
                break;
        }

        return schedules;
    }

    /**
     * 使用放宽约束的策略为单个教学任务排课
     */
    private List<Schedule> scheduleTaskWithRelaxedConstraints(TeachingTask task, Semester semester,
                                                              List<Classroom> allClassrooms,
                                                              List<TeacherTimeWhitelist> teacherPreferences,
                                                              List<ActivityBlacklist> activityBlacklists,
                                                              List<Schedule> existingSchedules,
                                                              List<TeachingTask> allTeachingTasks) {

        List<Schedule> schedules = new ArrayList<>();

        // 计算课程所需的总课时和周课时
        int totalLessons = task.getTotalLessons();
        BigDecimal lessonsPerWeek = task.getLessonsPerWeek();

        // 确定排课策略（单双周、连续等）基于week_schedule_pattern字段
        SchedulingStrategy strategy = determineSchedulingStrategy(task, lessonsPerWeek);

        // 尝试放宽约束条件，允许使用更大容量的教室
        List<Classroom> suitableClassrooms = findSuitableClassroomsWithRelaxedCapacity(
                task.getCoursePart() != null ? task.getCoursePart().getRequiredRoomType() : null,
                task.getStudentGroup(),
                allClassrooms,
                task.getCampusId() // 按任务指定的校区筛选
        );

        if (suitableClassrooms.isEmpty()) {
            System.out.println("即使放宽容量限制也无法找到合适的教室为任务: " + task.getTaskId() + " 在校区: " + task.getCampusId());
            return schedules;
        }

        // 按策略生成排课方案，但忽略教师时间偏好（放宽约束）
        switch (strategy.type()) {
            case CONTINUOUS:
                schedules = scheduleConsistentWithRelaxedConstraints(task, semester, suitableClassrooms,
                        teacherPreferences, activityBlacklists, existingSchedules,
                        allTeachingTasks, strategy);
                break;
            case ALTERNATING:
                schedules = scheduleAlternatingConsistentWithRelaxedConstraints(task, semester, suitableClassrooms,
                        teacherPreferences, activityBlacklists, existingSchedules,
                        allTeachingTasks, strategy);
                break;
            case FLEXIBLE:
                schedules = scheduleFlexibleConsistentWithRelaxedConstraints(task, semester, suitableClassrooms,
                        teacherPreferences, activityBlacklists, existingSchedules,
                        allTeachingTasks, strategy);
                break;
        }

        return schedules;
    }

    /**
     * 使用拆分课时策略为单个教学任务排课
     * 支持 4 节课拆成 2+2，6 节课拆成 2+2+2/2+4/4+2
     */
    private List<Schedule> scheduleTaskWithSplitting(TeachingTask task, Semester semester,
                                                      List<Classroom> allClassrooms,
                                                      List<TeacherTimeWhitelist> teacherPreferences,
                                                      List<ActivityBlacklist> activityBlacklists,
                                                      List<Schedule> existingSchedules,
                                                      List<TeachingTask> allTeachingTasks) {

        List<Schedule> schedules = new ArrayList<>();
        int totalLessons = task.getTotalLessons();
        int lessonsPerWeek = task.getLessonsPerWeek().intValue();

        // 获取合适的教室列表
        List<Classroom> suitableClassrooms = findSuitableClassrooms(
                task.getCoursePart() != null ? task.getCoursePart().getRequiredRoomType() : null,
                task.getStudentGroup(),
                allClassrooms,
                task.getCampusId()
        );

        if (suitableClassrooms.isEmpty()) {
            System.out.println("无法找到合适的教室为任务：" + task.getTaskId());
            return schedules;
        }

        // 生成拆分策略
        List<List<Integer>> splitStrategies = generateSplitStrategies(lessonsPerWeek);

        // 尝试每种拆分策略
        for (List<Integer> splitStrategy : splitStrategies) {
            System.out.println("尝试拆分策略：" + splitStrategy + " 为任务 " + task.getTaskId());

            List<Schedule> trySchedules = trySplitSchedule(task, semester, suitableClassrooms,
                    teacherPreferences, activityBlacklists, existingSchedules, allTeachingTasks,
                    splitStrategy, totalLessons);

            if (!trySchedules.isEmpty()) {
                schedules.addAll(trySchedules);
                System.out.println("拆分策略 " + splitStrategy + " 排课成功！");
                break; // 找到可行方案后退出
            }
        }

        return schedules;
    }

    /**
     * 生成课时拆分策略
     * @param lessonsPerWeek 每周课时数
     * @return 拆分策略列表，每个策略是一个课时分配列表
     */
    private List<List<Integer>> generateSplitStrategies(int lessonsPerWeek) {
        List<List<Integer>> strategies = new ArrayList<>();

        if (lessonsPerWeek == 4) {
            // 4 节课：拆成 2+2
            strategies.add(Arrays.asList(2, 2));
        } else if (lessonsPerWeek == 6) {
            // 6 节课：拆成 2+2+2, 2+4, 4+2
            strategies.add(Arrays.asList(2, 2, 2));
            strategies.add(Arrays.asList(2, 4));
            strategies.add(Arrays.asList(4, 2));
        } else if (lessonsPerWeek == 8) {
            // 8 节课：拆成 2+2+2+2, 4+4, 2+2+4, 4+2+2
            strategies.add(Arrays.asList(2, 2, 2, 2));
            strategies.add(Arrays.asList(4, 4));
            strategies.add(Arrays.asList(2, 2, 4));
            strategies.add(Arrays.asList(4, 2, 2));
        } else if (lessonsPerWeek >= 3) {
            // 其他情况，尝试拆分成 2 节课的组合
            List<Integer> splitBy2 = new ArrayList<>();
            int remaining = lessonsPerWeek;
            while (remaining > 0) {
                if (remaining >= 2) {
                    splitBy2.add(2);
                    remaining -= 2;
                } else {
                    // 如果最后剩 1 节，加到最后一个 2 上变成 3
                    if (!splitBy2.isEmpty()) {
                        int lastIndex = splitBy2.size() - 1;
                        splitBy2.set(lastIndex, splitBy2.get(lastIndex) + 1);
                    } else {
                        splitBy2.add(1);
                    }
                    remaining = 0;
                }
            }
            strategies.add(splitBy2);
        }

        return strategies;
    }

    /**
     * 尝试使用拆分策略排课
     */
    private List<Schedule> trySplitSchedule(TeachingTask task, Semester semester,
                                             List<Classroom> suitableClassrooms,
                                             List<TeacherTimeWhitelist> teacherPreferences,
                                             List<ActivityBlacklist> activityBlacklists,
                                             List<Schedule> existingSchedules,
                                             List<TeachingTask> allTeachingTasks,
                                             List<Integer> splitStrategy,
                                             int totalLessons) {

        List<Schedule> schedules = new ArrayList<>();
        int scheduledLessons = 0;
        int week = 1;
        int splitIndex = 0;

        // 临时记录已安排的课程，用于冲突检测
        List<Schedule> tempSchedules = new ArrayList<>(existingSchedules);

        while (scheduledLessons < totalLessons && week <= semester.getTotalWeeks()) {
            // 获取当前周的课时数（循环使用拆分策略）
            int lessonsThisWeek = splitStrategy.get(splitIndex % splitStrategy.size());

            // 检查本周是否能安排这么多课时
            if (scheduledLessons + lessonsThisWeek > totalLessons) {
                lessonsThisWeek = totalLessons - scheduledLessons;
            }

            // 尝试为本周安排课程
            Schedule weekSchedule = findFirstSession(task, semester, suitableClassrooms,
                    teacherPreferences, activityBlacklists, tempSchedules, allTeachingTasks,
                    lessonsThisWeek);

            if (weekSchedule != null) {
                schedules.add(weekSchedule);
                tempSchedules.add(weekSchedule);
                scheduledLessons += lessonsThisWeek;
            }

            splitIndex++;
            week++;
        }

        // 检查是否成功安排了所有课时
        if (scheduledLessons >= totalLessons) {
            return schedules;
        } else {
            return new ArrayList<>(); // 排课失败，返回空列表
        }
    }

    /**
     * 一致排课策略 - 确保每周安排相同
     */
    private List<Schedule> scheduleConsistent(TeachingTask task, Semester semester,
                                              List<Classroom> suitableClassrooms,
                                              List<TeacherTimeWhitelist> teacherPreferences,
                                              List<ActivityBlacklist> activityBlacklists,
                                              List<Schedule> existingSchedules,
                                              List<TeachingTask> allTeachingTasks,
                                              SchedulingStrategy strategy) {

        List<Schedule> schedules = new ArrayList<>();
        int totalLessons = task.getTotalLessons();
        int lessonsPerSession = strategy.lessonsPerSession();

        // 首先找到一个合适的时间和地点安排
        Schedule firstSession = findFirstSession(task, semester, suitableClassrooms,
                teacherPreferences, activityBlacklists, existingSchedules, allTeachingTasks,
                lessonsPerSession);

        if (firstSession == null) {
            System.out.println("无法为任务 " + task.getTaskId() + " 找到合适的时间和教室");
            return schedules;
        }

        // 确定每周都使用相同的时间和地点
        int dayOfWeek = firstSession.getDayOfWeek();
        int lessonStart = firstSession.getLessonStart();
        int lessonEnd = firstSession.getLessonEnd();
        Integer classroomId = firstSession.getClassroomId();

        // 计算总共需要安排多少个会话
        int totalSessions = (int) Math.ceil((double) totalLessons / lessonsPerSession);

        // 为每一周安排相同的课程
        int scheduledLessons = 0;
        for (int week = 1; week <= semester.getTotalWeeks() && scheduledLessons < totalLessons; week++) {
            // 检查这个时间是否与其他已安排的课程冲突（包括同一教师的其他任务）
            if (checkTimeSlotNotConflicting(task, classroomId, dayOfWeek, lessonStart, lessonEnd,
                    week, existingSchedules, allTeachingTasks)) {

                Schedule schedule = createScheduleWithDetails(task, classroomId, semester,
                        dayOfWeek, lessonStart, lessonEnd, week);
                schedules.add(schedule);
                scheduledLessons += lessonsPerSession;
            }
        }

        return scheduledLessons >= totalLessons ? schedules : new ArrayList<>();
    }

    /**
     * 一致排课策略 - 放宽约束
     */
    private List<Schedule> scheduleConsistentWithRelaxedConstraints(TeachingTask task, Semester semester,
                                                                    List<Classroom> suitableClassrooms,
                                                                    List<TeacherTimeWhitelist> teacherPreferences,
                                                                    List<ActivityBlacklist> activityBlacklists,
                                                                    List<Schedule> existingSchedules,
                                                                    List<TeachingTask> allTeachingTasks,
                                                                    SchedulingStrategy strategy) {

        List<Schedule> schedules = new ArrayList<>();
        int totalLessons = task.getTotalLessons();
        int lessonsPerSession = strategy.lessonsPerSession();

        // 首先找到一个合适的时间和地点安排（忽略教师偏好）
        Schedule firstSession = findFirstSessionWithRelaxedConstraints(task, semester, suitableClassrooms,
                teacherPreferences, activityBlacklists, existingSchedules, allTeachingTasks,
                lessonsPerSession);

        if (firstSession == null) {
            System.out.println("即使放宽约束也无法为任务 " + task.getTaskId() + " 找到合适的时间和教室");
            return schedules;
        }

        // 确定每周都使用相同的时间和地点
        int dayOfWeek = firstSession.getDayOfWeek();
        int lessonStart = firstSession.getLessonStart();
        int lessonEnd = firstSession.getLessonEnd();
        Integer classroomId = firstSession.getClassroomId();

        // 计算总共需要安排多少个会话
        int totalSessions = (int) Math.ceil((double) totalLessons / lessonsPerSession);

        // 为每一周安排相同的课程
        int scheduledLessons = 0;
        for (int week = 1; week <= semester.getTotalWeeks() && scheduledLessons < totalLessons; week++) {
            // 检查这个时间是否与其他已安排的课程冲突（包括同一教师的其他任务）
            if (checkTimeSlotNotConflicting(task, classroomId, dayOfWeek, lessonStart, lessonEnd,
                    week, existingSchedules, allTeachingTasks)) {

                Schedule schedule = createScheduleWithDetails(task, classroomId, semester,
                        dayOfWeek, lessonStart, lessonEnd, week);
                schedules.add(schedule);
                scheduledLessons += lessonsPerSession;
            }
        }

        return scheduledLessons >= totalLessons ? schedules : new ArrayList<>();
    }

    /**
     * 一致交替排课策略 - 单双周安排固定
     */
    private List<Schedule> scheduleAlternatingConsistent(TeachingTask task, Semester semester,
                                                         List<Classroom> suitableClassrooms,
                                                         List<TeacherTimeWhitelist> teacherPreferences,
                                                         List<ActivityBlacklist> activityBlacklists,
                                                         List<Schedule> existingSchedules,
                                                         List<TeachingTask> allTeachingTasks,
                                                         SchedulingStrategy strategy) {

        List<Schedule> schedules = new ArrayList<>();
        int totalLessons = task.getTotalLessons();
        int oddWeekLessons = strategy.oddWeekLessons();
        int evenWeekLessons = strategy.evenWeekLessons();

        // 为单周找一个时间安排
        Schedule oddWeekSession = null;
        if (oddWeekLessons > 0) {
            oddWeekSession = findFirstSession(task, semester, suitableClassrooms,
                    teacherPreferences, activityBlacklists, existingSchedules, allTeachingTasks,
                    oddWeekLessons);
        }

        // 为双周找一个时间安排
        Schedule evenWeekSession = null;
        if (evenWeekLessons > 0) {
            // 创建临时的已安排列表，包含单周安排（如果存在）
            List<Schedule> tempSchedules = new ArrayList<>(existingSchedules);
            if (oddWeekSession != null) {
                tempSchedules.add(oddWeekSession);
            }

            evenWeekSession = findFirstSession(task, semester, suitableClassrooms,
                    teacherPreferences, activityBlacklists, tempSchedules, allTeachingTasks,
                    evenWeekLessons);
        }

        if (oddWeekSession == null && evenWeekSession == null) {
            System.out.println("无法为任务 " + task.getTaskId() + " 找到单双周的合适时间");
            return schedules;
        }

        // 根据单双周安排计算课程分布
        int scheduledLessons = 0;
        for (int week = 1; week <= semester.getTotalWeeks(); week++) {
            if (scheduledLessons >= totalLessons) break;

            if (week % 2 == 1 && oddWeekSession != null) { // 单周
                if (scheduledLessons + oddWeekLessons <= totalLessons) {
                    // 检查这个时间是否与其他已安排的课程冲突（包括同一教师的其他任务）
                    if (checkTimeSlotNotConflicting(task, oddWeekSession.getClassroomId(),
                            oddWeekSession.getDayOfWeek(), oddWeekSession.getLessonStart(),
                            oddWeekSession.getLessonEnd(), week, existingSchedules, allTeachingTasks)) {

                        Schedule schedule = createScheduleWithDetails(task, oddWeekSession.getClassroomId(),
                                semester, oddWeekSession.getDayOfWeek(), oddWeekSession.getLessonStart(),
                                oddWeekSession.getLessonEnd(), week);
                        schedules.add(schedule);
                        scheduledLessons += oddWeekLessons;
                    }
                }
            } else if (week % 2 == 0 && evenWeekSession != null) { // 双周
                if (scheduledLessons + evenWeekLessons <= totalLessons) {
                    // 检查这个时间是否与其他已安排的课程冲突（包括同一教师的其他任务）
                    if (checkTimeSlotNotConflicting(task, evenWeekSession.getClassroomId(),
                            evenWeekSession.getDayOfWeek(), evenWeekSession.getLessonStart(),
                            evenWeekSession.getLessonEnd(), week, existingSchedules, allTeachingTasks)) {

                        Schedule schedule = createScheduleWithDetails(task, evenWeekSession.getClassroomId(),
                                semester, evenWeekSession.getDayOfWeek(), evenWeekSession.getLessonStart(),
                                evenWeekSession.getLessonEnd(), week);
                        schedules.add(schedule);
                        scheduledLessons += evenWeekLessons;
                    }
                }
            }
        }

        return scheduledLessons >= totalLessons ? schedules : new ArrayList<>();
    }

    /**
     * 一致交替排课策略 - 放宽约束
     */
    private List<Schedule> scheduleAlternatingConsistentWithRelaxedConstraints(TeachingTask task, Semester semester,
                                                                               List<Classroom> suitableClassrooms,
                                                                               List<TeacherTimeWhitelist> teacherPreferences,
                                                                               List<ActivityBlacklist> activityBlacklists,
                                                                               List<Schedule> existingSchedules,
                                                                               List<TeachingTask> allTeachingTasks,
                                                                               SchedulingStrategy strategy) {

        List<Schedule> schedules = new ArrayList<>();
        int totalLessons = task.getTotalLessons();
        int oddWeekLessons = strategy.oddWeekLessons();
        int evenWeekLessons = strategy.evenWeekLessons();

        // 为单周找一个时间安排（忽略教师偏好）
        Schedule oddWeekSession = null;
        if (oddWeekLessons > 0) {
            oddWeekSession = findFirstSessionWithRelaxedConstraints(task, semester, suitableClassrooms,
                    teacherPreferences, activityBlacklists, existingSchedules, allTeachingTasks,
                    oddWeekLessons);
        }

        // 为双周找一个时间安排（忽略教师偏好）
        Schedule evenWeekSession = null;
        if (evenWeekLessons > 0) {
            // 创建临时的已安排列表，包含单周安排（如果存在）
            List<Schedule> tempSchedules = new ArrayList<>(existingSchedules);
            if (oddWeekSession != null) {
                tempSchedules.add(oddWeekSession);
            }

            evenWeekSession = findFirstSessionWithRelaxedConstraints(task, semester, suitableClassrooms,
                    teacherPreferences, activityBlacklists, tempSchedules, allTeachingTasks,
                    evenWeekLessons);
        }

        if (oddWeekSession == null && evenWeekSession == null) {
            System.out.println("即使放宽约束也无法为任务 " + task.getTaskId() + " 找到单双周的合适时间");
            return schedules;
        }

        // 根据单双周安排计算课程分布
        int scheduledLessons = 0;
        for (int week = 1; week <= semester.getTotalWeeks(); week++) {
            if (scheduledLessons >= totalLessons) break;

            if (week % 2 == 1 && oddWeekSession != null) { // 单周
                if (scheduledLessons + oddWeekLessons <= totalLessons) {
                    // 检查这个时间是否与其他已安排的课程冲突（包括同一教师的其他任务）
                    if (checkTimeSlotNotConflicting(task, oddWeekSession.getClassroomId(),
                            oddWeekSession.getDayOfWeek(), oddWeekSession.getLessonStart(),
                            oddWeekSession.getLessonEnd(), week, existingSchedules, allTeachingTasks)) {

                        Schedule schedule = createScheduleWithDetails(task, oddWeekSession.getClassroomId(),
                                semester, oddWeekSession.getDayOfWeek(), oddWeekSession.getLessonStart(),
                                oddWeekSession.getLessonEnd(), week);
                        schedules.add(schedule);
                        scheduledLessons += oddWeekLessons;
                    }
                }
            } else if (week % 2 == 0 && evenWeekSession != null) { // 双周
                if (scheduledLessons + evenWeekLessons <= totalLessons) {
                    // 检查这个时间是否与其他已安排的课程冲突（包括同一教师的其他任务）
                    if (checkTimeSlotNotConflicting(task, evenWeekSession.getClassroomId(),
                            evenWeekSession.getDayOfWeek(), evenWeekSession.getLessonStart(),
                            evenWeekSession.getLessonEnd(), week, existingSchedules, allTeachingTasks)) {

                        Schedule schedule = createScheduleWithDetails(task, evenWeekSession.getClassroomId(),
                                semester, evenWeekSession.getDayOfWeek(), evenWeekSession.getLessonStart(),
                                evenWeekSession.getLessonEnd(), week);
                        schedules.add(schedule);
                        scheduledLessons += evenWeekLessons;
                    }
                }
            }
        }

        return scheduledLessons >= totalLessons ? schedules : new ArrayList<>();
    }

    /**
     * 一致灵活排课策略
     */
    private List<Schedule> scheduleFlexibleConsistent(TeachingTask task, Semester semester,
                                                      List<Classroom> suitableClassrooms,
                                                      List<TeacherTimeWhitelist> teacherPreferences,
                                                      List<ActivityBlacklist> activityBlacklists,
                                                      List<Schedule> existingSchedules,
                                                      List<TeachingTask> allTeachingTasks,
                                                      SchedulingStrategy strategy) {

        List<Schedule> schedules = new ArrayList<>();
        int totalLessons = task.getTotalLessons();
        int lessonsPerSession = strategy.lessonsPerSession();

        // 首先找到一个合适的时间和地点安排
        Schedule firstSession = findFirstSession(task, semester, suitableClassrooms,
                teacherPreferences, activityBlacklists, existingSchedules, allTeachingTasks,
                lessonsPerSession);

        if (firstSession == null) {
            System.out.println("无法为任务 " + task.getTaskId() + " 找到合适的时间和教室");
            return schedules;
        }

        // 确定每周都使用相同的时间和地点
        int dayOfWeek = firstSession.getDayOfWeek();
        int lessonStart = firstSession.getLessonStart();
        int lessonEnd = firstSession.getLessonEnd();
        Integer classroomId = firstSession.getClassroomId();

        // 计算总共需要安排多少个会话
        int totalSessions = (int) Math.ceil((double) totalLessons / lessonsPerSession);

        // 为每一周安排相同的课程
        int scheduledLessons = 0;
        for (int week = 1; week <= semester.getTotalWeeks() && scheduledLessons < totalLessons; week++) {
            // 检查这个时间是否与其他已安排的课程冲突（包括同一教师的其他任务）
            if (checkTimeSlotNotConflicting(task, classroomId, dayOfWeek, lessonStart, lessonEnd,
                    week, existingSchedules, allTeachingTasks)) {

                Schedule schedule = createScheduleWithDetails(task, classroomId, semester,
                        dayOfWeek, lessonStart, lessonEnd, week);
                schedules.add(schedule);
                scheduledLessons += lessonsPerSession;
            }
        }

        return scheduledLessons >= totalLessons ? schedules : new ArrayList<>();
    }

    /**
     * 一致灵活排课策略 - 放宽约束
     */
    private List<Schedule> scheduleFlexibleConsistentWithRelaxedConstraints(TeachingTask task, Semester semester,
                                                                            List<Classroom> suitableClassrooms,
                                                                            List<TeacherTimeWhitelist> teacherPreferences,
                                                                            List<ActivityBlacklist> activityBlacklists,
                                                                            List<Schedule> existingSchedules,
                                                                            List<TeachingTask> allTeachingTasks,
                                                                            SchedulingStrategy strategy) {

        List<Schedule> schedules = new ArrayList<>();
        int totalLessons = task.getTotalLessons();
        int lessonsPerSession = strategy.lessonsPerSession();

        // 首先找到一个合适的时间和地点安排（忽略教师偏好）
        Schedule firstSession = findFirstSessionWithRelaxedConstraints(task, semester, suitableClassrooms,
                teacherPreferences, activityBlacklists, existingSchedules, allTeachingTasks,
                lessonsPerSession);

        if (firstSession == null) {
            System.out.println("即使放宽约束也无法为任务 " + task.getTaskId() + " 找到合适的时间和教室");
            return schedules;
        }

        // 确定每周都使用相同的时间和地点
        int dayOfWeek = firstSession.getDayOfWeek();
        int lessonStart = firstSession.getLessonStart();
        int lessonEnd = firstSession.getLessonEnd();
        Integer classroomId = firstSession.getClassroomId();

        // 计算总共需要安排多少个会话
        int totalSessions = (int) Math.ceil((double) totalLessons / lessonsPerSession);

        // 为每一周安排相同的课程
        int scheduledLessons = 0;
        for (int week = 1; week <= semester.getTotalWeeks() && scheduledLessons < totalLessons; week++) {
            // 检查这个时间是否与其他已安排的课程冲突（包括同一教师的其他任务）
            if (checkTimeSlotNotConflicting(task, classroomId, dayOfWeek, lessonStart, lessonEnd,
                    week, existingSchedules, allTeachingTasks)) {

                Schedule schedule = createScheduleWithDetails(task, classroomId, semester,
                        dayOfWeek, lessonStart, lessonEnd, week);
                schedules.add(schedule);
                scheduledLessons += lessonsPerSession;
            }
        }

        return scheduledLessons >= totalLessons ? schedules : new ArrayList<>();
    }

    /**
     * 查找第一个可用的时间段 - 正常约束
     */
    private Schedule findFirstSession(TeachingTask task, Semester semester,
                                      List<Classroom> suitableClassrooms,
                                      List<TeacherTimeWhitelist> teacherPreferences,
                                      List<ActivityBlacklist> activityBlacklists,
                                      List<Schedule> allSchedules, // 包含所有已有排课
                                      List<TeachingTask> allTeachingTasks,
                                      int lessonsCount) {

        // 随机打乱教室顺序，增加随机性
        List<Classroom> shuffledClassrooms = new ArrayList<>(suitableClassrooms);
        Collections.shuffle(shuffledClassrooms);

        // 随机打乱星期顺序
        //List<Integer> days = Arrays.asList(1, 2, 3, 4, 5, 6, 7);
        List<Integer> days = Arrays.asList(1, 2, 3, 4, 5);
        Collections.shuffle(days);

        for (Integer day : days) {
            // 尝试不同的开始时间，但确保开始时间是奇数节课
            for (int startHour = 1; startHour <= 12 - lessonsCount + 1; startHour++) {
                // 检查开始时间是否为奇数节课
                if (startHour % 2 == 0) {
                    continue; // 跳过偶数节课作为开始时间
                }

                int endHour = startHour + lessonsCount - 1;

                for (Classroom classroom : shuffledClassrooms) {
                    // 检查所有约束 - 包含教师时间偏好
                    if (checkAllConstraints(task, classroom, day, startHour, endHour,
                            1, teacherPreferences, activityBlacklists, allSchedules, allTeachingTasks)) {

                        // 创建排课记录
                        Schedule schedule = createSchedule(task, classroom, semester,
                                day, startHour, endHour, 1);
                        return schedule;
                    }
                }
            }
        }

        return null; // 无法找到合适的时间和教室
    }

    /**
     * 查找第一个可用的时间段 - 放宽约束（忽略教师时间偏好）
     */
    private Schedule findFirstSessionWithRelaxedConstraints(TeachingTask task, Semester semester,
                                                            List<Classroom> suitableClassrooms,
                                                            List<TeacherTimeWhitelist> teacherPreferences,
                                                            List<ActivityBlacklist> activityBlacklists,
                                                            List<Schedule> allSchedules, // 包含所有已有排课
                                                            List<TeachingTask> allTeachingTasks,
                                                            int lessonsCount) {

        // 随机打乱教室顺序，增加随机性
        List<Classroom> shuffledClassrooms = new ArrayList<>(suitableClassrooms);
        Collections.shuffle(shuffledClassrooms);

        // 随机打乱星期顺序
        //List<Integer> days = Arrays.asList(1, 2, 3, 4, 5, 6, 7);
        List<Integer> days = Arrays.asList(1, 2, 3, 4, 5);
        Collections.shuffle(days);

        for (Integer day : days) {
            // 尝试不同的开始时间，但确保开始时间是奇数节课
            for (int startHour = 1; startHour <= 12 - lessonsCount + 1; startHour++) {
                // 检查开始时间是否为奇数节课
                if (startHour % 2 == 0) {
                    continue; // 跳过偶数节课作为开始时间
                }

                int endHour = startHour + lessonsCount - 1;

                for (Classroom classroom : shuffledClassrooms) {
                    // 检查约束，但忽略教师时间偏好
                    if (checkAllConstraintsWithRelaxedTeacherPref(task, classroom, day, startHour, endHour,
                            1, teacherPreferences, activityBlacklists, allSchedules, allTeachingTasks)) {

                        // 创建排课记录
                        Schedule schedule = createSchedule(task, classroom, semester,
                                day, startHour, endHour, 1);
                        return schedule;
                    }
                }
            }
        }

        return null; // 无法找到合适的时间和教室
    }

    /**
     * 检查特定时间槽是否不冲突（包括同一教师的其他任务）
     */
    private boolean checkTimeSlotNotConflicting(TeachingTask task, Integer classroomId,
                                                int day, int startHour, int endHour, int week,
                                                List<Schedule> allSchedules, List<TeachingTask> allTeachingTasks) {
        Long weekPattern = 1L << (week - 1); // 第n周对应第n位

        // 检查教室冲突
        if (ConstraintChecker.checkTimeConflictForClassroom(allSchedules, classroomId,
                day, startHour, endHour, weekPattern)) {
            return false;
        }

        // 检查教师冲突 - 使用更精确的冲突检测
        if (ConstraintChecker.checkTimeConflictForTeacher(allSchedules, task.getTeacherId(),
                day, startHour, endHour, weekPattern, allTeachingTasks)) {
            return false;
        }

        // 检查学生班级冲突
        if (ConstraintChecker.checkTimeConflictForClass(allSchedules, task.getClassId(),
                day, startHour, endHour, weekPattern)) {
            return false;
        }

        // 检查教师跨校区冲突 - 确保同一教师在同一天不在不同校区上课
        return !ConstraintChecker.checkCrossCampusConflict(allSchedules, task.getTeacherId(),
                day, startHour, endHour, weekPattern, allTeachingTasks);
    }

    /**
     * 检查所有约束条件（正常）
     */
    private boolean checkAllConstraints(TeachingTask task, Classroom classroom,
                                        int day, int startHour, int endHour, int week,
                                        List<TeacherTimeWhitelist> teacherPreferences,
                                        List<ActivityBlacklist> activityBlacklists,
                                        List<Schedule> allSchedules, List<TeachingTask> allTeachingTasks) {

        // 检查开始时间是否为奇数节课
        if (startHour % 2 == 0) {
            return false; // 开始时间必须是奇数节课
        }

        // 检查教室容量
        if (task.getStudentGroup() != null && classroom != null &&
                !ConstraintChecker.checkCapacity(classroom, task.getStudentGroup())) {
            return false;
        }

        // 检查教室类型
        if (task.getCoursePart() != null && classroom != null &&
                !ConstraintChecker.checkRoomType(classroom, task.getCoursePart().getRequiredRoomType())) {
            return false;
        }

        // 检查教室校区匹配
        if (classroom != null && task.getCampusId() != null &&
                !classroom.getCampusId().equals(task.getCampusId())) {
            return false; // 教室必须在任务指定的校区
        }

        // 检查教师时间偏好
        List<TeacherTimeWhitelist> teacherPrefs = teacherPreferences.stream()
                .filter(p -> p.getTeacherId().equals(task.getTeacherId()))
                .collect(Collectors.toList());

        if (!teacherPrefs.isEmpty() &&
                !ConstraintChecker.checkTeacherPreference(teacherPrefs, day, startHour, endHour)) {
            return false;
        }

        // 检查活动禁排表
        if (!ConstraintChecker.checkActivityBlacklist(activityBlacklists,
                task.getStudentGroup().getCollegeId(), task.getCampusId(),
                day, startHour, endHour)) {
            return false;
        }

        // 检查时间冲突 - 包括同一教师的其他任务
        return checkTimeSlotNotConflicting(task, classroom.getClassroomId(), day, startHour, endHour,
                week, allSchedules, allTeachingTasks);
    }

    /**
     * 检查所有约束条件（放宽教师时间偏好）
     */
    private boolean checkAllConstraintsWithRelaxedTeacherPref(TeachingTask task, Classroom classroom,
                                                              int day, int startHour, int endHour, int week,
                                                              List<TeacherTimeWhitelist> teacherPreferences,
                                                              List<ActivityBlacklist> activityBlacklists,
                                                              List<Schedule> allSchedules, List<TeachingTask> allTeachingTasks) {

        // 检查开始时间是否为奇数节课
        if (startHour % 2 == 0) {
            return false; // 开始时间必须是奇数节课
        }

        // 检查教室容量
        if (task.getStudentGroup() != null && classroom != null &&
                !ConstraintChecker.checkCapacity(classroom, task.getStudentGroup())) {
            return false;
        }

        // 检查教室类型
        if (task.getCoursePart() != null && classroom != null &&
                !ConstraintChecker.checkRoomType(classroom, task.getCoursePart().getRequiredRoomType())) {
            return false;
        }

        // 检查教室校区匹配
        if (classroom != null && task.getCampusId() != null &&
                !classroom.getCampusId().equals(task.getCampusId())) {
            return false; // 教室必须在任务指定的校区
        }

        // 忽略教师时间偏好（放宽约束）

        // 检查活动禁排表
        if (!ConstraintChecker.checkActivityBlacklist(activityBlacklists,
                task.getStudentGroup().getCollegeId(), task.getCampusId(),
                day, startHour, endHour)) {
            return false;
        }

        // 检查时间冲突 - 包括同一教师的其他任务
        return checkTimeSlotNotConflicting(task, classroom.getClassroomId(), day, startHour, endHour,
                week, allSchedules, allTeachingTasks);
    }

    /**
     * 创建排课记录
     */
    private Schedule createSchedule(TeachingTask task, Classroom classroom,
                                    Semester semester, int day, int startHour,
                                    int endHour, int week) {
        Schedule schedule = new Schedule();
        schedule.setTaskId(task.getTaskId());
        schedule.setClassroomId(classroom.getClassroomId());
        schedule.setSemesterId(semester.getSemesterId());
        schedule.setDayOfWeek(day);
        schedule.setLessonStart(startHour);
        schedule.setLessonEnd(endHour);
        schedule.setWeekPattern(1L << (week - 1)); // 设置周次位图
        return schedule;
    }

    /**
     * 创建详细排课记录
     */
    private Schedule createScheduleWithDetails(TeachingTask task, Integer classroomId,
                                               Semester semester, int day, int startHour,
                                               int endHour, int week) {
        Schedule schedule = new Schedule();
        schedule.setTaskId(task.getTaskId());
        schedule.setClassroomId(classroomId);
        schedule.setSemesterId(semester.getSemesterId());
        schedule.setDayOfWeek(day);
        schedule.setLessonStart(startHour);
        schedule.setLessonEnd(endHour);
        schedule.setWeekPattern(1L << (week - 1)); // 设置周次位图
        return schedule;
    }

    /**
     * 查找合适的教室 - 按校区筛选
     */
    private List<Classroom> findSuitableClassrooms(RoomType requiredRoomType,
                                                   StudentGroup studentGroup,
                                                   List<Classroom> allClassrooms,
                                                   Integer targetCampusId) {
        if (requiredRoomType == null) return new ArrayList<>();

        return allClassrooms.stream()
                .filter(room -> room.getRoomTypeId().equals(requiredRoomType.getTypeId()))
                .filter(room -> room.getCapacity() >= studentGroup.getStudentCount())
                .filter(room -> room.getIsAvailable())
                .filter(room -> room.getCampusId().equals(targetCampusId)) // 确保教室在正确的校区
                .collect(Collectors.toList());
    }

    /**
     * 查找合适的教室（放宽容量限制）- 按校区筛选
     */
    private List<Classroom> findSuitableClassroomsWithRelaxedCapacity(RoomType requiredRoomType,
                                                                      StudentGroup studentGroup,
                                                                      List<Classroom> allClassrooms,
                                                                      Integer targetCampusId) {
        if (requiredRoomType == null) return new ArrayList<>();

        return allClassrooms.stream()
                .filter(room -> room.getRoomTypeId().equals(requiredRoomType.getTypeId()))
                .filter(room -> room.getIsAvailable())
                .filter(room -> room.getCampusId().equals(targetCampusId)) // 确保教室在正确的校区
                .collect(Collectors.toList());
    }

    /**
     * 确定排课策略 - 根据week_schedule_pattern字段
     */
    private SchedulingStrategy determineSchedulingStrategy(TeachingTask task, BigDecimal lessonsPerWeek) {
        String weekSchedulePattern = task.getWeekSchedulePattern();
        int lessonsInt = lessonsPerWeek.intValue();
        int decimal = lessonsPerWeek.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) > 0 ? 1 : 0;

        if (weekSchedulePattern != null) {
            // 根据week_schedule_pattern字段确定策略
            switch (weekSchedulePattern.toUpperCase()) {
                case "ALTERNATING":
                    // 单双周交替排课
                    if (lessonsInt == 1) {
                        // 1课时：单周0课时，双周2课时 或 单周2课时，双周0课时
                        return new SchedulingStrategy(SchedulingStrategy.Type.ALTERNATING,
                                0, 0, 2);
                    } else if (lessonsInt == 3) {
                        // 3课时：单周2课时，双周4课时 或 单周4课时，双周2课时
                        return new SchedulingStrategy(SchedulingStrategy.Type.ALTERNATING,
                                0, 2, 4);
                    } else {
                        // 其他情况，按单双周平均分配
                        int oddWeekLessons = lessonsInt;
                        int evenWeekLessons = lessonsInt + 1;
                        return new SchedulingStrategy(SchedulingStrategy.Type.ALTERNATING,
                                0, oddWeekLessons, evenWeekLessons);
                    }
                case "CONTINUOUS":
                    // 连续排课
                    return new SchedulingStrategy(SchedulingStrategy.Type.CONTINUOUS,
                            lessonsInt, 0, 0);
                case "FLEXIBLE":
                    // 灵活排课
                    return new SchedulingStrategy(SchedulingStrategy.Type.FLEXIBLE,
                            lessonsInt, 0, 0);
                default:
                    // 默认处理：如果是奇数课时则采用单双周策略
                    if (decimal == 1) {
                        if (lessonsInt == 1) {
                            return new SchedulingStrategy(SchedulingStrategy.Type.ALTERNATING,
                                    0, 0, 2);
                        } else if (lessonsInt == 3) {
                            return new SchedulingStrategy(SchedulingStrategy.Type.ALTERNATING,
                                    0, 2, 4);
                        } else {
                            return new SchedulingStrategy(SchedulingStrategy.Type.ALTERNATING,
                                    0, lessonsInt, lessonsInt + 1);
                        }
                    } else {
                        // 整数课时，按正常周课时安排
                        if (lessonsInt <= 2) {
                            return new SchedulingStrategy(SchedulingStrategy.Type.FLEXIBLE,
                                    lessonsInt, 0, 0);
                        } else {
                            return new SchedulingStrategy(SchedulingStrategy.Type.CONTINUOUS,
                                    lessonsInt, 0, 0);
                        }
                    }
            }
        } else {
            // 如果没有设置week_schedule_pattern字段，默认处理
            if (decimal == 1) {
                // 奇数课时，应用单双周策略
                if (lessonsInt == 1) {
                    return new SchedulingStrategy(SchedulingStrategy.Type.ALTERNATING,
                            0, 0, 2);
                } else if (lessonsInt == 3) {
                    return new SchedulingStrategy(SchedulingStrategy.Type.ALTERNATING,
                            0, 2, 4);
                } else {
                    return new SchedulingStrategy(SchedulingStrategy.Type.ALTERNATING,
                            0, lessonsInt, lessonsInt + 1);
                }
            } else {
                // 整数学分，按正常周课时安排
                if (lessonsInt <= 2) {
                    return new SchedulingStrategy(SchedulingStrategy.Type.FLEXIBLE,
                            lessonsInt, 0, 0);
                } else {
                    return new SchedulingStrategy(SchedulingStrategy.Type.CONTINUOUS,
                            lessonsInt, 0, 0);
                }
            }
        }
    }

    /**
     * 排课状态枚举
     */
    public enum SchedulingStatus {
        SUCCESS, FAILED, PARTIAL_SUCCESS
    }

    /**
     * 排课结果类
     */
    public static class SchedulingResult {
        private SchedulingStatus status;
        private List<Schedule> scheduledClasses;
        private List<TeachingTask> failedTasks;
        private String message;

        public SchedulingResult(SchedulingStatus status, List<Schedule> scheduledClasses,
                                List<TeachingTask> failedTasks, String message) {
            this.status = status;
            this.scheduledClasses = scheduledClasses;
            this.failedTasks = failedTasks;
            this.message = message;
        }

        // Getters and Setters
        public SchedulingStatus getStatus() {
            return status;
        }

        public void setStatus(SchedulingStatus status) {
            this.status = status;
        }

        public List<Schedule> getScheduledClasses() {
            return scheduledClasses;
        }

        public void setScheduledClasses(List<Schedule> scheduledClasses) {
            this.scheduledClasses = scheduledClasses;
        }

        public List<TeachingTask> getFailedTasks() {
            return failedTasks;
        }

        public void setFailedTasks(List<TeachingTask> failedTasks) {
            this.failedTasks = failedTasks;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    /**
     * 排课约束检查器
     */
    public static class ConstraintChecker {

        /**
         * 检查教室时间冲突
         */
        public static boolean checkTimeConflictForClassroom(
                List<Schedule> existingSchedules,
                Integer classroomId,
                Integer dayOfWeek,
                Integer lessonStart,
                Integer lessonEnd,
                Long weekPattern) {

            if (classroomId == null) return false;

            for (Schedule schedule : existingSchedules) {
                if (schedule.getClassroomId() != null &&
                        schedule.getClassroomId().equals(classroomId) &&
                        isTimeOverlap(schedule, dayOfWeek, lessonStart, lessonEnd, weekPattern)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * 检查教师时间冲突 - 修复了逻辑，确保同一教师不能在同一时间上两门课
         */
        public static boolean checkTimeConflictForTeacher(
                List<Schedule> existingSchedules,
                Integer teacherId,
                Integer dayOfWeek,
                Integer lessonStart,
                Integer lessonEnd,
                Long weekPattern,
                List<TeachingTask> allTeachingTasks) {

            if (teacherId == null) return false;

            for (Schedule schedule : existingSchedules) {
                // 获取对应的任务信息
                TeachingTask task = getTeachingTaskById(schedule.getTaskId(), allTeachingTasks);
                if (task != null && task.getTeacherId() != null &&
                        task.getTeacherId().equals(teacherId) &&
                        isTimeOverlap(schedule, dayOfWeek, lessonStart, lessonEnd, weekPattern)) {
                    return true; // 同一教师在同一时间有课，冲突
                }
            }
            return false;
        }

        /**
         * 检查学生班级时间冲突
         */
        public static boolean checkTimeConflictForClass(
                List<Schedule> existingSchedules,
                Integer classId,
                Integer dayOfWeek,
                Integer lessonStart,
                Integer lessonEnd,
                Long weekPattern) {

            if (classId == null) return false;

            for (Schedule schedule : existingSchedules) {
                TeachingTask task = getTeachingTaskById(schedule.getTaskId(),
                        getAllTeachingTasksFromSchedules(existingSchedules));
                if (task != null && task.getClassId() != null &&
                        task.getClassId().equals(classId) &&
                        isTimeOverlap(schedule, dayOfWeek, lessonStart, lessonEnd, weekPattern)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * 检查教师跨校区冲突 - 确保同一教师在同一天不在不同校区上课
         */
        public static boolean checkCrossCampusConflict(
                List<Schedule> existingSchedules,
                Integer teacherId,
                Integer dayOfWeek,
                Integer lessonStart,
                Integer lessonEnd,
                Long weekPattern,
                List<TeachingTask> allTeachingTasks) {

            if (teacherId == null) return false;

            for (Schedule schedule : existingSchedules) {
                TeachingTask task = getTeachingTaskById(schedule.getTaskId(), allTeachingTasks);
                if (task != null && task.getTeacherId() != null &&
                        task.getTeacherId().equals(teacherId) &&
                        schedule.getDayOfWeek() != null && schedule.getDayOfWeek().equals(dayOfWeek) &&
                        weekPatternOverlaps(schedule.getWeekPattern(), weekPattern)) {

                    // 检查是否在不同校区
                    TeachingTask currentTask = findTeachingTaskById(allTeachingTasks, teacherId, dayOfWeek);
                    if (currentTask != null && !task.getCampusId().equals(currentTask.getCampusId())) {
                        // 检查时间段是否有冲突（上午/下午分开）
                        if (isSameDayTimeConflict(lessonStart, lessonEnd, schedule.getLessonStart(), schedule.getLessonEnd())) {
                            return true; // 跨校区冲突
                        }
                    }
                }
            }
            return false;
        }

        /**
         * 检查同一天的时间冲突
         */
        private static boolean isSameDayTimeConflict(Integer start1, Integer end1, Integer start2, Integer end2) {
            // 检查时间段是否重叠
            int maxStart = Math.max(start1, start2);
            int minEnd = Math.min(end1, end2);
            return maxStart < minEnd;
        }

        /**
         * 根据教师ID和星期查找教学任务
         */
        private static TeachingTask findTeachingTaskById(List<TeachingTask> allTasks, Integer teacherId, Integer dayOfWeek) {
            for (TeachingTask task : allTasks) {
                if (task.getTeacherId() != null && task.getTeacherId().equals(teacherId)) {
                    return task;
                }
            }
            return null;
        }

        /**
         * 检查同一课程由同一教师教授的其他部分时间冲突
         * 注意：这里只检查完全相同的时间段，不同时间段的同一课程部分是可以安排的
         */
        public static boolean checkTimeConflictForSameCourseParts(
                List<Schedule> existingSchedules,
                Integer courseId,
                Integer teacherId,
                Integer dayOfWeek,
                Integer lessonStart,
                Integer lessonEnd,
                Long weekPattern,
                List<TeachingTask> allTeachingTasks) {

            if (courseId == null || teacherId == null) return false;

            for (Schedule schedule : existingSchedules) {
                TeachingTask task = getTeachingTaskById(schedule.getTaskId(), allTeachingTasks);
                if (task != null &&
                        task.getCourseId() != null && task.getCourseId().equals(courseId) &&
                        task.getTeacherId() != null && task.getTeacherId().equals(teacherId) &&
                        schedule.getDayOfWeek().equals(dayOfWeek) &&
                        schedule.getLessonStart().equals(lessonStart) &&
                        schedule.getLessonEnd().equals(lessonEnd) &&
                        weekPatternOverlaps(schedule.getWeekPattern(), weekPattern)) {
                    return true; // 同一课程由同一教师教授的其他部分在同一时间安排，冲突
                }
            }
            return false;
        }

        /**
         * 根据任务ID获取教学任务
         */
        private static TeachingTask getTeachingTaskById(Integer taskId, List<TeachingTask> allTasks) {
            for (TeachingTask task : allTasks) {
                if (task.getTaskId() != null && task.getTaskId().equals(taskId)) {
                    return task;
                }
            }
            return null;
        }

        /**
         * 从排课记录中获取所有教学任务
         */
        private static List<TeachingTask> getAllTeachingTasksFromSchedules(List<Schedule> schedules) {
            // 这里简化处理，实际应从数据库查询
            return new ArrayList<>();
        }

        /**
         * 检查时间是否重叠
         */
        private static boolean isTimeOverlap(Schedule schedule, Integer dayOfWeek,
                                             Integer lessonStart, Integer lessonEnd, Long weekPattern) {
            return schedule.getDayOfWeek() != null && schedule.getDayOfWeek().equals(dayOfWeek) &&
                    weekPatternOverlaps(schedule.getWeekPattern(), weekPattern) &&
                    timePeriodsOverlap(schedule.getLessonStart(), schedule.getLessonEnd(),
                            lessonStart, lessonEnd);
        }

        /**
         * 检查周次模式是否重叠
         */
        private static boolean weekPatternOverlaps(Long pattern1, Long pattern2) {
            return pattern1 != null && pattern2 != null && (pattern1 & pattern2) != 0;
        }

        /**
         * 检查时间段是否重叠 - 修复了时间段重叠检测逻辑
         * 两个时间段重叠的情况：
         * 1. [start1, end1] 和 [start2, end2] 重叠，当且仅当 max(start1, start2) < min(end1, end2)
         * 例如：
         * - [1, 4] 和 [2, 3]：max(1, 2)=2, min(4, 3)=3, 因为2<3所以重叠
         * - [1, 3] 和 [4, 5]：max(1, 4)=4, min(3, 5)=3, 因为4>3所以不重叠
         */
        private static boolean timePeriodsOverlap(Integer start1, Integer end1,
                                                  Integer start2, Integer end2) {
            if (start1 == null || end1 == null || start2 == null || end2 == null) {
                return false;
            }
            // 重叠条件：max(start1, start2) < min(end1, end2)
            int maxStart = Math.max(start1, start2);
            int minEnd = Math.min(end1, end2);
            return maxStart < minEnd;
        }

        /**
         * 检查教室容量是否满足要求
         */
        public static boolean checkCapacity(Classroom classroom, StudentGroup studentGroup) {
            if (classroom == null || studentGroup == null) return false;
            return classroom.getCapacity() >= studentGroup.getStudentCount();
        }

        /**
         * 检查教室类型是否匹配
         */
        public static boolean checkRoomType(Classroom classroom, RoomType requiredRoomType) {
            if (classroom == null || requiredRoomType == null) return false;
            return classroom.getRoomTypeId().equals(requiredRoomType.getTypeId());
        }

        /**
         * 检查教师时间偏好（白名单）
         */
        public static boolean checkTeacherPreference(List<TeacherTimeWhitelist> preferences,
                                                     Integer dayOfWeek, Integer lessonStart, Integer lessonEnd) {
            if (dayOfWeek == null || lessonStart == null || lessonEnd == null) return false;

            for (TeacherTimeWhitelist pref : preferences) {
                if (pref.getDayOfWeek().equals(dayOfWeek)) {
                    if (lessonStart >= pref.getLessonStart() && lessonEnd <= pref.getLessonEnd()) {
                        return true; // 在允许的时间范围内
                    }
                }
            }
            return false; // 不在允许的时间范围内
        }

        /**
         * 检查活动禁排表
         */
        public static boolean checkActivityBlacklist(List<ActivityBlacklist> blacklists,
                                                     Integer collegeId, Integer campusId,
                                                     Integer dayOfWeek, Integer lessonStart, Integer lessonEnd) {
            if (dayOfWeek == null || lessonStart == null || lessonEnd == null) return true;

            for (ActivityBlacklist blacklist : blacklists) {
                boolean matchesScope = false;
                if ("UNIVERSITY".equals(blacklist.getScopeType())) {
                    matchesScope = true;
                } else if ("COLLEGE".equals(blacklist.getScopeType()) &&
                        blacklist.getCollegeId() != null &&
                        blacklist.getCollegeId().equals(collegeId)) {
                    matchesScope = true;
                }

                if (matchesScope &&
                        blacklist.getDayOfWeek().equals(dayOfWeek) &&
                        blacklist.getCampusId() != null &&
                        blacklist.getCampusId().equals(campusId.toString())) {

                    // 检查时间槽是否冲突
                    String timeSlot = blacklist.getTimeSlot();
                    if (isInTimeSlot(lessonStart, lessonEnd, timeSlot)) {
                        return false; // 时间冲突，不可用
                    }
                }
            }
            return true; // 没有冲突，可用
        }

        private static boolean isInTimeSlot(Integer lessonStart, Integer lessonEnd, String timeSlot) {
            if (lessonStart == null || lessonEnd == null) return false;

            switch (timeSlot) {
                case "MORNING":
                    return lessonStart >= 1 && lessonEnd <= 4;
                case "AFTERNOON":
                    return lessonStart >= 5 && lessonEnd <= 8;
                case "EVENING":
                    return lessonStart >= 9;
                case "ALL_DAY":
                    return true;
                default:
                    return false;
            }
        }
    }

    /**
     * 排课策略类
     *
     * @param type              类型
     * @param lessonsPerSession 每次课的课时数
     * @param oddWeekLessons    单周课时数
     * @param evenWeekLessons   双周课时数
     */
    public record SchedulingStrategy(Type type, int lessonsPerSession, int oddWeekLessons, int evenWeekLessons) {

        public enum Type {
            CONTINUOUS,    // 连续排课
            ALTERNATING,   // 交替排课（单双周）
            FLEXIBLE       // 灵活排课
        }
    }
}