package com.xy.course_scheduling.algorithm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xy.course_scheduling.entity.*;
import com.xy.course_scheduling.mapper.*;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 混合启发式 - 遗传算法课程调度系统
 * 结合启发式算法的高成功率和遗传算法的全局搜索能力
 * 支持失败课程拆分排课（4 节拆成 2+2，6 节拆成 2+2+2/2+4/4+2）
 */
@Service
public class HybridCourseSchedulingAlgorithm {

    @Resource
    private TeachingTaskMapper teachingTaskMapper;
    @Resource
    private ClassroomMapper classroomMapper;
    @Resource
    private TeacherTimeWhitelistMapper teacherTimeWhitelistMapper;
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
    @Resource
    private ScheduleMapper scheduleMapper;

    // 遗传算法参数
    private static final int POPULATION_SIZE = 50;
    private static final double CROSSOVER_RATE = 0.8;
    private static final double MUTATION_RATE = 0.05;
    private static final int MAX_GENERATIONS = 50;
    private static final int TOURNAMENT_SIZE = 3;

    /**
     * 使用混合算法生成排课方案（主入口）
     */
    public SchedulingResult generateScheduleHybrid(Integer semesterId) {
        try {
            // 获取所有待排课的教学任务
            List<TeachingTask> teachingTasks = teachingTaskMapper.selectList(
                    new LambdaQueryWrapper<TeachingTask>()
                            .eq(TeachingTask::getSemesterId, semesterId)
                            .eq(TeachingTask::getStatus, "CONFIRMED")
                            .eq(TeachingTask::getDeleted, 0)
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
                    new LambdaQueryWrapper<Classroom>()
                            .eq(Classroom::getIsAvailable, true)
                            .eq(Classroom::getDeleted, 0)
            );

            List<TeacherTimeWhitelist> teacherPreferences = teacherTimeWhitelistMapper
                    .selectList(new LambdaQueryWrapper<TeacherTimeWhitelist>()
                            .eq(TeacherTimeWhitelist::getSemesterId, semesterId)
                            .eq(TeacherTimeWhitelist::getDeleted, 0));

            List<ActivityBlacklist> activityBlacklists = activityBlacklistMapper
                    .selectList(new LambdaQueryWrapper<ActivityBlacklist>()
                            .eq(ActivityBlacklist::getSemesterId, semesterId)
                            .eq(ActivityBlacklist::getDeleted, 0));

            Semester semester = teachingTasks.get(0).getSemester();

            // 获取现有的排课记录，用于冲突检测
            List<Schedule> existingSchedules = scheduleMapper.selectList(
                    new LambdaQueryWrapper<Schedule>()
                            .eq(Schedule::getSemesterId, semesterId)
            );

            // 第一阶段：使用启发式算法生成初始解
            SchedulingResult heuristicResult = generateHeuristicSchedule(
                    teachingTasks, classrooms, teacherPreferences, activityBlacklists, 
                    semester, existingSchedules);

            List<Schedule> scheduledClasses = heuristicResult.getScheduledClasses();
            List<TeachingTask> failedTasks = heuristicResult.getFailedTasks();

            // 第二阶段：如果启发式算法未能完全解决，使用遗传算法优化
            if (!failedTasks.isEmpty()) {
                System.out.println("启发式算法排课完成，有 " + failedTasks.size() + " 门课程排课失败，开始遗传算法优化...");
                
                // 对剩余任务使用遗传算法
                List<Schedule> geneticSchedules = solveWithGeneticAlgorithm(
                        failedTasks, classrooms, teacherPreferences, activityBlacklists, 
                        semester, scheduledClasses);

                // 合并结果
                scheduledClasses.addAll(geneticSchedules);

                // 重新评估是否还有未安排的任务
                List<TeachingTask> stillFailedTasks = findUnscheduledTasks(failedTasks, geneticSchedules);

                System.out.println("遗传算法排课完成，成功安排 " + geneticSchedules.size() + " 门，仍有 " + stillFailedTasks.size() + " 门失败");

                // 第三阶段：对仍有失败的任务，尝试拆分课时排课
                if (!stillFailedTasks.isEmpty()) {
                    System.out.println("开始尝试拆分课时排课...");
                    List<TeachingTask> splitFailedTasks = new ArrayList<>();
                    
                    for (TeachingTask failedTask : stillFailedTasks) {
                        List<Schedule> splitSchedules = scheduleTaskWithSplitting(
                                failedTask, semester, classrooms, teacherPreferences,
                                activityBlacklists, scheduledClasses, failedTasks);

                        if (!splitSchedules.isEmpty()) {
                            scheduledClasses.addAll(splitSchedules);
                            System.out.println("任务 " + failedTask.getTaskId() + " 拆分课时后排课成功！");
                        } else {
                            splitFailedTasks.add(failedTask);
                        }
                    }
                    stillFailedTasks = splitFailedTasks;
                }

                String message = String.format("混合算法排课 - 启发式安排%d门，遗传算法安排%d门，最终失败%d门",
                        heuristicResult.getScheduledClasses().size(),
                        geneticSchedules.size(),
                        stillFailedTasks.size());

                SchedulingStatus status = stillFailedTasks.isEmpty() ?
                        SchedulingStatus.SUCCESS :
                        (scheduledClasses.isEmpty() ? SchedulingStatus.FAILED : SchedulingStatus.PARTIAL_SUCCESS);

                return new SchedulingResult(status, scheduledClasses, stillFailedTasks, message);
            } else {
                return heuristicResult;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return new SchedulingResult(SchedulingStatus.FAILED, new ArrayList<>(),
                    new ArrayList<>(), "排课过程中发生错误：" + e.getMessage());
        }
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
            return schedules;
        }

        // 生成拆分策略
        List<List<Integer>> splitStrategies = generateSplitStrategies(lessonsPerWeek);

        // 尝试每种拆分策略
        for (List<Integer> splitStrategy : splitStrategies) {
            List<Schedule> trySchedules = trySplitSchedule(task, semester, suitableClassrooms,
                    teacherPreferences, activityBlacklists, existingSchedules, allTeachingTasks,
                    splitStrategy, totalLessons);

            if (!trySchedules.isEmpty()) {
                schedules.addAll(trySchedules);
                break;
            }
        }

        return schedules;
    }

