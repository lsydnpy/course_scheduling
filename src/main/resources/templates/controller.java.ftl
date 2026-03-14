<#assign s = "String">
package ${package.Controller};

import jakarta.annotation.Resource;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import ${package.Entity}.Result;
import ${package.Entity}.${entity};
import ${package.Service}.${table.serviceName};
import java.util.List;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
<#if restControllerStyle>
import org.springframework.web.bind.annotation.RestController;
<#else>
import org.springframework.stereotype.Controller;
</#if>
<#if superControllerClass??>
import ${superControllerClass};
</#if>

/**
* <p>
    * ${table.comment!} 前端控制器
    * </p>
*
* @author ${author}
* @since ${date}
*/
<#if restControllerStyle>
@RestController
<#else>
@Controller
</#if>
@RequestMapping("<#if package.ModuleName?? && package.ModuleName != "">/${package.ModuleName}</#if>/${table.entityPath?uncap_first}")
<#if superControllerClass??>
public class ${table.controllerName} extends ${superControllerClass} {
<#else>
public class ${table.controllerName} {
</#if>

    @Resource
    private ${table.serviceName} ${table.serviceName?uncap_first};

    @PostMapping
    public Result<${entity}> save(${entity} ${entity?uncap_first}) {
        boolean save = ${table.serviceName?uncap_first}.save(${entity?uncap_first});
        if (save) {
            return Result.ok(${entity?uncap_first});
        }
        return Result.fail("添加失败");
    }

    @DeleteMapping("/{id}")
    public Result<${s}> delete(@PathVariable Long id) {
        boolean remove = ${table.serviceName?uncap_first}.removeById(id);
        if (remove) {
            return Result.ok("删除成功");
        }
        return Result.fail("删除失败");
    }

    @PutMapping
    public Result<${s}> update(${entity} ${entity?uncap_first}) {
        boolean update = ${table.serviceName?uncap_first}.updateById(${entity?uncap_first});
        if (update){
            return Result.ok("修改成功");
        }
        return Result.fail("修改失败");
    }

    @GetMapping("/{id}")
    public Result<${entity}> getById(@PathVariable Long id) {
        return Result.ok(${table.serviceName?uncap_first}.getById(id));
    }
    @GetMapping("list")
    public Result< List<${entity}>> list(${entity} ${entity?uncap_first}, Page<${entity}> page) {
        LambdaQueryWrapper<${entity}> wrapper = new LambdaQueryWrapper<>();
        <#list table.fields as field>
            <#if field.propertyType == "String">
                wrapper.like(ObjectUtils.isNotEmpty(${entity?uncap_first}.get${field.capitalName}()), ${entity}::get${field.capitalName}, ${entity?uncap_first}.get${field.capitalName}());
            </#if>
            <#if field.propertyType == "Integer">
                wrapper.eq(ObjectUtils.isNotEmpty(${entity?uncap_first}.get${field.capitalName}()), ${entity}::get${field.capitalName}, ${entity?uncap_first}.get${field.capitalName}());
            </#if>
        </#list>
        IPage<${entity}> pageData = ${table.serviceName?uncap_first}.page(page, wrapper);
        return Result.ok(pageData);
    }
}