package com.xy.course_scheduling.algorithm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xy.course_scheduling.entity.*;
import com.xy.course_scheduling.mapper.*;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 基于遗传算法的智能课程调度系统
 */
@Service
public class GeneticCourseSchedulingAlgorithm {

    // 遗传算法参数
    private static final int POPULATION_SIZE = 100;
    private static final double CROSSOVER_RATE = 0.8;
    private static final double MUTATION_RATE = 0.1;
    private static final int MAX_GENERATIONS = 200;
    private static final int TOURNAMENT_SIZE = 5;
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
     * 使用遗传算法生成排课方案
     */
    public SchedulingResult generateScheduleGenetic(Integer semesterId) {
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
            loadRelatedData(teachingTasks);

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

            // 初始化种群
            List<Individual> population = initializePopulation(teachingTasks, classrooms, semester);

            // 遗传算法主循环
            Individual bestIndividual = null;
            for (int generation = 0; generation < MAX_GENERATIONS; generation++) {
                // 评估种群
                evaluatePopulation(population, teachingTasks, teacherPreferences,
                        activityBlacklists, classrooms);

                // 找出最优个体
                Individual currentBest = findBestIndividual(population);
                if (bestIndividual == null || currentBest.fitness > bestIndividual.fitness) {
                    bestIndividual = currentBest.copy();
                }

                // 生成新种群
                population = evolvePopulation(population, teachingTasks, classrooms, semester);

                System.out.println("第 " + (generation + 1) + " 代，最佳适应度: " + bestIndividual.fitness);

                // 如果达到满意解，提前终止
                if (bestIndividual.fitness >= 0.99) {
                    break;
                }
            }

            // 解码最优个体为排课结果
            List<Schedule> scheduledClasses = decodeIndividual(bestIndividual, teachingTasks, semester);
            List<TeachingTask> failedTasks = findFailedTasks(bestIndividual, teachingTasks);

            // 统计结果
            String message = String.format("遗传算法排课 - 成功排课%d门，失败%d门",
                    scheduledClasses.size(), failedTasks.size());

            SchedulingStatus status = failedTasks.isEmpty() ?
                    SchedulingStatus.SUCCESS :
                    (scheduledClasses.isEmpty() ? SchedulingStatus.FAILED : SchedulingStatus.PARTIAL_SUCCESS);

            return new SchedulingResult(status, scheduledClasses, failedTasks, message);

        } catch (Exception e) {
            e.printStackTrace();
            return new SchedulingResult(SchedulingStatus.FAILED, new ArrayList<>(),
                    new ArrayList<>(), "排课过程中发生错误: " + e.getMessage());
        }
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

                // 为每个任务随机分配时间和教室
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
        // 查找合适的教室
        List<Classroom> suitableClassrooms = findSuitableClassrooms(
                task.getCoursePart() != null ? task.getCoursePart().getRequiredRoomType() : null,
                task.getStudentGroup(), classrooms, task.getCampusId()
        );

        if (suitableClassrooms.isEmpty()) {
            // 返回空排课表示无法安排
            return null;
        }

        Random random = ThreadLocalRandom.current();
        Classroom selectedClassroom = suitableClassrooms.get(random.nextInt(suitableClassrooms.size()));

        // 随机选择时间和日期
        int dayOfWeek = random.nextInt(5) + 1; // 1-5 (周一到周五)
        int lessonStart = (random.nextInt(6) * 2) + 1; // 奇数节课开始 (1,3,5,7,9,11)
        int lessonsPerSession = task.getLessonsPerWeek().intValue();
        int lessonEnd = lessonStart + lessonsPerSession - 1;

        // 确保不超出范围
        if (lessonEnd > 12) lessonEnd = 12;

        Schedule schedule = new Schedule();
        schedule.setTaskId(task.getTaskId());
        schedule.setClassroomId(selectedClassroom.getClassroomId());
        schedule.setSemesterId(semester.getSemesterId());
        schedule.setDayOfWeek(dayOfWeek);
        schedule.setLessonStart(lessonStart);
        schedule.setLessonEnd(lessonEnd);
        schedule.setWeekPattern(1L << (random.nextInt(semester.getTotalWeeks()))); // 随机周次

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