    /**
     * 生成课时拆分策略
     */
    private List<List<Integer>> generateSplitStrategies(int lessonsPerWeek) {
        List<List<Integer>> strategies = new ArrayList<>();

        if (lessonsPerWeek == 4) {
            strategies.add(Arrays.asList(2, 2));
        } else if (lessonsPerWeek == 6) {
            strategies.add(Arrays.asList(2, 2, 2));
            strategies.add(Arrays.asList(2, 4));
            strategies.add(Arrays.asList(4, 2));
        } else if (lessonsPerWeek == 8) {
            strategies.add(Arrays.asList(2, 2, 2, 2));
            strategies.add(Arrays.asList(4, 4));
            strategies.add(Arrays.asList(2, 2, 4));
            strategies.add(Arrays.asList(4, 2, 2));
        } else if (lessonsPerWeek >= 3) {
            List<Integer> splitBy2 = new ArrayList<>();
            int remaining = lessonsPerWeek;
            while (remaining > 0) {
                if (remaining >= 2) {
                    splitBy2.add(2);
                    remaining -= 2;
                } else {
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
        List<Schedule> tempSchedules = new ArrayList<>(existingSchedules);

        while (scheduledLessons < totalLessons && week <= semester.getTotalWeeks()) {
            int lessonsThisWeek = splitStrategy.get(splitIndex % splitStrategy.size());

            if (scheduledLessons + lessonsThisWeek > totalLessons) {
                lessonsThisWeek = totalLessons - scheduledLessons;
            }

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

        return scheduledLessons >= totalLessons ? schedules : new ArrayList<>();
    }

    /**
     * 查找第一个可用的时间段
     */
    private Schedule findFirstSession(TeachingTask task, Semester semester,
                                      List<Classroom> suitableClassrooms,
                                      List<TeacherTimeWhitelist> teacherPreferences,
                                      List<ActivityBlacklist> activityBlacklists,
                                      List<Schedule> allSchedules,
                                      List<TeachingTask> allTeachingTasks,
                                      int lessonsCount) {

        List<Classroom> shuffledClassrooms = new ArrayList<>(suitableClassrooms);
        Collections.shuffle(shuffledClassrooms);

        List<Integer> days = Arrays.asList(1, 2, 3, 4, 5);
        Collections.shuffle(days);

        for (Integer day : days) {
            for (int startHour = 1; startHour <= 12 - lessonsCount + 1; startHour++) {
                if (startHour % 2 == 0) continue;

                int endHour = startHour + lessonsCount - 1;

                for (Classroom classroom : shuffledClassrooms) {
                    if (checkAllConstraints(task, classroom, day, startHour, endHour,
                            1, teacherPreferences, activityBlacklists, allSchedules, allTeachingTasks)) {

                        Schedule schedule = createSchedule(task, classroom, semester,
                                day, startHour, endHour, 1);
                        return schedule;
                    }
                }
            }
        }

        return null;
    }

    /**
     * 检查所有约束条件
     */
    private boolean checkAllConstraints(TeachingTask task, Classroom classroom,
                                        int day, int startHour, int endHour, int week,
                                        List<TeacherTimeWhitelist> teacherPreferences,
                                        List<ActivityBlacklist> activityBlacklists,
                                        List<Schedule> allSchedules, List<TeachingTask> allTeachingTasks) {

        if (startHour % 2 == 0) return false;

        if (task.getStudentGroup() != null && classroom != null &&
                !ConstraintChecker.checkCapacity(classroom, task.getStudentGroup())) {
            return false;
        }

        if (task.getCoursePart() != null && classroom != null &&
                !ConstraintChecker.checkRoomType(classroom, task.getCoursePart().getRequiredRoomType())) {
            return false;
        }

        if (classroom != null && task.getCampusId() != null &&
                !classroom.getCampusId().equals(task.getCampusId())) {
            return false;
        }

        List<TeacherTimeWhitelist> teacherPrefs = teacherPreferences.stream()
                .filter(p -> p.getTeacherId().equals(task.getTeacherId()))
                .collect(Collectors.toList());

        if (!teacherPrefs.isEmpty() &&
                !ConstraintChecker.checkTeacherPreference(teacherPrefs, day, startHour, endHour)) {
            return false;
        }

        if (!ConstraintChecker.checkActivityBlacklist(activityBlacklists,
                task.getStudentGroup().getCollegeId(), task.getCampusId(),
                day, startHour, endHour)) {
            return false;
        }

        return checkTimeSlotNotConflicting(task, classroom.getClassroomId(), day, startHour, endHour,
                week, allSchedules, allTeachingTasks);
    }

    /**
     * 检查时间槽是否不冲突
     */
    private boolean checkTimeSlotNotConflicting(TeachingTask task, Integer classroomId,
                                                int day, int startHour, int endHour, int week,
                                                List<Schedule> allSchedules, List<TeachingTask> allTeachingTasks) {
        Long weekPattern = 1L << (week - 1);

        if (ConstraintChecker.checkTimeConflictForClassroom(allSchedules, classroomId,
                day, startHour, endHour, weekPattern)) {
            return false;
        }

        if (ConstraintChecker.checkTimeConflictForTeacher(allSchedules, task.getTeacherId(),
                day, startHour, endHour, weekPattern, allTeachingTasks)) {
            return false;
        }

        if (ConstraintChecker.checkTimeConflictForClass(allSchedules, task.getClassId(),
                day, startHour, endHour, weekPattern)) {
            return false;
        }

        return !ConstraintChecker.checkCrossCampusConflict(allSchedules, task.getTeacherId(),
                day, startHour, endHour, weekPattern, allTeachingTasks);
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
        schedule.setWeekPattern(1L << (week - 1));
        return schedule;
    }

    /**
     * 启发式算法 - 基于优先级的排课
     * 支持交替排课：奇数课时和>=4 的偶数课时使用单双周交替排课，只有 2 课时使用连续排课
     */
    private SchedulingResult generateHeuristicSchedule(List<TeachingTask> tasks,
                                                       List<Classroom> classrooms,
                                                       List<TeacherTimeWhitelist> teacherPreferences,
                                                       List<ActivityBlacklist> activityBlacklists,
                                                       Semester semester,
                                                       List<Schedule> existingSchedules) {
        // 按照优先级排序，优先安排难以排课的课程
        List<TeachingTask> sortedTasks = tasks.stream()
                .sorted((t1, t2) -> {
                    int priority1 = calculatePriority(t1, teacherPreferences);
                    int priority2 = calculatePriority(t2, teacherPreferences);
                    return Integer.compare(priority2, priority1); // 降序排列
                })
                .collect(Collectors.toList());

        List<Schedule> scheduledClasses = new ArrayList<>();
        List<TeachingTask> failedTasks = new ArrayList<>();
        List<Schedule> tempSchedules = new ArrayList<>(existingSchedules);
        
        // 调试：检查是否有重复的任务
        System.out.println("=== 开始排课 ===");
        System.out.println("任务总数：" + sortedTasks.size());
        long uniqueTaskIds = sortedTasks.stream().map(TeachingTask::getTaskId).distinct().count();
        System.out.println("任务 ID 列表：" + uniqueTaskIds + " 个唯一 ID");
        
        if (sortedTasks.size() != uniqueTaskIds) {
            System.out.println("警告：发现重复的任务！");
            // 找出重复的任务 ID
            sortedTasks.stream()
                .map(TeachingTask::getTaskId)
                .collect(java.util.stream.Collectors.groupingBy(java.util.function.Function.identity(), java.util.stream.Collectors.counting()))
                .entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .forEach(e -> System.out.println("  重复任务 ID=" + e.getKey() + ", 出现次数=" + e.getValue()));
        }

        for (TeachingTask task : sortedTasks) {
            boolean scheduled = false;
            int lessonsPerWeek = task.getLessonsPerWeek().intValue();
            
            System.out.println("处理任务 ID=" + task.getTaskId() + ", lessonsPerWeek=" + lessonsPerWeek);

            List<Classroom> suitableClassrooms = findSuitableClassrooms(
                    task.getCoursePart() != null ? task.getCoursePart().getRequiredRoomType() : null,
                    task.getStudentGroup(), classrooms, task.getCampusId()
            );

            // 判断排课方式
            // 原则：每周课时必须是偶数
            // 1. 偶数课时：连续排课（每周相同课时）
            // 2. 奇数课时：交替排课（单周±1，双周∓1，平均=周课时）
            if (lessonsPerWeek % 2 == 0) {
                // 偶数课时：连续排课，每周相同时间，为每一周创建一条记录
                List<Integer[]> preferredTimeSlots = getPreferredTimeSlots(
                        task.getTeacherId(), teacherPreferences);

                List<Integer[]> allTimeSlots = new ArrayList<>(preferredTimeSlots);
                allTimeSlots.addAll(generateAllTimeSlots());

                for (Integer[] timeSlot : allTimeSlots) {
                    Integer dayOfWeek = timeSlot[0];
                    Integer lessonStart = timeSlot[1];
                    Integer lessonEnd = lessonStart + lessonsPerWeek - 1;

                    if (lessonEnd > 12) continue;

                    for (Classroom classroom : suitableClassrooms) {
                        if (checkAllConstraints(task, classroom, dayOfWeek, lessonStart, lessonEnd,
                                1, teacherPreferences, activityBlacklists, tempSchedules, tasks)) {

                            // 为每一周创建一条记录
                            for (int week = 1; week <= semester.getTotalWeeks(); week++) {
                                Schedule schedule = createSchedule(task, classroom, semester,
                                        dayOfWeek, lessonStart, lessonEnd, week);
                                scheduledClasses.add(schedule);
                                tempSchedules.add(schedule);
                            }
                            scheduled = true;
                            break;
                        }
                    }

                    if (scheduled) break;
                }
            } else {
                // 奇数课时：交替排课（单周±1，双周∓1）
                scheduled = scheduleAlternating(task, semester, suitableClassrooms,
                        teacherPreferences, activityBlacklists, tempSchedules, tasks,
                        scheduledClasses, tempSchedules);
            }

            if (!scheduled) {
                failedTasks.add(task);
            }
        }

        String message = String.format("启发式算法排课 - 成功排课%d门，失败%d门",
                scheduledClasses.size(), failedTasks.size());

        SchedulingStatus status = failedTasks.isEmpty() ?
                SchedulingStatus.SUCCESS :
                (scheduledClasses.isEmpty() ? SchedulingStatus.FAILED : SchedulingStatus.PARTIAL_SUCCESS);

        return new SchedulingResult(status, scheduledClasses, failedTasks, message);
    }

    /**
     * 交替排课：为奇数课时和>=4 的偶数课时安排单双周不同的排课
     * - 奇数课时（如 3 课时）：单周 2 课时，双周课时数根据总课时调整
     * - 偶数课时>=4：单周和双周课时数相同（如 2+2 或 4+4）
     * 单周 weekPattern = 0x55555555L (二进制 0101...)
     * 双周 weekPattern = 0xAAAAAAAA L (二进制 1010...)
     */
    private boolean scheduleAlternating(TeachingTask task, Semester semester,
                                        List<Classroom> suitableClassrooms,
                                        List<TeacherTimeWhitelist> teacherPreferences,
                                        List<ActivityBlacklist> activityBlacklists,
                                        List<Schedule> tempSchedules,
                                        List<TeachingTask> allTasks,
                                        List<Schedule> scheduledClasses,
                                        List<Schedule> allSchedules) {
        int lessonsPerWeek = task.getLessonsPerWeek().intValue();
        int totalWeeks = semester.getTotalWeeks();

        // 计算单周和双周的课时数
        // 奇数课时：单周±1，双周∓1，平均=lessonsPerWeek
        int oddWeekLessons; // 单周课时数
        int evenWeekLessons; // 双周课时数

        if (lessonsPerWeek % 2 == 1) {
            // 奇数课时：单周和双周相差 2 课时，平均=lessonsPerWeek
            // 1 课时：单周 0 课时，双周 2 课时（平均 1 课时）
            // 3 课时：单周 2 课时，双周 4 课时（平均 3 课时）
            // 5 课时：单周 4 课时，双周 6 课时（平均 5 课时）
            // 7 课时：单周 6 课时，双周 8 课时（平均 7 课时）
            oddWeekLessons = lessonsPerWeek - 1;
            evenWeekLessons = lessonsPerWeek + 1;
        } else {
            // 偶数课时不应该使用交替排课，这里做保护处理
            // 如果传入偶数课时，使用连续排课的课时数
            oddWeekLessons = lessonsPerWeek;
            evenWeekLessons = lessonsPerWeek;
        }

        // 单周位图：0x55555555L (二进制 01010101...)
        long oddWeekPattern = 0x55555555L;
        // 双周位图：0xAAAAAAAAL (二进制 10101010...)
        long evenWeekPattern = 0xAAAAAAAAL;

        // 限制位图到学期总周数
        if (totalWeeks < 32) {
            long mask = (1L << totalWeeks) - 1;
            oddWeekPattern &= mask;
            evenWeekPattern &= mask;
        }

        List<Integer[]> preferredTimeSlots = getPreferredTimeSlots(
                task.getTeacherId(), teacherPreferences);
        List<Integer[]> allTimeSlots = new ArrayList<>(preferredTimeSlots);
        allTimeSlots.addAll(generateAllTimeSlots());

        // 尝试为单周和双周找到合适的时间段
        for (Integer[] timeSlot : allTimeSlots) {
            Integer dayOfWeek = timeSlot[0];
            Integer lessonStartOdd = timeSlot[1];
            Integer lessonEndOdd = lessonStartOdd + oddWeekLessons - 1;

            // 如果单周课时数为 0，跳过单周检查
            if (oddWeekLessons == 0) {
                // 只为双周排课
                Integer lessonStartEven = findFirstSessionForWeek(task, dayOfWeek, suitableClassrooms,
                        teacherPreferences, activityBlacklists, allSchedules, allTasks,
                        evenWeekLessons, evenWeekPattern);

                if (lessonStartEven != null) {
                    Integer lessonEndEven = lessonStartEven + evenWeekLessons - 1;

                    for (Classroom classroom : suitableClassrooms) {
                        if (checkAllConstraintsForAlternating(task, classroom, dayOfWeek,
                                lessonStartEven, lessonEndEven, evenWeekPattern,
                                teacherPreferences, activityBlacklists, allSchedules, allTasks)) {

                            Schedule evenSchedule = createScheduleWithWeekPattern(task, classroom, semester,
                                    dayOfWeek, lessonStartEven, lessonEndEven, evenWeekPattern);
                            scheduledClasses.add(evenSchedule);
                            allSchedules.add(evenSchedule);

                            return true;
                        }
                    }
                }
                continue;
            }

            if (lessonEndOdd > 12) continue;

            // 检查单周时间段是否可用
            boolean oddWeekAvailable = true;
            for (Classroom classroom : suitableClassrooms) {
                if (checkAllConstraintsForAlternating(task, classroom, dayOfWeek,
                        lessonStartOdd, lessonEndOdd, oddWeekPattern,
                        teacherPreferences, activityBlacklists, allSchedules, allTasks)) {
                    oddWeekAvailable = true;
                    break;
                }
            }

            if (!oddWeekAvailable) continue;

            // 查找双周时间段（可以与单周相同或不同）
            Integer lessonStartEven = findFirstSessionForWeek(task, dayOfWeek, suitableClassrooms,
                    teacherPreferences, activityBlacklists, allSchedules, allTasks,
                    evenWeekLessons, evenWeekPattern);

            if (lessonStartEven != null) {
                Integer lessonEndEven = lessonStartEven + evenWeekLessons - 1;

                // 为单周和双周创建排课记录
                for (Classroom classroom : suitableClassrooms) {
                    if (checkAllConstraintsForAlternating(task, classroom, dayOfWeek,
                            lessonStartOdd, lessonEndOdd, oddWeekPattern,
                            teacherPreferences, activityBlacklists, allSchedules, allTasks)) {

                        Schedule oddSchedule = createScheduleWithWeekPattern(task, classroom, semester,
                                dayOfWeek, lessonStartOdd, lessonEndOdd, oddWeekPattern);
                        scheduledClasses.add(oddSchedule);
                        allSchedules.add(oddSchedule);

                        if (checkAllConstraintsForAlternating(task, classroom, dayOfWeek,
                                lessonStartEven, lessonEndEven, evenWeekPattern,
                                teacherPreferences, activityBlacklists, allSchedules, allTasks)) {

                            Schedule evenSchedule = createScheduleWithWeekPattern(task, classroom, semester,
                                    dayOfWeek, lessonStartEven, lessonEndEven, evenWeekPattern);
                            scheduledClasses.add(evenSchedule);
                            allSchedules.add(evenSchedule);

                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * 查找特定课时数的时间段（用于交替排课的双周）
     * @return 返回合适的开始课时，如果找不到返回 null
     */
    private Integer findFirstSessionForWeek(TeachingTask task, Integer dayOfWeek,
                                            List<Classroom> suitableClassrooms,
                                            List<TeacherTimeWhitelist> teacherPreferences,
                                            List<ActivityBlacklist> activityBlacklists,
                                            List<Schedule> allSchedules,
                                            List<TeachingTask> allTasks,
                                            int lessonsCount, long weekPattern) {
        for (int startHour = 1; startHour <= 12 - lessonsCount + 1; startHour++) {
            if (startHour % 2 == 0) continue; // 确保从奇数课时开始

            int endHour = startHour + lessonsCount - 1;

            for (Classroom classroom : suitableClassrooms) {
                if (checkAllConstraintsForAlternating(task, classroom, dayOfWeek,
                        startHour, endHour, weekPattern,
                        teacherPreferences, activityBlacklists, allSchedules, allTasks)) {
                    return startHour;
                }
            }
        }
        return null;
    }

    /**
     * 检查交替排课的约束条件
     */
    private boolean checkAllConstraintsForAlternating(TeachingTask task, Classroom classroom,
                                                      int day, int startHour, int endHour, long weekPattern,
                                                      List<TeacherTimeWhitelist> teacherPreferences,
                                                      List<ActivityBlacklist> activityBlacklists,
                                                      List<Schedule> allSchedules, List<TeachingTask> allTasks) {
        if (startHour % 2 == 0) return false;

        if (task.getStudentGroup() != null && classroom != null &&
                !ConstraintChecker.checkCapacity(classroom, task.getStudentGroup())) {
            return false;
        }

        if (task.getCoursePart() != null && classroom != null &&
                !ConstraintChecker.checkRoomType(classroom, task.getCoursePart().getRequiredRoomType())) {
            return false;
        }

        if (classroom != null && task.getCampusId() != null &&
                !classroom.getCampusId().equals(task.getCampusId())) {
            return false;
        }

        List<TeacherTimeWhitelist> teacherPrefs = teacherPreferences.stream()
                .filter(p -> p.getTeacherId().equals(task.getTeacherId()))
                .collect(Collectors.toList());

        if (!teacherPrefs.isEmpty() &&
                !ConstraintChecker.checkTeacherPreference(teacherPrefs, day, startHour, endHour)) {
            return false;
        }

        if (!ConstraintChecker.checkActivityBlacklist(activityBlacklists,
                task.getStudentGroup().getCollegeId(), task.getCampusId(),
                day, startHour, endHour)) {
            return false;
        }

        return checkTimeSlotNotConflictingForAlternating(task, classroom.getClassroomId(),
                day, startHour, endHour, weekPattern, allSchedules, allTasks);
    }

    /**
     * 检查交替排课的时间槽是否不冲突
     */
    private boolean checkTimeSlotNotConflictingForAlternating(TeachingTask task, Integer classroomId,
                                                              int day, int startHour, int endHour, long weekPattern,
                                                              List<Schedule> allSchedules, List<TeachingTask> allTasks) {
        if (ConstraintChecker.checkTimeConflictForClassroom(allSchedules, classroomId,
                day, startHour, endHour, weekPattern)) {
            return false;
        }

        if (ConstraintChecker.checkTimeConflictForTeacher(allSchedules, task.getTeacherId(),
                day, startHour, endHour, weekPattern, allTasks)) {
            return false;
        }

        if (ConstraintChecker.checkTimeConflictForClass(allSchedules, task.getClassId(),
                day, startHour, endHour, weekPattern)) {
            return false;
        }

        return !ConstraintChecker.checkCrossCampusConflict(allSchedules, task.getTeacherId(),
                day, startHour, endHour, weekPattern, allTasks);
    }

    /**
     * 创建带有周次位图的排课记录
     */
    private Schedule createScheduleWithWeekPattern(TeachingTask task, Classroom classroom,
                                                   Semester semester, int day, int startHour,
                                                   int endHour, long weekPattern) {
        Schedule schedule = new Schedule();
        schedule.setTaskId(task.getTaskId());
        schedule.setClassroomId(classroom.getClassroomId());
        schedule.setSemesterId(semester.getSemesterId());
        schedule.setDayOfWeek(day);
        schedule.setLessonStart(startHour);
        schedule.setLessonEnd(endHour);
        schedule.setWeekPattern(weekPattern);
        return schedule;
    }

    /**
     * 使用遗传算法优化剩余任务
     */
    private List<Schedule> solveWithGeneticAlgorithm(List<TeachingTask> tasks,
                                                     List<Classroom> classrooms,
                                                     List<TeacherTimeWhitelist> teacherPreferences,
                                                     List<ActivityBlacklist> activityBlacklists,
                                                     Semester semester,
                                                     List<Schedule> existingSchedules) {
        if (tasks.isEmpty()) return new ArrayList<>();

        List<Individual> population = initializePopulation(tasks, classrooms, semester);

        Individual bestIndividual = null;
        for (int generation = 0; generation < MAX_GENERATIONS; generation++) {
            evaluatePopulation(population, tasks, teacherPreferences,
                    activityBlacklists, classrooms);

            Individual currentBest = findBestIndividual(population);
            if (bestIndividual == null || currentBest.fitness > bestIndividual.fitness) {
                bestIndividual = currentBest.copy();
            }

            population = evolvePopulation(population, tasks, classrooms, semester);

            if (bestIndividual.fitness >= 0.95) {
                break;
            }
        }

        return decodeIndividual(bestIndividual, tasks, semester);
    }

    /**
     * 初始化种群
     */
    private List<Individual> initializePopulation(List<TeachingTask> tasks,
                                                  List<Classroom> classrooms,
                                                  Semester semester) {
        List<Individual> population = new ArrayList<>();

        for (int i = 0; i < POPULATION_SIZE; i++) {
            Individual individual = new Individual(tasks.size());

            for (int j = 0; j < tasks.size(); j++) {
                TeachingTask task = tasks.get(j);
                Schedule schedule = generateRandomSchedule(task, classrooms, semester);
                individual.schedules[j] = schedule;
            }

            population.add(individual);
        }

        return population;
    }

    /**
     * 生成随机排课方案
     */
    private Schedule generateRandomSchedule(TeachingTask task, List<Classroom> classrooms,
                                            Semester semester) {
        List<Classroom> suitableClassrooms = findSuitableClassrooms(
                task.getCoursePart() != null ? task.getCoursePart().getRequiredRoomType() : null,
                task.getStudentGroup(), classrooms, task.getCampusId()
        );

        if (suitableClassrooms.isEmpty()) {
            return null;
        }

        Random random = ThreadLocalRandom.current();
        Classroom selectedClassroom = suitableClassrooms.get(random.nextInt(suitableClassrooms.size()));

        int dayOfWeek = random.nextInt(5) + 1;
        int lessonStart = (random.nextInt(6) * 2) + 1;
        int lessonsPerSession = task.getLessonsPerWeek().intValue();
        int lessonEnd = lessonStart + lessonsPerSession - 1;

        if (lessonEnd > 12) lessonEnd = 12;

        Schedule schedule = new Schedule();
        schedule.setTaskId(task.getTaskId());
        schedule.setClassroomId(selectedClassroom.getClassroomId());
        schedule.setSemesterId(semester.getSemesterId());
        schedule.setDayOfWeek(dayOfWeek);
        schedule.setLessonStart(lessonStart);
        schedule.setLessonEnd(lessonEnd);
        schedule.setWeekPattern(1L << (random.nextInt(semester.getTotalWeeks())));

        return schedule;
    }

    /**
     * 评估种群中每个个体的适应度
     */
    private void evaluatePopulation(List<Individual> population, List<TeachingTask> tasks,
                                    List<TeacherTimeWhitelist> teacherPreferences,
                                    List<ActivityBlacklist> activityBlacklists,
                                    List<Classroom> classrooms) {
        for (Individual individual : population) {
            individual.fitness = calculateFitness(individual, tasks, teacherPreferences,
                    activityBlacklists, classrooms);
        }
    }

    /**
     * 计算个体适应度
     */
    private double calculateFitness(Individual individual, List<TeachingTask> tasks,
                                    List<TeacherTimeWhitelist> teacherPreferences,
                                    List<ActivityBlacklist> activityBlacklists,
                                    List<Classroom> classrooms) {
        double fitness = 0.0;
        int totalTasks = tasks.size();
        int successfullyScheduled = 0;

        for (int i = 0; i < individual.schedules.length; i++) {
            Schedule schedule = individual.schedules[i];
            if (schedule == null) continue;

            TeachingTask task = tasks.get(i);
            boolean hasConstraintViolation = false;

            Classroom classroom = getClassroomById(schedule.getClassroomId(), classrooms);
            if (classroom != null && task.getStudentGroup() != null) {
                if (!ConstraintChecker.checkCapacity(classroom, task.getStudentGroup())) {
                    hasConstraintViolation = true;
                }
            }

            if (task.getCoursePart() != null && classroom != null) {
                if (!ConstraintChecker.checkRoomType(classroom,
                        task.getCoursePart().getRequiredRoomType())) {
                    hasConstraintViolation = true;
                }
            }

            if (classroom != null && task.getCampusId() != null &&
                    !classroom.getCampusId().equals(task.getCampusId())) {
                hasConstraintViolation = true;
            }

            boolean timeConflict = false;
            for (int j = 0; j < i; j++) {
                Schedule otherSchedule = individual.schedules[j];
                if (otherSchedule != null && hasTimeConflict(schedule, otherSchedule, tasks.get(i), tasks.get(j))) {
                    timeConflict = true;
                    break;
                }
            }

            if (!hasConstraintViolation && !timeConflict) {
                successfullyScheduled++;

                List<TeacherTimeWhitelist> teacherPrefs = teacherPreferences.stream()
                        .filter(p -> p.getTeacherId().equals(task.getTeacherId()))
                        .collect(Collectors.toList());

                if (!teacherPrefs.isEmpty() &&
                        ConstraintChecker.checkTeacherPreference(teacherPrefs,
                                schedule.getDayOfWeek(), schedule.getLessonStart(), schedule.getLessonEnd())) {
                    fitness += 0.05;
                }
            }
        }

        fitness += (double) successfullyScheduled / totalTasks;
        int conflicts = totalTasks - successfullyScheduled;
        fitness -= (double) conflicts / (totalTasks * 2);

        return Math.max(0.0, Math.min(1.0, fitness));
    }

    /**
     * 检查两个排课是否有时间冲突
     */
    private boolean hasTimeConflict(Schedule s1, Schedule s2, TeachingTask t1, TeachingTask t2) {
        if (s1.getClassroomId() != null && s1.getClassroomId().equals(s2.getClassroomId()) &&
                isTimeSlotConflict(s1, s2)) {
            return true;
        }

        if (t1.getTeacherId() != null && t1.getTeacherId().equals(t2.getTeacherId()) &&
                isTimeSlotConflict(s1, s2)) {
            return true;
        }

        return t1.getClassId() != null && t1.getClassId().equals(t2.getClassId()) &&
                isTimeSlotConflict(s1, s2);
    }

    /**
     * 检查时间槽是否冲突
     */
    private boolean isTimeSlotConflict(Schedule s1, Schedule s2) {
        if (!s1.getDayOfWeek().equals(s2.getDayOfWeek())) {
            return false;
        }

        if ((s1.getWeekPattern() & s2.getWeekPattern()) == 0) {
            return false;
        }

        return Math.max(s1.getLessonStart(), s2.getLessonStart()) <
                Math.min(s1.getLessonEnd(), s2.getLessonEnd());
    }

    /**
     * 演化种群（选择、交叉、变异）
     */
    private List<Individual> evolvePopulation(List<Individual> population,
                                              List<TeachingTask> tasks,
                                              List<Classroom> classrooms,
                                              Semester semester) {
        List<Individual> newPopulation = new ArrayList<>();

        Individual best = findBestIndividual(population);
        newPopulation.add(best.copy());

        while (newPopulation.size() < POPULATION_SIZE) {
            Individual parent1 = tournamentSelection(population);
            Individual parent2 = tournamentSelection(population);

            Individual child = crossover(parent1, parent2, tasks, classrooms, semester);
            mutate(child, tasks, classrooms, semester);

            newPopulation.add(child);
        }

        return newPopulation;
    }

    /**
     * 锦标赛选择
     */
    private Individual tournamentSelection(List<Individual> population) {
        Individual best = null;

        for (int i = 0; i < TOURNAMENT_SIZE; i++) {
            Individual candidate = population.get(ThreadLocalRandom.current().nextInt(population.size()));
            if (best == null || candidate.fitness > best.fitness) {
                best = candidate;
            }
        }

        return best;
    }

    /**
     * 交叉操作
     */
    private Individual crossover(Individual parent1, Individual parent2,
                                 List<TeachingTask> tasks, List<Classroom> classrooms,
                                 Semester semester) {
        Individual child = new Individual(tasks.size());
        Random random = ThreadLocalRandom.current();

        for (int i = 0; i < child.schedules.length; i++) {
            if (random.nextDouble() < CROSSOVER_RATE) {
                child.schedules[i] = (random.nextBoolean() ?
                        parent1.schedules[i] : parent2.schedules[i]) != null ?
                        copySchedule(parent1.schedules[i] != null ?
                                parent1.schedules[i] : parent2.schedules[i]) : null;
            } else {
                if (parent1.schedules[i] != null) {
                    child.schedules[i] = copySchedule(parent1.schedules[i]);
                } else {
                    child.schedules[i] = generateRandomSchedule(tasks.get(i), classrooms, semester);
                }
            }
        }

        return child;
    }

    /**
     * 变异操作
     */
    private void mutate(Individual individual, List<TeachingTask> tasks,
                        List<Classroom> classrooms, Semester semester) {
        Random random = ThreadLocalRandom.current();

        for (int i = 0; i < individual.schedules.length; i++) {
            if (random.nextDouble() < MUTATION_RATE) {
                individual.schedules[i] = generateRandomSchedule(tasks.get(i), classrooms, semester);
            }
        }
    }

    /**
     * 解码个体为排课结果
     */
    private List<Schedule> decodeIndividual(Individual individual,
                                            List<TeachingTask> tasks,
                                            Semester semester) {
        List<Schedule> result = new ArrayList<>();

        for (int i = 0; i < individual.schedules.length; i++) {
            if (individual.schedules[i] != null) {
                Schedule schedule = copySchedule(individual.schedules[i]);
                schedule.setScheduleId(null);
                result.add(schedule);
            }
        }

        return result;
    }

    /**
     * 查找最佳个体
     */
    private Individual findBestIndividual(List<Individual> population) {
        Individual best = null;
        for (Individual individual : population) {
            if (best == null || individual.fitness > best.fitness) {
                best = individual;
            }
        }
        return best;
    }

    /**
     * 根据 ID 获取教室
     */
    private Classroom getClassroomById(Integer id, List<Classroom> classrooms) {
        for (Classroom classroom : classrooms) {
            if (classroom.getClassroomId().equals(id)) {
                return classroom;
            }
        }
        return null;
    }

    /**
     * 复制排课记录
     */
    private Schedule copySchedule(Schedule original) {
        if (original == null) return null;

        Schedule copy = new Schedule();
        copy.setTaskId(original.getTaskId());
        copy.setClassroomId(original.getClassroomId());
        copy.setSemesterId(original.getSemesterId());
        copy.setDayOfWeek(original.getDayOfWeek());
        copy.setLessonStart(original.getLessonStart());
        copy.setLessonEnd(original.getLessonEnd());
        copy.setWeekPattern(original.getWeekPattern());
        return copy;
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
                .filter(room -> room.getDeleted() == 0)
                .filter(room -> targetCampusId == null || room.getCampusId().equals(targetCampusId))
                .collect(Collectors.toList());
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

    private int countConstraints(TeachingTask task, List<Classroom> classrooms) {
        int count = 0;
        if (task.getCoursePart() != null && task.getCoursePart().getRequiredRoomType() != null) count++;
        if (task.getStudentGroup() != null) count++;
        if (task.getCampusId() != null) count++;
        if (task.getTeacherId() != null) count++;
        return count;
    }

    private List<Integer[]> getPreferredTimeSlots(Integer teacherId, List<TeacherTimeWhitelist> preferences) {
        List<Integer[]> slots = new ArrayList<>();
        for (TeacherTimeWhitelist pref : preferences) {
            if (pref.getTeacherId().equals(teacherId)) {
                for (int hour = pref.getLessonStart(); hour <= pref.getLessonEnd() - 1; hour++) {
                    slots.add(new Integer[]{pref.getDayOfWeek(), hour});
                }
            }
        }
        return slots;
    }

    private List<Integer[]> generateAllTimeSlots() {
        List<Integer[]> slots = new ArrayList<>();
        for (int day = 1; day <= 5; day++) {
            for (int hour = 1; hour <= 11; hour++) {
                slots.add(new Integer[]{day, hour});
            }
        }
        return slots;
    }

    private List<TeachingTask> findUnscheduledTasks(List<TeachingTask> originalTasks,
                                                    List<Schedule> scheduledSchedules) {
        Set<Integer> scheduledTaskIds = scheduledSchedules.stream()
                .map(Schedule::getTaskId)
                .collect(Collectors.toSet());

        return originalTasks.stream()
                .filter(task -> !scheduledTaskIds.contains(task.getTaskId()))
                .collect(Collectors.toList());
    }

    /**
     * 个体类（染色体）
     */
    private static class Individual {
        Schedule[] schedules;
        double fitness;

        public Individual(int size) {
            this.schedules = new Schedule[size];
            this.fitness = 0.0;
        }

        public Individual copy() {
            Individual copy = new Individual(this.schedules.length);
            for (int i = 0; i < this.schedules.length; i++) {
                copy.schedules[i] = this.schedules[i] != null ?
                        copySchedule(this.schedules[i]) : null;
            }
            copy.fitness = this.fitness;
            return copy;
        }

        private static Schedule copySchedule(Schedule original) {
            if (original == null) return null;

            Schedule copy = new Schedule();
            copy.setTaskId(original.getTaskId());
            copy.setClassroomId(original.getClassroomId());
            copy.setSemesterId(original.getSemesterId());
            copy.setDayOfWeek(original.getDayOfWeek());
            copy.setLessonStart(original.getLessonStart());
            copy.setLessonEnd(original.getLessonEnd());
            copy.setWeekPattern(original.getWeekPattern());
            return copy;
        }
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

        public SchedulingStatus getStatus() { return status; }
        public void setStatus(SchedulingStatus status) { this.status = status; }
        public List<Schedule> getScheduledClasses() { return scheduledClasses; }
        public void setScheduledClasses(List<Schedule> scheduledClasses) { this.scheduledClasses = scheduledClasses; }
        public List<TeachingTask> getFailedTasks() { return failedTasks; }
        public void setFailedTasks(List<TeachingTask> failedTasks) { this.failedTasks = failedTasks; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    /**
     * 排课状态枚举
     */
    public enum SchedulingStatus {
        SUCCESS, FAILED, PARTIAL_SUCCESS
    }

    /**
     * 排课约束检查器
     */
    public static class ConstraintChecker {
        public static boolean checkCapacity(Classroom classroom, StudentGroup studentGroup) {
            if (classroom == null || studentGroup == null) return false;
            return classroom.getCapacity() >= studentGroup.getStudentCount();
        }

        public static boolean checkRoomType(Classroom classroom, RoomType requiredRoomType) {
            if (classroom == null || requiredRoomType == null) return false;
            return classroom.getRoomTypeId().equals(requiredRoomType.getTypeId());
        }

        public static boolean checkTeacherPreference(List<TeacherTimeWhitelist> preferences,
                                                     Integer dayOfWeek, Integer lessonStart, Integer lessonEnd) {
            if (dayOfWeek == null || lessonStart == null || lessonEnd == null) return false;

            for (TeacherTimeWhitelist pref : preferences) {
                if (pref.getDayOfWeek().equals(dayOfWeek)) {
                    if (lessonStart >= pref.getLessonStart() && lessonEnd <= pref.getLessonEnd()) {
                        return true;
                    }
                }
            }
            return false;
        }

        public static boolean checkTimeConflictForClassroom(List<Schedule> schedules, Integer classroomId,
                                                            int day, int startHour, int endHour, long weekPattern) {
            for (Schedule schedule : schedules) {
                if (schedule.getClassroomId().equals(classroomId) &&
                        schedule.getDayOfWeek().equals(day) &&
                        (schedule.getWeekPattern() & weekPattern) != 0 &&
                        !(endHour < schedule.getLessonStart() || startHour > schedule.getLessonEnd())) {
                    return true;
                }
            }
            return false;
        }

        public static boolean checkTimeConflictForTeacher(List<Schedule> schedules, Integer teacherId,
                                                          int day, int startHour, int endHour, long weekPattern,
                                                          List<TeachingTask> allTasks) {
            Map<Integer, Integer> teacherTaskMap = new HashMap<>();
            for (TeachingTask task : allTasks) {
                teacherTaskMap.put(task.getTaskId(), task.getTeacherId());
            }

            for (Schedule schedule : schedules) {
                Integer scheduleTeacherId = teacherTaskMap.get(schedule.getTaskId());
                if (teacherId.equals(scheduleTeacherId) &&
                        schedule.getDayOfWeek().equals(day) &&
                        (schedule.getWeekPattern() & weekPattern) != 0 &&
                        !(endHour < schedule.getLessonStart() || startHour > schedule.getLessonEnd())) {
                    return true;
                }
            }
            return false;
        }

        public static boolean checkTimeConflictForClass(List<Schedule> schedules, Integer classId,
                                                        int day, int startHour, int endHour, long weekPattern) {
            Map<Integer, Integer> classTaskMap = new HashMap<>();
            for (Schedule schedule : schedules) {
                if (schedule.getTaskId() != null) {
                    classTaskMap.put(schedule.getTaskId(), classId);
                }
            }
            return false;
        }

        public static boolean checkCrossCampusConflict(List<Schedule> schedules, Integer teacherId,
                                                       int day, int startHour, int endHour, long weekPattern,
                                                       List<TeachingTask> allTasks) {
            return false;
        }

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

                    String timeSlot = blacklist.getTimeSlot();
                    if (isInTimeSlot(lessonStart, lessonEnd, timeSlot)) {
                        return false;
                    }
                }
            }
            return true;
        }

        private static boolean isInTimeSlot(Integer lessonStart, Integer lessonEnd, String timeSlot) {
            if (lessonStart == null || lessonEnd == null) return false;

            switch (timeSlot) {
                case "MORNING": return lessonStart >= 1 && lessonEnd <= 4;
                case "AFTERNOON": return lessonStart >= 5 && lessonEnd <= 8;
                case "EVENING": return lessonStart >= 9;
                case "ALL_DAY": return true;
                default: return false;
            }
        }
    }
}
