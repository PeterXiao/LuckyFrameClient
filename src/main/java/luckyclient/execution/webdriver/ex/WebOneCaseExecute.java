package luckyclient.execution.webdriver.ex;

import java.io.IOException;
import java.util.List;

import luckyclient.remote.api.PostServerApi;
import org.openqa.selenium.WebDriver;

import luckyclient.execution.httpinterface.TestControl;
import luckyclient.execution.webdriver.WebDriverInitialization;
import luckyclient.remote.api.GetServerApi;
import luckyclient.remote.api.serverOperation;
import luckyclient.remote.entity.ProjectCase;
import luckyclient.remote.entity.ProjectCaseParams;
import luckyclient.remote.entity.ProjectCaseSteps;
import luckyclient.utils.LogUtil;

/**
 * =================================================================
 * ����һ�������Ƶ�������������������κ�δ�������ǰ���¶Գ����������޸ĺ�������ҵ��;��Ҳ������Գ�������޸ĺ����κ���ʽ�κ�Ŀ�ĵ��ٷ�����
 * Ϊ���������ߵ��Ͷ��ɹ���LuckyFrame�ؼ���Ȩ��Ϣ�Ͻ��۸�
 * ���κ����ʻ�ӭ��ϵ�������ۡ� QQ:1573584944  seagull1985
 * =================================================================
 *
 * @author�� seagull
 * @date 2017��12��1�� ����9:29:40
 */
public class WebOneCaseExecute {

    public static void oneCaseExecuteForTast(Integer caseId, String taskid) {
        //��¼��־�����ݿ�
        serverOperation.exetype = 0;
        TestControl.TASKID = taskid;
        int drivertype = serverOperation.querydrivertype(taskid);
        WebDriver wd = null;
        try {
            wd = WebDriverInitialization.setWebDriverForTask(drivertype);
        } catch (IOException e1) {
            LogUtil.APP.error("��ʼ��WebDriver����", e1);
        }
        serverOperation caselog = new serverOperation();
        ProjectCase testcase = GetServerApi.cGetCaseByCaseId(caseId);
        //ɾ���ɵ���־
        serverOperation.deleteTaskCaseLog(testcase.getCaseId(), taskid);

        List<ProjectCaseParams> pcplist = GetServerApi.cgetParamsByProjectid(String.valueOf(testcase.getProjectId()));
        LogUtil.APP.info("��ʼִ������:��{}��......", testcase.getCaseSign());
        try {
            List<ProjectCaseSteps> steps = GetServerApi.getStepsbycaseid(testcase.getCaseId());
            WebCaseExecution.caseExcution(testcase, steps, taskid, null, wd, caselog, pcplist);
            LogUtil.APP.info("��ǰ����:��{}��ִ�����......������һ��", testcase.getCaseSign());
        } catch (Exception e) {
            LogUtil.APP.error("�û�ִ�й������׳��쳣��", e);
        }
        serverOperation.updateTaskExecuteData(taskid, 0, 2);
        //�ر������
        assert wd != null;
        wd.quit();
    }

    public static void debugoneCaseExecute(int caseId, int userId, int drivertype) {
        //����¼��־�����ݿ�
        serverOperation.exetype = 1;
        WebDriver wd = null;
        try {
            PostServerApi.cPostDebugLog(userId, caseId, "INFO", "׼����ʼ��WebDriver����...", 0);
            wd = WebDriverInitialization.setWebDriverForTask(drivertype);

            serverOperation caselog = new serverOperation();
            ProjectCase testcase = GetServerApi.cGetCaseByCaseId(caseId);

            List<ProjectCaseParams> pcplist = GetServerApi.cgetParamsByProjectid(String.valueOf(testcase.getProjectId()));
			PostServerApi.cPostDebugLog(userId, caseId, "INFO", "��ʼִ������...", 0);
            LogUtil.APP.info("��ʼִ������:��{}��......", testcase.getCaseSign());

            List<ProjectCaseSteps> steps = GetServerApi.getStepsbycaseid(testcase.getCaseId());
            WebCaseExecution.caseExcution(testcase, steps, "888888", null, wd, caselog, pcplist);
			PostServerApi.cPostDebugLog(userId, caseId, "INFOover", "��ǰ��������ȫ��ִ�����...", 1);
            LogUtil.APP.info("��ǰ����:��{}��ִ�����......", testcase.getCaseSign());
        } catch (IOException e1) {
            PostServerApi.cPostDebugLog(userId, caseId, "ERRORover", "��ʼ��WebDriver�����쳣��Ϣ��鿴�ͻ�����־...", 1);
            LogUtil.APP.error("��ʼ��WebDriver����", e1);
        } catch (Exception e) {
            LogUtil.APP.error("�û�ִ�й������׳��쳣��", e);
        }
        //�ر������
        assert wd != null;
        wd.quit();
    }

}