        // 检查每个排课是否有效
        for (int i = 0; i < individual.schedules.length; i++) {
            Schedule schedule = individual.schedules[i];
            if (schedule == null) continue;

            TeachingTask task = tasks.get(i);

            // 检查各种约束
            boolean hasConstraintViolation = false;

            // 1. 教室容量检查
            Classroom classroom = getClassroomById(schedule.getClassroomId(), classrooms);
            if (classroom != null && task.getStudentGroup() != null) {
                if (!ConstraintChecker.checkCapacity(classroom, task.getStudentGroup())) {
                    hasConstraintViolation = true;
                }
            }

            // 2. 教室类型检查
            if (task.getCoursePart() != null && classroom != null) {
                if (!ConstraintChecker.checkRoomType(classroom,
                        task.getCoursePart().getRequiredRoomType())) {
                    hasConstraintViolation = true;
                }
            }

            // 3. 教室校区匹配检查
            if (classroom != null && task.getCampusId() != null &&
                    !classroom.getCampusId().equals(task.getCampusId())) {
                hasConstraintViolation = true;
            }

            // 4. 时间冲突检查（与已安排的课程冲突）
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

                // 额外奖励：符合教师偏好的时间
                List<TeacherTimeWhitelist> teacherPrefs = teacherPreferences.stream()
                        .filter(p -> p.getTeacherId().equals(task.getTeacherId()))
                        .collect(Collectors.toList());

                if (!teacherPrefs.isEmpty() &&
                        ConstraintChecker.checkTeacherPreference(teacherPrefs,
                                schedule.getDayOfWeek(), schedule.getLessonStart(), schedule.getLessonEnd())) {
                    fitness += 0.1; // 教师偏好奖励
                }
            }
        }

        // 基础适应度：成功安排的课程比例
        fitness += (double) successfullyScheduled / totalTasks;

        // 冲突惩罚
        int conflicts = totalTasks - successfullyScheduled;
        fitness -= (double) conflicts / (totalTasks * 2);

        // 确保适应度在[0, 1]范围内
        return Math.max(0.0, Math.min(1.0, fitness));
    }

    /**
     * 检查两个排课是否有时间冲突
     */
    private boolean hasTimeConflict(Schedule s1, Schedule s2, TeachingTask t1, TeachingTask t2) {
        // 检查教室冲突
        if (s1.getClassroomId() != null && s1.getClassroomId().equals(s2.getClassroomId()) &&
                isTimeSlotConflict(s1, s2)) {
            return true;
        }

        // 检查教师冲突
        if (t1.getTeacherId() != null && t1.getTeacherId().equals(t2.getTeacherId()) &&
                isTimeSlotConflict(s1, s2)) {
            return true;
        }

        // 检查学生班级冲突
        return t1.getClassId() != null && t1.getClassId().equals(t2.getClassId()) &&
                isTimeSlotConflict(s1, s2);
    }

    /**
     * 检查时间槽是否冲突
     */
    private boolean isTimeSlotConflict(Schedule s1, Schedule s2) {
        // 检查是否是同一天
        if (!s1.getDayOfWeek().equals(s2.getDayOfWeek())) {
            return false;
        }

        // 检查周次是否重叠
        if ((s1.getWeekPattern() & s2.getWeekPattern()) == 0) {
            return false;
        }

        // 检查时间段是否重叠
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

        // 精英保留
        Individual best = findBestIndividual(population);
        newPopulation.add(best.copy());

        // 生成剩余个体
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
                // 从父代继承
                child.schedules[i] = (random.nextBoolean() ?
                        parent1.schedules[i] : parent2.schedules[i]) != null ?
                        copySchedule(parent1.schedules[i] != null ?
                                parent1.schedules[i] : parent2.schedules[i]) : null;
            } else {
                // 保持原样或重新生成
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
                // 重新生成这个排课
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
                schedule.setScheduleId(null); // 清除ID让数据库自动生成
                result.add(schedule);
            }
        }

        return result;
    }

    /**
     * 查找未安排的任务
     */
    private List<TeachingTask> findFailedTasks(Individual individual, List<TeachingTask> tasks) {
        List<TeachingTask> failedTasks = new ArrayList<>();

        for (int i = 0; i < individual.schedules.length; i++) {
            if (individual.schedules[i] == null) {
                failedTasks.add(tasks.get(i));
            }
        }

        return failedTasks;
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
     * 加载关联数据
     */
    private void loadRelatedData(List<TeachingTask> teachingTasks) {
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
    }

    /**
     * 根据ID获取教室
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
                .filter(room -> room.getCampusId().equals(targetCampusId))
                .collect(Collectors.toList());
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
    }

    /**
     * 个体类（染色体）
     */
    private class Individual {
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
    }
}