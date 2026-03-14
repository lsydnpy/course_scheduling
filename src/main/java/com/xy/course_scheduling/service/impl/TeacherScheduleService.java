package com.xy.course_scheduling.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xy.course_scheduling.entity.*;
import com.xy.course_scheduling.mapper.*;
import com.xy.course_scheduling.service.ClassroomService;
import com.xy.course_scheduling.service.CourseService;
import com.xy.course_scheduling.service.StudentGroupService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 教师课表服务实现类
 */
@Service
public class TeacherScheduleService {

    /**
     * 排课信息Mapper接口注入
     */
    @Resource
    private ScheduleMapper scheduleMapper;

    /**
     * 教师信息Mapper接口注入
     */
    @Resource
    private TeacherMapper teacherMapper;

    /**
     * 教学任务Mapper接口注入
     */
    @Resource
    private TeachingTaskMapper teachingTaskMapper;
    @Resource
    private CourseMapper courseMapper;
    @Resource
    private ClassroomMapper classroomMapper;
    @Resource
    private StudentGroupMapper studentGroupMapper;
    @Autowired
    private CoursePartMapper coursePartMapper;

    /**
     * 获取指定教师的课表
     *
     * @param teacherId 教师ID
     * @return 课表数据
     */
    public List<ClassTable> getTeacherSchedule(Integer teacherId) {
        // 查询教师信息，验证教师是否存在
        Teacher teacher = teacherMapper.selectById(teacherId);
        if (teacher == null) {
            throw new RuntimeException("教师不存在");
        }

        // 创建排课信息查询条件包装器
        LambdaQueryWrapper<Schedule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Schedule::getTaskId, null); // 需要通过teaching_task关联查询

        // 由于Schedule实体中没有直接关联Teacher，需要先查询该教师的教学任务
        List<TeachingTask> teachingTasks = getTeachingTasksByTeacherId(teacherId);

        // 收集所有相关的任务ID，用于后续查询排课信息
        List<Integer> taskIds = new ArrayList<>();
        for (TeachingTask task : teachingTasks) {
            taskIds.add(task.getTaskId());
        }

        // 如果没有找到相关任务，返回空课表
        if (taskIds.isEmpty()) {
            return generateEmptyClassTable();
        }

        // 查询这些任务的排课信息，按星期和节次排序
        LambdaQueryWrapper<Schedule> scheduleQuery = new LambdaQueryWrapper<>();
        scheduleQuery.in(Schedule::getTaskId, taskIds);
        scheduleQuery.orderByAsc(Schedule::getDayOfWeek).orderByAsc(Schedule::getLessonStart);

        List<Schedule> schedules = scheduleMapper.selectList(scheduleQuery);

