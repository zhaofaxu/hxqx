package cn.stylefeng.guns.sys.modular.tenant.service.impl;

import cn.stylefeng.guns.base.db.entity.DatabaseInfo;
import cn.stylefeng.guns.base.db.util.DbUtil;
import cn.stylefeng.guns.base.db.util.SqlRunUtil;
import cn.stylefeng.guns.base.pojo.page.LayuiPageFactory;
import cn.stylefeng.guns.base.pojo.page.LayuiPageInfo;
import cn.stylefeng.guns.sys.core.util.SaltUtil;
import cn.stylefeng.guns.sys.modular.db.factory.DataBaseInfoFactory;
import cn.stylefeng.guns.sys.modular.db.model.params.DatabaseInfoParam;
import cn.stylefeng.guns.sys.modular.db.service.DatabaseInfoService;
import cn.stylefeng.guns.sys.modular.tenant.entity.TenantInfo;
import cn.stylefeng.guns.sys.modular.tenant.mapper.TenantInfoMapper;
import cn.stylefeng.guns.sys.modular.tenant.model.params.TenantInfoParam;
import cn.stylefeng.guns.sys.modular.tenant.model.result.TenantInfoResult;
import cn.stylefeng.guns.sys.modular.tenant.service.TenantInfoService;
import cn.stylefeng.roses.core.config.properties.DruidProperties;
import cn.stylefeng.roses.core.mutidatasource.DataSourceContextHolder;
import cn.stylefeng.roses.core.util.SpringContextHolder;
import cn.stylefeng.roses.core.util.ToolUtil;
import cn.stylefeng.roses.groovy.core.data.SqlExecute;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.List;

import static cn.stylefeng.guns.sys.modular.tenant.consts.TenantConstants.TENANT_DB_PREFIX;

/**
 * <p>
 * 租户表 服务实现类
 * </p>
 *
 * @author stylefeng
 * @since 2019-06-16
 */
@Service
public class TenantInfoServiceImpl extends ServiceImpl<TenantInfoMapper, TenantInfo> implements TenantInfoService {

    @Autowired
    private DatabaseInfoService databaseInfoService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(TenantInfoParam param) {

        //创建租户数据库
        DruidProperties druidProperties = SpringContextHolder.getBean(DruidProperties.class);
        String databaseName = TENANT_DB_PREFIX + param.getCode();
        DbUtil.createDatabase(druidProperties, databaseName);

        //创建租户的数据源记录
        DatabaseInfoParam dataBaseInfo = DataBaseInfoFactory.createDataBaseInfo(druidProperties, databaseName);
        databaseInfoService.add(dataBaseInfo);

        //初始化租户的数据库
        SqlRunUtil.runClassPathSql("tenant_init.sql", databaseName);

        //插入租户记录
        TenantInfo entity = getEntity(param);
        entity.setDbName(databaseName);
        this.save(entity);

        //切换数据源到新的租户
        DataSourceContextHolder.setDataSourceType(databaseName);

        try {
            String randomSalt = SaltUtil.getRandomSalt();
            String md5 = SaltUtil.md5Encrypt(param.getAdminPassword(), randomSalt);

            //初始化新租户的用户名和密码
            SqlExecute.update("update sys_user set password = " + md5 + ", salt = " + randomSalt + "  where account = 'admin'", null);
        } finally {
            DataSourceContextHolder.clearDataSourceType();
        }

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(TenantInfoParam param) {

        //获取租户信息
        TenantInfo tenantInfo = this.getById(param.getTenantId());

        //删除租户信息
        this.removeById(getKey(param));

        //删除对应的数据源
        databaseInfoService.remove(
                new QueryWrapper<DatabaseInfo>().eq("db_name", tenantInfo.getDbName()));
    }

    @Override
    public void update(TenantInfoParam param) {
        TenantInfo oldEntity = getOldEntity(param);
        TenantInfo newEntity = getEntity(param);
        ToolUtil.copyProperties(newEntity, oldEntity);
        this.updateById(newEntity);
    }

    @Override
    public TenantInfoResult findBySpec(TenantInfoParam param) {
        return null;
    }

    @Override
    public List<TenantInfoResult> findListBySpec(TenantInfoParam param) {
        return null;
    }

    @Override
    public LayuiPageInfo findPageBySpec(TenantInfoParam param) {
        Page pageContext = getPageContext();
        IPage page = this.baseMapper.customPageList(pageContext, param);
        return LayuiPageFactory.createPageInfo(page);
    }

    @Override
    public TenantInfo getByCode(String code) {
        return this.getOne(new QueryWrapper<TenantInfo>().eq("code", code));
    }

    private Serializable getKey(TenantInfoParam param) {
        return param.getTenantId();
    }

    private Page getPageContext() {
        return LayuiPageFactory.defaultPage();
    }

    private TenantInfo getOldEntity(TenantInfoParam param) {
        return this.getById(getKey(param));
    }

    private TenantInfo getEntity(TenantInfoParam param) {
        TenantInfo entity = new TenantInfo();
        ToolUtil.copyProperties(param, entity);
        return entity;
    }

}
