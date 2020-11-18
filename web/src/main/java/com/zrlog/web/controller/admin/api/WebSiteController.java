package com.zrlog.web.controller.admin.api;

import com.hibegin.common.util.BeanUtil;
import com.jfinal.config.Plugins;
import com.jfinal.core.JFinal;
import com.jfinal.plugin.IPlugin;
import com.zrlog.business.rest.base.*;
import com.zrlog.business.rest.response.UpdateRecordResponse;
import com.zrlog.business.rest.response.VersionResponse;
import com.zrlog.business.rest.response.WebSiteSettingUpdateResponse;
import com.zrlog.business.rest.response.WebSiteSettingsResponse;
import com.zrlog.business.service.WebSiteService;
import com.zrlog.common.type.AutoUpgradeVersionType;
import com.zrlog.model.WebSite;
import com.zrlog.util.BlogBuildInfoUtil;
import com.zrlog.util.ZrLogUtil;
import com.zrlog.web.annotation.RefreshCache;
import com.zrlog.web.controller.BaseController;
import com.zrlog.web.interceptor.TemplateHelper;
import com.zrlog.web.plugin.UpdateVersionPlugin;

import java.util.Map;
import java.util.Map.Entry;

public class WebSiteController extends BaseController {

    private final WebSiteService webSiteService = new WebSiteService();

    public VersionResponse version() {
        VersionResponse versionResponse = new VersionResponse();
        versionResponse.setBuildId(BlogBuildInfoUtil.getBuildId());
        versionResponse.setVersion(BlogBuildInfoUtil.getVersion());
        versionResponse.setChangelog(UpdateVersionPlugin.getChangeLog(BlogBuildInfoUtil.getVersion(), BlogBuildInfoUtil.getBuildId()));
        return versionResponse;
    }

    public WebSiteSettingsResponse settings() {
        return webSiteService.loadSettings(getRequest(), TemplateHelper.getTemplatePathByCookie(getRequest().getCookies()));
    }

    @RefreshCache
    public WebSiteSettingUpdateResponse basic() {
        return update(BasicWebSiteRequest.class);
    }

    private WebSiteSettingUpdateResponse update(Class<?> t) {
        Map<String, Object> requestMap = BeanUtil.convert(ZrLogUtil.convertRequestBody(getRequest(), t), Map.class);
        for (Entry<String, Object> param : requestMap.entrySet()) {
            new WebSite().updateByKV(param.getKey(), param.getValue());
        }
        WebSiteSettingUpdateResponse updateResponse = new WebSiteSettingUpdateResponse();
        updateResponse.setError(0);
        return updateResponse;
    }

    @RefreshCache
    public WebSiteSettingUpdateResponse blog() {
        return update(BlogWebSiteRequest.class);
    }

    @RefreshCache
    public WebSiteSettingUpdateResponse other() {
        return update(OtherWebSiteRequest.class);
    }

    @RefreshCache
    public WebSiteSettingUpdateResponse upgrade() {
        UpgradeWebSiteRequest request = ZrLogUtil.convertRequestBody(getRequest(), UpgradeWebSiteRequest.class);
        Map<String, Object> requestMap = BeanUtil.convert(request, Map.class);
        for (Entry<String, Object> param : requestMap.entrySet()) {
            new WebSite().updateByKV(param.getKey(), param.getValue());
        }
        Plugins plugins = (Plugins) JFinal.me().getServletContext().getAttribute("plugins");
        if (AutoUpgradeVersionType.cycle(request.getAutoUpgradeVersion().intValue()) == AutoUpgradeVersionType.NEVER) {
            for (IPlugin plugin : plugins.getPluginList()) {
                if (plugin instanceof UpdateVersionPlugin) {
                    plugin.stop();
                }
            }
        } else {
            for (IPlugin plugin : plugins.getPluginList()) {
                if (plugin instanceof UpdateVersionPlugin) {
                    plugin.start();
                }
            }
        }
        WebSiteSettingUpdateResponse recordResponse = new WebSiteSettingUpdateResponse();
        recordResponse.setError(0);
        return recordResponse;
    }

    @RefreshCache
    public WebSiteSettingUpdateResponse template() {
        return update(Object.class);
    }
}