        // 构建并返回课表数据
        return buildClassTable(schedules, teachingTasks);
    }

    /**
     * 根据教师ID获取教学任务列表
     *
     * @param teacherId 教师ID
     * @return 教学任务列表
     */
    private List<TeachingTask> getTeachingTasksByTeacherId(Integer teacherId) {
        // 创建教学任务查询条件包装器
        LambdaQueryWrapper<TeachingTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TeachingTask::getTeacherId, teacherId);
        // 执行查询并返回结果
        return teachingTaskMapper.selectList(queryWrapper);
    }

    /**
     * 构建课表数据
     *
     * @param schedules     排课信息列表
     * @param teachingTasks 教学任务列表
     * @return 课表数据列表
     */
    private List<ClassTable> buildClassTable(List<Schedule> schedules, List<TeachingTask> teachingTasks) {
        // 初始化课表，总共假设每天最多12节课
        List<ClassTable> classTables = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            ClassTable classTable = new ClassTable();
            classTable.setSection(i + "");          // 设置节次
            classTable.setMon(new ArrayList<>());                  // 初始化周一为空
            classTable.setTue(new ArrayList<>());                  // 初始化周二为空
            classTable.setWed(new ArrayList<>());                  // 初始化周三为空
            classTable.setThu(new ArrayList<>());                  // 初始化周四为空
            classTable.setFri(new ArrayList<>());                  // 初始化周五为空
            classTable.setSat(new ArrayList<>());                  // 初始化周六为空
            classTable.setSun(new ArrayList<>());                  // 初始化周日为空
            classTables.add(classTable);
        }

        // 填充课表数据
        for (Schedule schedule : schedules) {
            // 获取对应的TeachingTask对象
            TeachingTask task = findTeachingTaskById(teachingTasks, schedule.getTaskId());

            if (task != null) {
                task.setCourse(courseMapper.selectById(task.getCourseId()));
                task.setCoursePart(coursePartMapper.selectById(task.getPartId()));
                String courseName = task.getCourse() != null ? task.getCourse().getCourseName() + "(" + (task.getCoursePart().getPartType().equals("THEORY") ? "理论" : "实践") + ")" : "未知课程";

                // 获取教室信息，如果教室信息为空则显示"未知教室"
                schedule.setClassroom(classroomMapper.selectById(schedule.getClassroomId()));
                String classroomCode = schedule.getClassroom() != null ? schedule.getClassroom().getClassroomCode() : "未知教室";

                // 获取班级信息，如果班级信息为空则显示"未知班级"
                task.setStudentGroup(studentGroupMapper.selectById(task.getClassId()));
                String className = task.getStudentGroup() != null ? task.getStudentGroup().getClassName() : "未知班级";

                // 获取并解析周次信息
                String weekInfo = parseWeekPattern(schedule.getWeekPattern(), schedule.getLessonStart(), schedule.getLessonEnd());
                // 计算课表行索引（节次从1开始，数组索引从0开始）
                for (Integer i = schedule.getLessonStart(); i < schedule.getLessonEnd() + 1; i++) {
                    ClassTable classTable = classTables.get(i - 1);//迭代每一节课
                    List<TaskTable> courseInfo = null;
                    boolean isExist = false;
                    // 根据星期几填入课程信息到对应列
                    switch (schedule.getDayOfWeek()) {
                        case 1:
                            courseInfo = classTable.getMon();
                            isExist = false;
                            for (TaskTable taskTable : courseInfo) {
                                if (taskTable.getTaskId().equals(task.getTaskId())) {
                                    isExist = true;
                                    taskTable.getWeeks().add(Long.numberOfTrailingZeros(schedule.getWeekPattern()) + 1);
                                    break;
                                }
                            }
                            if (!isExist) {
                                TaskTable taskTable = new TaskTable();
                                taskTable.setTaskId(task.getTaskId());
                                taskTable.setCourseName(courseName);
                                taskTable.setClassRoomCode(classroomCode);
                                taskTable.setStudentGroup(className);
                                taskTable.setWeeks(new ArrayList<>());
                                taskTable.getWeeks().add(Long.numberOfTrailingZeros(schedule.getWeekPattern()) + 1);
                                courseInfo.add(taskTable);
                            }
                            break;
                        case 2:
                            courseInfo = classTable.getTue();
                            isExist = false;
                            for (TaskTable taskTable : courseInfo) {
                                if (taskTable.getTaskId().equals(task.getTaskId())) {
                                    isExist = true;
                                    taskTable.getWeeks().add(Long.numberOfTrailingZeros(schedule.getWeekPattern()) + 1);
                                    break;
                                }
                            }
                            if (!isExist) {
                                TaskTable taskTable = new TaskTable();
                                taskTable.setTaskId(task.getTaskId());
                                taskTable.setCourseName(courseName);
                                taskTable.setClassRoomCode(classroomCode);
                                taskTable.setStudentGroup(className);
                                taskTable.setWeeks(new ArrayList<>());
                                taskTable.getWeeks().add(Long.numberOfTrailingZeros(schedule.getWeekPattern()) + 1);
                                courseInfo.add(taskTable);
                            }
                            break;
                        case 3:
                            courseInfo = classTable.getWed();
                            isExist = false;
                            for (TaskTable taskTable : courseInfo) {
                                if (taskTable.getTaskId().equals(task.getTaskId())) {
                                    isExist = true;
                                    taskTable.getWeeks().add(Long.numberOfTrailingZeros(schedule.getWeekPattern()) + 1);
                                    break;
                                }
                            }
                            if (!isExist) {
                                TaskTable taskTable = new TaskTable();
                                taskTable.setTaskId(task.getTaskId());
                                taskTable.setCourseName(courseName);
                                taskTable.setClassRoomCode(classroomCode);
                                taskTable.setStudentGroup(className);
                                taskTable.setWeeks(new ArrayList<>());
                                taskTable.getWeeks().add(Long.numberOfTrailingZeros(schedule.getWeekPattern()) + 1);
                                courseInfo.add(taskTable);
                            }
                            break;
                        case 4:
                            courseInfo = classTable.getThu();
                            isExist = false;
                            for (TaskTable taskTable : courseInfo) {
                                if (taskTable.getTaskId().equals(task.getTaskId())) {
                                    isExist = true;
                                    taskTable.getWeeks().add(Long.numberOfTrailingZeros(schedule.getWeekPattern()) + 1);
                                    break;
                                }
                            }
                            if (!isExist) {
                                TaskTable taskTable = new TaskTable();
                                taskTable.setTaskId(task.getTaskId());
                                taskTable.setCourseName(courseName);
                                taskTable.setClassRoomCode(classroomCode);
                                taskTable.setStudentGroup(className);
                                taskTable.setWeeks(new ArrayList<>());
                                taskTable.getWeeks().add(Long.numberOfTrailingZeros(schedule.getWeekPattern()) + 1);
                                courseInfo.add(taskTable);
                            }
                            break;
                        case 5:
                            courseInfo = classTable.getFri();
                            isExist = false;
                            for (TaskTable taskTable : courseInfo) {
                                if (taskTable.getTaskId().equals(task.getTaskId())) {
                                    isExist = true;
                                    taskTable.getWeeks().add(Long.numberOfTrailingZeros(schedule.getWeekPattern()) + 1);
                                    break;
                                }
                            }
                            if (!isExist) {
                                TaskTable taskTable = new TaskTable();
                                taskTable.setTaskId(task.getTaskId());
                                taskTable.setCourseName(courseName);
                                taskTable.setClassRoomCode(classroomCode);
                                taskTable.setStudentGroup(className);
                                taskTable.setWeeks(new ArrayList<>());
                                taskTable.getWeeks().add(Long.numberOfTrailingZeros(schedule.getWeekPattern()) + 1);
                                courseInfo.add(taskTable);
                            }
                            break;
                        case 6:
                            courseInfo = classTable.getSat();
                            isExist = false;
                            for (TaskTable taskTable : courseInfo) {
                                if (taskTable.getTaskId().equals(task.getTaskId())) {
                                    isExist = true;
                                    taskTable.getWeeks().add(Long.numberOfTrailingZeros(schedule.getWeekPattern()) + 1);
                                    break;
                                }
                            }
                            if (!isExist) {
                                TaskTable taskTable = new TaskTable();
                                taskTable.setTaskId(task.getTaskId());
                                taskTable.setCourseName(courseName);
                                taskTable.setClassRoomCode(classroomCode);
                                taskTable.setStudentGroup(className);
                                taskTable.setWeeks(new ArrayList<>());
                                taskTable.getWeeks().add(Long.numberOfTrailingZeros(schedule.getWeekPattern()) + 1);
                                courseInfo.add(taskTable);
                            }
                            break;
                        case 7:
                            courseInfo = classTable.getSun();
                            isExist = false;
                            for (TaskTable taskTable : courseInfo) {
                                if (taskTable.getTaskId().equals(task.getTaskId())) {
                                    isExist = true;
                                    taskTable.getWeeks().add(Long.numberOfTrailingZeros(schedule.getWeekPattern()) + 1);
                                    break;
                                }
                            }
                            if (!isExist) {
                                TaskTable taskTable = new TaskTable();
                                taskTable.setTaskId(task.getTaskId());
                                taskTable.setCourseName(courseName);
                                taskTable.setClassRoomCode(classroomCode);
                                taskTable.setStudentGroup(className);
                                taskTable.setWeeks(new ArrayList<>());
                                taskTable.getWeeks().add(Long.numberOfTrailingZeros(schedule.getWeekPattern()) + 1);
                                courseInfo.add(taskTable);
                            }
                            break;
                    }
                }
//                int lessonIndex = schedule.getLessonStart() - 1;
//                if (lessonIndex >= 0 && lessonIndex < classTables.size()) {
//                    ClassTable classTable = classTables.get(lessonIndex);
//
//                    // 根据星期几填入课程信息到对应列
//                    switch (schedule.getDayOfWeek()) {
//                        case 1:
//                            classTable.setMon(courseInfo);  // 周一
//                            break;
//                        case 2:
//                            classTable.setTue(courseInfo);  // 周二
//                            break;
//                        case 3:
//                            classTable.setWed(courseInfo);  // 周三
//                            break;
//                        case 4:
//                            classTable.setThu(courseInfo);  // 周四
//                            break;
//                        case 5:
//                            classTable.setFri(courseInfo);  // 周五
//                            break;
//                        case 6:
//                            classTable.setSat(courseInfo);  // 周六
//                            break;
//                        case 7:
//                            classTable.setSun(courseInfo);  // 周日
//                            break;
//                    }
//                }
            }
        }

        return classTables;
    }

    /**
     * 根据任务ID查找TeachingTask对象
     *
     * @param tasks  教学任务列表
     * @param taskId 任务ID
     * @return 对应的TeachingTask对象，未找到则返回null
     */
    private TeachingTask findTeachingTaskById(List<TeachingTask> tasks, Integer taskId) {
        // 遍历教学任务列表查找匹配的任务
        for (TeachingTask task : tasks) {
            if (task.getTaskId().equals(taskId)) {
                return task;  // 找到匹配的任务则返回
            }
        }
        return null;  // 未找到匹配的任务
    }

    /**
     * 构造课程显示信息字符串
     *
     * @param task     教学任务对象
     * @param schedule 排课信息对象
     * @return 格式化的课程信息字符串
     */
    private String getCourseInfo(TeachingTask task, Schedule schedule) {
        // 获取课程名称，如果课程信息为空则显示"未知课程"
        task.setCourse(courseMapper.selectById(task.getCourseId()));
        String courseName = task.getCourse() != null ? task.getCourse().getCourseName() : "未知课程";

        // 获取教室信息，如果教室信息为空则显示"未知教室"
        schedule.setClassroom(classroomMapper.selectById(schedule.getClassroomId()));
        String classroomCode = schedule.getClassroom() != null ? schedule.getClassroom().getClassroomCode() : "未知教室";

        // 获取班级信息，如果班级信息为空则显示"未知班级"
        task.setStudentGroup(studentGroupMapper.selectById(task.getClassId()));
        String className = task.getStudentGroup() != null ? task.getStudentGroup().getClassName() : "未知班级";

        // 获取并解析周次信息
        String weekInfo = parseWeekPattern(schedule.getWeekPattern(), schedule.getLessonStart(), schedule.getLessonEnd());

        // 格式化返回课程信息字符串
        return String.format("%s[%s]%s\n%s(%d-%d节)",
                courseName, className, classroomCode,
                weekInfo, schedule.getLessonStart(), schedule.getLessonEnd());
    }

    /**
     * 解析周次模式为可读字符串
     *
     * @param weekPattern 周次位图模式
     * @param startLesson 开始节次
     * @param endLesson   结束节次
     * @return 解析后的周次信息字符串
     */
    private String parseWeekPattern(Long weekPattern, int startLesson, int endLesson) {
        // 如果周次模式为空或为0，表示每周都有课
        if (weekPattern == null || weekPattern == 0) {
            return "每周";
        }

        // 使用StringBuilder构建周次信息字符串
        StringBuilder sb = new StringBuilder();
        // 假设最多32周，检查每一位是否为1
        for (int i = 0; i < 32; i++) {
            if ((weekPattern & (1L << i)) != 0) {
                // 如果不是第一个周次，添加逗号分隔
                if (sb.length() > 0) {
                    sb.append(",");
                }
                // 添加周次信息
                sb.append((i + 1) + "周");
            }
        }

        // 如果没有解析出任何周次信息，默认显示"每周"
        if (sb.length() == 0) {
            sb.append("每周");
        }

        return sb.toString();
    }

    /**
     * 生成空课表数据
     *
     * @return 空课表数据列表
     */
    private List<ClassTable> generateEmptyClassTable() {
        // 创建课表列表
        List<ClassTable> classTables = new ArrayList<>();
        // 生成12节空课表数据（覆盖一整天的课程安排）
        for (int i = 1; i <= 12; i++) {
            ClassTable classTable = new ClassTable();
            classTable.setSection(i + "");          // 设置节次
            classTable.setMon(new ArrayList<>());                  // 周一无课程
            classTable.setTue(new ArrayList<>());                  // 周二无课程
            classTable.setWed(new ArrayList<>());                  // 周三无课程
            classTable.setThu(new ArrayList<>());                  // 周四无课程
            classTable.setFri(new ArrayList<>());                  // 周五无课程
            classTable.setSat(new ArrayList<>());                  // 周六无课程
            classTable.setSun(new ArrayList<>());                  // 周日无课程
            classTables.add(classTable);
        }
        return classTables;
    }
}