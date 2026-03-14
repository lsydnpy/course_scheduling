//
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
// * 基于遗传算法的课程自动排课系统
// * 更适合大规模复杂排课场景
// */
//@Service
//public class GeneticAlgorithmScheduling {
//
//    // 遗传算法参数 - 种群大小设置为100
//    private static final int POPULATION_SIZE = 100;
//    // 遗传算法参数 - 最大迭代次数设置为500
//    private static final int MAX_GENERATIONS = 500;
//    // 遗传算法参数 - 变异率设置为0.1
//    private static final double MUTATION_RATE = 0.1;
//    // 遗传算法参数 - 交叉率设置为0.8
//    private static final double CROSSOVER_RATE = 0.8;
//    // 注入教学任务数据访问对象
//    @Resource
//    private TeachingTaskMapper teachingTaskMapper;
//    // 注入排课数据访问对象
//    @Resource
//    private ScheduleMapper scheduleMapper;
//    // 注入教师时间黑名单数据访问对象
//    @Resource
//    private TeacherTimeWhitelistMapper TeacherTimeWhitelistMapper;
//    // 注入活动黑名单数据访问对象
//    @Resource
//    private ActivityBlacklistMapper activityBlacklistMapper;
//    // 注入教室数据访问对象
//    @Resource
//    private ClassroomMapper classroomMapper;
//    // 注入教师数据访问对象
//    @Resource
//    private TeacherMapper teacherMapper;
//    // 注入学生组数据访问对象
//    @Resource
//    private StudentGroupMapper studentGroupMapper;
//    // 注入课程数据访问对象
//    @Resource
//    private CourseMapper courseMapper;
//    // 注入课程部分数据访问对象
//    @Resource
//    private CoursePartMapper coursePartMapper;
//    // 注入房间类型数据访问对象
//    @Resource
//    private RoomTypeMapper roomTypeMapper;
//    // 注入校区数据访问对象
//    @Resource
//    private CampusMapper campusMapper;
//    // 注入学期数据访问对象
//    @Resource
//    private SemesterMapper semesterMapper;
//    // 注入学院数据访问对象
//    @Resource
//    private CollegeMapper collegeMapper;
//
//    /**
//     * 执行自动排课
//     */
//    public Result<String> autoSchedule(Integer semesterId) {
//        try {
//            // 获取学期信息
//            Semester semester = semesterMapper.selectById(semesterId);
//            if (semester == null) {
//                // 如果学期不存在，返回错误结果
//                return Result.fail("学期不存在");
//            }
//
//            // 获取待排课的教学任务
//            List<TeachingTask> tasks = teachingTaskMapper.selectList(
//                    new LambdaQueryWrapper<TeachingTask>()
//                            .eq(TeachingTask::getSemesterId, semesterId)
//                            .eq(TeachingTask::getStatus, "CONFIRMED")
//                            .eq(TeachingTask::getDeleted, 0)
//            );
//
//            if (tasks.isEmpty()) {
//                // 如果没有需要排课的教学任务，返回成功但无任务
//                return Result.ok("没有需要排课的教学任务");
//            }
//
//            // 清除已有排课
//            clearExistingSchedules(semesterId);
//
//            // 准备排课数据
//            SchedulingData data = prepareSchedulingData(tasks, semesterId);
//
//            // 运行遗传算法
//            ScheduleIndividual bestSolution = runGeneticAlgorithm(data);
//
//            if (bestSolution != null && bestSolution.fitness >= 95.0) { // 设定阈值
//                // 保存最佳方案
//                saveBestSchedule(bestSolution, data);
//                // 返回成功结果，包含适应度信息
//                return Result.ok("排课成功，适应度：" + String.format("%.2f", bestSolution.fitness));
//            } else {
//                // 返回失败结果，说明适应度不足
//                return Result.fail("排课失败，最佳方案适应度不足：" + String.format("%.2f", bestSolution != null ? bestSolution.fitness : 0.0));
//            }
//        } catch (Exception e) {
//            // 捕获异常并返回失败结果
//            return Result.fail("排课失败: " + e.getMessage());
//        }
//    }
//
//    /**
//     * 准备排课数据
//     */
//    private SchedulingData prepareSchedulingData(List<TeachingTask> tasks, Integer semesterId) {
//        // 创建排课数据对象
//        SchedulingData data = new SchedulingData();
//        // 设置任务列表
//        data.tasks = tasks;
//        // 设置学期ID
//        data.semesterId = semesterId;
//
//        // 获取约束信息 - 获取教师时间黑名单
//        data.teacherBlacklists = TeacherTimeWhitelistMapper.selectList(
//                new LambdaQueryWrapper<TeacherTimeWhitelist>()
//                        .eq(TeacherTimeWhitelist::getSemesterId, semesterId)
//                        .eq(TeacherTimeWhitelist::getDeleted, 0)
//        );
//
//        // 获取约束信息 - 获取活动黑名单
//        data.activityBlacklists = activityBlacklistMapper.selectList(
//                new LambdaQueryWrapper<ActivityBlacklist>()
//                        .eq(ActivityBlacklist::getSemesterId, semesterId)
//                        .eq(ActivityBlacklist::getDeleted, 0)
//        );
//
//        // 获取教室信息 - 获取所有可用教室
//        data.allClassrooms = classroomMapper.selectList(
//                new LambdaQueryWrapper<Classroom>()
//                        .eq(Classroom::getIsAvailable, true)
//                        .eq(Classroom::getDeleted, 0)
//        );
//
//        // 构建教师映射表，便于快速查找
//        data.teacherMap = teacherMapper.selectList(new LambdaQueryWrapper<>()).stream()
//                .collect(Collectors.toMap(Teacher::getTeacherId, t -> t));
//
//        // 构建教室映射表，便于快速查找
//        data.classroomMap = data.allClassrooms.stream()
//                .collect(Collectors.toMap(Classroom::getClassroomId, c -> c));
//
//        // 构建学生组映射表，便于快速查找
//        data.studentGroupMap = studentGroupMapper.selectList(new LambdaQueryWrapper<>()).stream()
//                .collect(Collectors.toMap(StudentGroup::getClassId, g -> g));
//
//        // 构建课程映射表，便于快速查找
//        data.courseMap = courseMapper.selectList(new LambdaQueryWrapper<>()).stream()
//                .collect(Collectors.toMap(Course::getCourseId, c -> c));
//
//        // 构建课程部分映射表，便于快速查找
//        data.coursePartMap = coursePartMapper.selectList(new LambdaQueryWrapper<>()).stream()
//                .collect(Collectors.toMap(CoursePart::getPartId, p -> p));
//
//        // 返回准备好的排课数据
//        return data;
//    }
//
//    /**
//     * 运行遗传算法
//     */
//    private ScheduleIndividual runGeneticAlgorithm(SchedulingData data) {
//        // 初始化种群
//        List<ScheduleIndividual> population = initializePopulation(data);
//
//        // 初始化最佳个体和最佳适应度
//        ScheduleIndividual bestIndividual = null;
//        double bestFitness = 0.0;
//
//        // 迭代指定代数
//        for (int generation = 0; generation < MAX_GENERATIONS; generation++) {
//            // 计算每个个体的适应度
//            for (ScheduleIndividual individual : population) {
//                individual.calculateFitness(data);
//            }
//
//            // 找出当前代的最佳个体
//            ScheduleIndividual currentBest = population.stream()
//                    .max(Comparator.comparingDouble(ind -> ind.fitness))
//                    .orElse(null);
//
//            // 更新全局最佳个体和适应度
//            if (currentBest != null && currentBest.fitness > bestFitness) {
//                bestFitness = currentBest.fitness;
//                bestIndividual = currentBest;
//
//                // 如果适应度达到较高水平，提前结束
//                if (bestFitness >= 98.0) {
//                    break;
//                }
//            }
//
//            // 选择、交叉、变异，生成新种群
//            List<ScheduleIndividual> newPopulation = new ArrayList<>();
//
//            // 保留精英个体（最优个体）
//            newPopulation.add(bestIndividual.clone());
//
//            // 生成剩余个体
//            while (newPopulation.size() < POPULATION_SIZE) {
//                // 锦标赛选择两个父代个体
//                ScheduleIndividual parent1 = tournamentSelection(population);
//                ScheduleIndividual parent2 = tournamentSelection(population);
//
//                // 交叉操作生成子代
//                ScheduleIndividual child = crossover(parent1, parent2, data);
//                // 变异操作
//                mutate(child, data);
//
//                // 将子代加入新种群
//                newPopulation.add(child);
//            }
//
//            // 更新种群
//            population = newPopulation;
//        }
//
//        // 返回找到的最佳个体
//        return bestIndividual;
//    }
//
//    /**
//     * 初始化种群
//     */
//    private List<ScheduleIndividual> initializePopulation(SchedulingData data) {
//        // 创建种群列表
//        List<ScheduleIndividual> population = new ArrayList<>();
//
//        // 生成指定数量的个体
//        for (int i = 0; i < POPULATION_SIZE; i++) {
//            // 创建个体，大小为任务数量
//            ScheduleIndividual individual = new ScheduleIndividual(data.tasks.size());
//
//            // 为每个任务随机分配时间和地点
//            for (int j = 0; j < data.tasks.size(); j++) {
//                TeachingTask task = data.tasks.get(j);
//
//                // 获取可用的时间和教室
//                List<TimeSlot> availableSlots = findAvailableTimeSlots(task, data);
//
//                if (!availableSlots.isEmpty()) {
//                    // 随机选择一个可用时间槽
//                    TimeSlot selectedSlot = availableSlots.get(ThreadLocalRandom.current().nextInt(availableSlots.size()));
//                    individual.schedule[j] = selectedSlot;
//                } else {
//                    // 如果找不到可用时间槽，使用默认值（表示未安排）
//                    individual.schedule[j] = new TimeSlot(-1, -1, -1, -1, -1);
//                }
//            }
//
//            // 将个体添加到种群
//            population.add(individual);
//        }
//
//        // 返回初始化的种群
//        return population;
//    }
//
//    /**
//     * 查找可用的时间槽
//     */
//    private List<TimeSlot> findAvailableTimeSlots(TeachingTask task, SchedulingData data) {
//        // 初始化可用时间槽列表
//        List<TimeSlot> availableSlots = new ArrayList<>();
//
//        // 获取任务相关信息
//        CoursePart part = data.coursePartMap.get(task.getPartId());
//        Teacher teacher = data.teacherMap.get(task.getTeacherId());
//        StudentGroup group = data.studentGroupMap.get(task.getClassId());
//
//        // 获取教师所属学院的教室
//        College teacherCollege = collegeMapper.selectById(teacher.getCollegeId());
//        Campus campus = campusMapper.selectById(teacherCollege.getCampusId());
//
//        // 过滤符合条件的教室
//        List<Classroom> suitableClassrooms = data.allClassrooms.stream()
//                .filter(c -> c.getRoomTypeId().equals(part.getRequiredRoomTypeId())) // 教室类型匹配
//                .filter(c -> c.getCapacity() >= group.getStudentCount()) // 容量满足需求
//                .filter(c -> c.getCampusId().equals(campus.getCampusId())) // 校区匹配
//                .collect(Collectors.toList());
//
//        // 尝试安排在一周的不同天和时段
//        for (int week = 1; week <= 20; week++) { // 假设最多20周
//            for (int day = 1; day <= 7; day++) {
//                // 遍历可能的开始课时
//                for (int startLesson = 1; startLesson <= 12 - task.getLessonsPerWeek().intValue() + 1; startLesson++) {
//                    // 计算结束课时
//                    int endLesson = startLesson + task.getLessonsPerWeek().intValue() - 1;
//
//                    // 检查教师时间黑名单
//                    boolean teacherAvailable = isTeacherAvailable(teacher.getTeacherId(), day, startLesson, endLesson, data);
//
//                    // 检查活动黑名单
//                    boolean activityAvailable = isActivityAvailable(day, startLesson, endLesson, data);
//
//                    if (teacherAvailable && activityAvailable) {
//                        // 尝试分配教室
//                        for (Classroom classroom : suitableClassrooms) {
//                            // 创建时间槽并添加到可用列表
//                            availableSlots.add(new TimeSlot(
//                                    task.getTaskId(),
//                                    classroom.getClassroomId(),
//                                    week,
//                                    day,
//                                    startLesson,
//                                    endLesson
//                            ));
//                        }
//                    }
//                }
//            }
//        }
//
//        // 返回可用时间槽列表
//        return availableSlots;
//    }
//
//    /**
//     * 检查教师是否可用
//     */
//    private boolean isTeacherAvailable(Integer teacherId, int day, int startLesson, int endLesson, SchedulingData data) {
//        // 遍历教师时间黑名单
//        for (TeacherTimeWhitelist blacklist : data.teacherBlacklists) {
//            // 检查是否为同一教师且同一天
//            if (blacklist.getTeacherId().equals(teacherId) &&
//                    blacklist.getDayOfWeek() == day) {
//                // 检查时间段是否有冲突（endLesson < blacklist.lessonStart 或 startLesson > blacklist.lessonEnd 时无冲突）
//                if (!(endLesson < blacklist.getLessonStart() || startLesson > blacklist.getLessonEnd())) {
//                    return false; // 存在冲突，教师不可用
//                }
//            }
//        }
//        return true; // 教师可用
//    }
//
//    /**
//     * 检查活动时间是否可用
//     */
//    private boolean isActivityAvailable(int day, int startLesson, int endLesson, SchedulingData data) {
//        // 遍历活动黑名单
//        for (ActivityBlacklist blacklist : data.activityBlacklists) {
//            // 检查是否为同一天
//            if (blacklist.getDayOfWeek() == day) {
//                // 检查时间段是否有冲突
//                if (!(endLesson < blacklist.getLessonStart() || startLesson > blacklist.getLessonEnd())) {
//                    return false; // 存在冲突，时间不可用
//                }
//            }
//        }
//        return true; // 时间可用
//    }
//
//    /**
//     * 锦标赛选择
//     */
//    private ScheduleIndividual tournamentSelection(List<ScheduleIndividual> population) {
//        // 锦标赛规模设置为5
//        int tournamentSize = 5;
//        ScheduleIndividual best = null;
//
//        // 进行指定次数的锦标赛选择
//        for (int i = 0; i < tournamentSize; i++) {
//            // 随机选择一个个体
//            ScheduleIndividual candidate = population.get(ThreadLocalRandom.current().nextInt(population.size()));
//            // 比较适应度，选择更优个体
//            if (best == null || candidate.fitness > best.fitness) {
//                best = candidate;
//            }
//        }
//
//        // 返回锦标赛中的最优个体
//        return best;
//    }
//
//    /**
//     * 交叉操作
//     */
//    private ScheduleIndividual crossover(ScheduleIndividual parent1, ScheduleIndividual parent2, SchedulingData data) {
//        // 创建子代个体，大小与父代相同
//        ScheduleIndividual child = new ScheduleIndividual(parent1.schedule.length);
//
//        // 根据交叉率决定是否进行交叉
//        if (ThreadLocalRandom.current().nextDouble() < CROSSOVER_RATE) {
//            // 随机选择交叉点
//            int crossoverPoint = ThreadLocalRandom.current().nextInt(parent1.schedule.length);
//
//            // 交叉操作：前半部分来自parent1，后半部分来自parent2
//            for (int i = 0; i < parent1.schedule.length; i++) {
//                if (i < crossoverPoint) {
//                    child.schedule[i] = parent1.schedule[i].clone();
//                } else {
//                    child.schedule[i] = parent2.schedule[i].clone();
//                }
//            }
//        } else {
//            // 不交叉，随机选择一个父代
//            ScheduleIndividual selectedParent = ThreadLocalRandom.current().nextBoolean() ? parent1 : parent2;
//            for (int i = 0; i < selectedParent.schedule.length; i++) {
//                child.schedule[i] = selectedParent.schedule[i].clone();
//            }
//        }
//
//        // 返回子代个体
//        return child;
//    }
//
//    /**
//     * 变异操作
//     */
//    private void mutate(ScheduleIndividual individual, SchedulingData data) {
//        // 对个体的每个时间槽进行变异
//        for (int i = 0; i < individual.schedule.length; i++) {
//            // 根据变异率决定是否进行变异
//            if (ThreadLocalRandom.current().nextDouble() < MUTATION_RATE) {
//                TeachingTask task = data.tasks.get(i);
//
//                // 尝试找到一个新的可用时间槽
//                List<TimeSlot> availableSlots = findAvailableTimeSlots(task, data);
//                if (!availableSlots.isEmpty()) {
//                    // 随机选择一个可用时间槽进行替换
//                    TimeSlot newSlot = availableSlots.get(ThreadLocalRandom.current().nextInt(availableSlots.size()));
//                    individual.schedule[i] = newSlot;
//                }
//            }
//        }
//    }
//
//    /**
//     * 保存最佳排课方案
//     */
//    private void saveBestSchedule(ScheduleIndividual bestSolution, SchedulingData data) {
//        // 遍历最佳解决方案中的每个时间槽
//        for (int i = 0; i < bestSolution.schedule.length; i++) {
//            TimeSlot slot = bestSolution.schedule[i];
//            if (slot.taskId != -1) { // 有效安排（非未安排标记）
//                // 创建排课实体
//                Schedule schedule = new Schedule();
//                schedule.setTaskId(slot.taskId); // 设置任务ID
//                schedule.setClassroomId(slot.classroomId); // 设置教室ID
//                schedule.setSemesterId(data.semesterId); // 设置学期ID
//                schedule.setDayOfWeek(slot.day); // 设置星期几
//                schedule.setLessonStart(slot.startLesson); // 设置开始课时
//                schedule.setLessonEnd(slot.endLesson); // 设置结束课时
//                schedule.setWeekPattern(1L << (slot.week - 1)); // 使用位运算表示周次
//
//                // 保存排课信息到数据库
//                scheduleMapper.insert(schedule);
//            }
//        }
//    }
//
//    /**
//     * 清除指定学期的已有排课
//     */
//    private void clearExistingSchedules(Integer semesterId) {
//        // 删除指定学期的排课记录
//        scheduleMapper.delete(
//                new LambdaQueryWrapper<Schedule>()
//                        .eq(Schedule::getSemesterId, semesterId)
//        );
//    }
//
//    /**
//     * 排课数据容器
//     */
//    private static class SchedulingData {
//        // 教学任务列表
//        List<TeachingTask> tasks;
//        // 学期ID
//        Integer semesterId;
//        // 教师时间黑名单列表
//        List<TeacherTimeWhitelist> teacherBlacklists;
//        // 活动黑名单列表
//        List<ActivityBlacklist> activityBlacklists;
//        // 所有教室列表
//        List<Classroom> allClassrooms;
//        // 教师映射表（ID -> 教师对象）
//        Map<Integer, Teacher> teacherMap;
//        // 教室映射表（ID -> 教室对象）
//        Map<Integer, Classroom> classroomMap;
//        // 学生组映射表（ID -> 学生组对象）
//        Map<Integer, StudentGroup> studentGroupMap;
//        // 课程映射表（ID -> 课程对象）
//        Map<Integer, Course> courseMap;
//        // 课程部分映射表（ID -> 课程部分对象）
//        Map<Integer, CoursePart> coursePartMap;
//    }
//
//    /**
//     * 时间槽类
//     */
//    private static class TimeSlot implements Cloneable {
//        // 任务ID
//        Integer taskId;
//        // 教室ID
//        Integer classroomId;
//        // 周次
//        Integer week;
//        // 星期几
//        Integer day;
//        // 开始课时
//        Integer startLesson;
//        // 结束课时
//        Integer endLesson;
//
//        // 完整构造函数
//        public TimeSlot(Integer taskId, Integer classroomId, Integer week, Integer day, Integer startLesson, Integer endLesson) {
//            this.taskId = taskId;
//            this.classroomId = classroomId;
//            this.week = week;
//            this.day = day;
//            this.startLesson = startLesson;
//            this.endLesson = endLesson;
//        }
//
//        // 默认构造函数（用于未安排的情况）
//        public TimeSlot(Integer taskId, Integer classroomId, Integer week, Integer day, Integer lesson) {
//            this(taskId, classroomId, week, day, lesson, lesson);
//        }
//
//        // 克隆方法
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
//     * 个体类（染色体）
//     */
//    private static class ScheduleIndividual {
//        // 时间槽数组，表示个体的排课方案
//        TimeSlot[] schedule;
//        // 适应度值
//        double fitness = 0.0;
//
//        // 构造函数，指定个体大小
//        public ScheduleIndividual(int size) {
//            this.schedule = new TimeSlot[size];
//        }
//
//        // 计算适应度方法
//        public void calculateFitness(SchedulingData data) {
//            // 获取总任务数
//            int totalTasks = schedule.length;
//            // 已安排任务数
//            int scheduledTasks = 0;
//            // 冲突数量
//            int conflictCount = 0;
//
//            // 统计已安排的任务数
//            for (TimeSlot slot : schedule) {
//                if (slot.taskId != -1) scheduledTasks++;
//            }
//
//            // 检查冲突
//            // 教师时间安排映射
//            Map<String, Integer> teacherSchedule = new HashMap<>();
//            // 教室时间安排映射
//            Map<String, Integer> classroomSchedule = new HashMap<>();
//            // 班级时间安排映射
//            Map<String, Integer> classSchedule = new HashMap<>();
//
//            // 遍历所有时间槽检查冲突
//            for (int i = 0; i < schedule.length; i++) {
//                TimeSlot slot = schedule[i];
//                if (slot.taskId == -1) continue; // 跳过未安排的任务
//
//                TeachingTask task = data.tasks.get(i);
//
//                // 检查每个课时的冲突情况
//                for (int lesson = slot.startLesson; lesson <= slot.endLesson; lesson++) {
//                    // 教师时间键
//                    String teacherKey = "teacher_" + task.getTeacherId() + "_" + slot.week + "_" + slot.day + "_" + lesson;
//                    // 如果教师时间冲突，增加冲突计数
//                    if (teacherSchedule.containsKey(teacherKey)) {
//                        conflictCount++;
//                    } else {
//                        teacherSchedule.put(teacherKey, task.getTaskId());
//                    }
//
//                    // 教室时间键
//                    String classroomKey = "classroom_" + slot.classroomId + "_" + slot.week + "_" + slot.day + "_" + lesson;
//                    // 如果教室时间冲突，增加冲突计数
//                    if (classroomSchedule.containsKey(classroomKey)) {
//                        conflictCount++;
//                    } else {
//                        classroomSchedule.put(classroomKey, task.getTaskId());
//                    }
//
//                    // 班级时间键
//                    String classKey = "class_" + task.getClassId() + "_" + slot.week + "_" + slot.day + "_" + lesson;
//                    // 如果班级时间冲突，增加冲突计数
//                    if (classSchedule.containsKey(classKey)) {
//                        conflictCount++;
//                    } else {
//                        classSchedule.put(classKey, task.getTaskId());
//                    }
//                }
//            }
//
//            // 计算适应度（越高越好）
//            // 计算排课率
//            double schedulingRate = (double) scheduledTasks / totalTasks;
//            // 计算冲突惩罚
//            double conflictPenalty = Math.min(1.0, (double) conflictCount / totalTasks);
//
//            // 计算最终适应度值
//            this.fitness = Math.max(0, (schedulingRate * 100 - conflictPenalty * 20) * 100) / 100;
//        }
//
//        // 克隆方法
//        public ScheduleIndividual clone() {
//            // 创建新的个体实例
//            ScheduleIndividual cloned = new ScheduleIndividual(this.schedule.length);
//            // 复制时间槽数组
//            for (int i = 0; i < this.schedule.length; i++) {
//                cloned.schedule[i] = this.schedule[i].clone();
//            }
//            // 复制适应度值
//            cloned.fitness = this.fitness;
//            // 返回克隆的个体
//            return cloned;
//        }
//    }
//}