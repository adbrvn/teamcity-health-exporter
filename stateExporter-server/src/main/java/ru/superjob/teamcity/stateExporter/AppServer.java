package ru.superjob.teamcity.stateExporter;

import jetbrains.buildServer.controllers.AuthorizationInterceptor;
import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AppServer extends BaseController {
    final static String ENDPOINT_PATH = "/state/";

    private PluginDescriptor myDescriptor;
    private BuildQueue queue;
    private BuildAgentManager agentManager;

    public AppServer (WebControllerManager manager,
                      PluginDescriptor descriptor,
                      BuildQueue queue,
                      BuildAgentManager agentManager,
                      AuthorizationInterceptor authorizationInterceptor) {
        authorizationInterceptor.addPathNotRequiringAuth(ENDPOINT_PATH);
        manager.registerController(ENDPOINT_PATH,this);
        myDescriptor=descriptor;
        this.queue = queue;
        this.agentManager = agentManager;
    }

    @Nullable
    @Override
    protected ModelAndView doHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws Exception {
        JSONObject state = new JSONObject();
        long maxQTime = 0;
        int totalAgents = 0, idleAgents = 0, activeAgents = 0;

        for (SQueuedBuild qItem : queue.getItems()) {
            TimeInterval curTInterval = qItem.getBuildEstimates().getTimeInterval();
            if (null != curTInterval) {
                long curQTime = curTInterval.getStartPoint().getRelativeSeconds();
                if (curQTime != Long.MAX_VALUE && curQTime > maxQTime) {
                    maxQTime = curQTime;
                }
            }
        }

        for (SBuildAgent agent : agentManager.getRegisteredAgents()) {
            totalAgents++;
            if (agent.getIdleTime() > 0) {
                idleAgents++;
            } else {
                activeAgents++;
            }
        }

        state.put("queueLength", queue.getItems().size());
        state.put("maxQueueTime", maxQTime);
        state.put("allAgents", totalAgents);
        state.put("activeAgents", activeAgents);
        state.put("idleAgents", idleAgents);


        ModelAndView mv = new ModelAndView(myDescriptor.getPluginResourcesPath("json.jsp"));
        mv.getModel().put("data", state.toString());

        return mv;
    }
}
