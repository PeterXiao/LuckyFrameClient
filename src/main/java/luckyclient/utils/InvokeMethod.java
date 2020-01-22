package luckyclient.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import luckyclient.execution.dispose.ChangString;
import luckyclient.execution.dispose.ParamsManageForSteps;
import luckyclient.remote.api.GetServerApi;
import luckyclient.remote.entity.ProjectProtocolTemplate;
import luckyclient.remote.entity.ProjectTemplateParams;
import luckyclient.utils.httputils.HttpClientTools;

/**
 * =================================================================
 * ����һ�������Ƶ��������������������κ�δ��������ǰ���¶Գ����������޸ĺ�������ҵ��;��Ҳ�������Գ�������޸ĺ����κ���ʽ�κ�Ŀ�ĵ��ٷ�����
 * Ϊ���������ߵ��Ͷ��ɹ���LuckyFrame�ؼ���Ȩ��Ϣ�Ͻ��۸�
 * ���κ����ʻ�ӭ��ϵ�������ۡ� QQ:1573584944  seagull1985
 * =================================================================
 *
 * @ClassName: InvokeMethod
 * @Description: ��̬���÷���
 * @author�� seagull
 * @date 2017��9��24�� ����9:29:40
 */
public class InvokeMethod {

    /**
     * ��̬����JAVA
     * @param packagename
     * @param functionname
     * @param getParameterValues
     * @param steptype
     * @param action
     * @return
     */
    public static String callCase(String packagename, String functionname, Object[] getParameterValues, int steptype, String extend) {
        String result = "�����쳣����鿴������־��";
        try {
            if (steptype == 2) {                
                if(functionname.toLowerCase().endsWith(".py")){
                	//����Python�ű�
                	LogUtil.APP.info("׼����ʼ����Python�ű�......");
                	result = callPy(packagename, functionname, getParameterValues);
                }else{
                	//����JAVA
                    // ���÷Ǿ�̬�����õ�
                	LogUtil.APP.info("׼����ʼ����JAVA����׮����......");
                    Object server = Class.forName(packagename).newInstance();
                    @SuppressWarnings("rawtypes")
                    Class[] getParameterTypes = null;
                    if (getParameterValues != null) {
                        int paramscount = getParameterValues.length;
                        // ��ֵ���飬��������
                        getParameterTypes = new Class[paramscount];
                        for (int i = 0; i < paramscount; i++) {
                            getParameterTypes[i] = String.class;
                        }
                    }
                    Method method = getMethod(server.getClass().getMethods(), functionname, getParameterTypes);
                    if (method == null) {
                        throw new Exception("�ͻ��˱�������Ŀ¼��û���ڰ���Ϊ��" + packagename + "�����ҵ������õķ�����" + functionname + "��,���鷽�������Լ����������Ƿ�һ�£�");
                    }
                    Object str = method.invoke(server, getParameterValues);
                    if (str == null) {
                        result = "�����쳣�����ؽ����null";
                    } else {
                        result = str.toString();
                    }
                }
            } else if (steptype == 0) {
            	if(null==extend||"".equals(extend)||!extend.contains("��")){
            		result = "����ǰ������HTTP������ȷ���Ƿ�û�����ö�Ӧ��HTTPЭ��ģ��...";
            		LogUtil.APP.warn("����ǰ������HTTP������ȷ���Ƿ�û�����ö�Ӧ��HTTPЭ��ģ��...");
            		return result;
            	}
                String templateidstr = extend.substring(1, extend.indexOf("��"));
                String templatenamestr = extend.substring(extend.indexOf("��") + 1);
                LogUtil.APP.info("����ʹ��ģ�塾{}����ID:��{}������HTTP����",templatenamestr,templateidstr);

                ProjectProtocolTemplate ppt = GetServerApi.clientGetProjectProtocolTemplateByTemplateId(Integer.valueOf(templateidstr));
                if (null == ppt) {
                    LogUtil.APP.warn("Э��ģ��Ϊ�գ���������ʹ�õ�Э��ģ���Ƿ��Ѿ�ɾ����");
                    return "Э��ģ��Ϊ�գ���ȷ������ʹ�õ�ģ���Ƿ��Ѿ�ɾ����";
                }

                List<ProjectTemplateParams> paramslist = GetServerApi.clientGetProjectTemplateParamsListByTemplateId(Integer.valueOf(templateidstr));

                //����ͷ��
                Map<String, String> headmsg = new HashMap<String, String>(0);
                if (null != ppt.getHeadMsg() && !ppt.getHeadMsg().equals("") && ppt.getHeadMsg().indexOf("=") > 0) {
                    String headmsgtemp = ppt.getHeadMsg().replace("\\;", "!!!fhzh");
                    String[] temp = headmsgtemp.split(";", -1);
                    for (int i = 0; i < temp.length; i++) {
                        if (null != temp[i] && !temp[i].equals("") && temp[i].indexOf("=") > 0) {
                            String key = temp[i].substring(0, temp[i].indexOf("="));
                            String value = temp[i].substring(temp[i].indexOf("=") + 1);
                            value = value.replace("!!!fhzh",";");
                            value = ParamsManageForSteps.paramsManage(value);
                            headmsg.put(key, value);
                        }
                    }
                }

                //������������
                if (null != getParameterValues) {
                    String booleanheadmsg = "headmsg(";
                    String msgend = ")";
                    for (Object obp : getParameterValues) {
                        String paramob = obp.toString();
                        if(paramob.contains("#")){
                            String key = paramob.substring(0, paramob.indexOf("#"));
                            String value = paramob.substring(paramob.indexOf("#") + 1);
                            value = ParamsManageForSteps.paramsManage(value);
                            if (key.contains(booleanheadmsg) && key.contains(msgend)) {
                                String head = key.substring(key.indexOf(booleanheadmsg) + 8, key.lastIndexOf(msgend));
                                headmsg.put(head, value);
                                continue;
                            }
                            int replaceflag=0;
                            for (int i = 0; i < paramslist.size(); i++) {
                                ProjectTemplateParams ptp = paramslist.get(i);
                                ptp.setParamValue(ParamsManageForSteps.paramsManage(ptp.getParamValue()));
                                if("_forTextJson".equals(ptp.getParamName())){
                            		//���������滻���
                            		int index = 1;
                            		if (key.contains("[") && key.endsWith("]")) {
                            			index = Integer.valueOf(key.substring(key.lastIndexOf("[") + 1, key.lastIndexOf("]")));
                            			key = key.substring(0, key.lastIndexOf("["));
                            			LogUtil.APP.info("׼���滻JSON�����еĲ���ֵ���滻ָ����{}������...",index);
                            		} else {
                            			LogUtil.APP.info("׼���滻JSON�����еĲ���ֵ��δ��⵽ָ����������ţ�Ĭ���滻��1������...");                       			
                            		}
                            		
                                	if(ptp.getParamValue().contains("\""+key+"\":")){
                                		Map<String,String> map=ChangString.changjson(ptp.getParamValue(), key, value,index);
                                		if("true".equals(map.get("boolean"))){
                                            ptp.setParamValue(map.get("json"));
                                            paramslist.set(i, ptp);
                                            replaceflag=1;
                                            LogUtil.APP.info("�滻������{}�����...",key);
                                            break;
                                		}
                                	}else if(ptp.getParamValue().contains(key)){
                                		ptp.setParamValue(ptp.getParamValue().replace(key, value));
                                		paramslist.set(i, ptp);
                                        replaceflag=1;
                                        LogUtil.APP.info("��鵱ǰ�ı�������JSON,���ַ�����{}����ֱ�Ӱѡ�{}���滻�ɡ�{}��...",ptp.getParamValue(),key,value);
                                        break;
                                	}else{
                                		LogUtil.APP.warn("�������Ĵ��ı�ģ���Ƿ���������JSON��ʽ�����ı����Ƿ�������滻�Ĺؼ��֡�");
                                	}
                                }else{
                                    if (ptp.getParamName().equals(key)) {
                                        ptp.setParamValue(value);
                                        paramslist.set(i, ptp);
                                        replaceflag=1;
                                        LogUtil.APP.info("��ģ���в�����{}����ֵ���óɡ�{}��",key,value);
                                        break;
                                    }
                                }
                            }
                            if(replaceflag==0){
                            	LogUtil.APP.warn("���������{}��û����ģ�����ҵ����滻�Ĳ�����ӦĬ��ֵ��"
                            			+ "�����������ʧ�ܣ�����Э��ģ���д˲����Ƿ���ڡ�",key);
                            }
                        }else{
                        	LogUtil.APP.warn("�滻ģ�����ͷ�����ʧ�ܣ�ԭ������Ϊû�м�⵽#��"
                        			+ "ע��HTTP�����滻������ʽ�ǡ�headmsg(ͷ����#ͷ��ֵ)|������#����ֵ|������2#����ֵ2��");
                        }

                    }
                }
                //��������
                Map<String, Object> params = new HashMap<String, Object>(0);
                for (ProjectTemplateParams ptp : paramslist) {
                	String tempparam = "";
                	if(null!=ptp.getParamValue()){
                		tempparam =  ptp.getParamValue().replace("&quot;", "\"");
                	}else{
                		break;
                	}
                    //������������
                    if (ptp.getParamType() == 1) {
                        JSONObject json = JSONObject.parseObject(tempparam);
                        params.put(ptp.getParamName().replace("&quot;", "\""), json);
                        LogUtil.APP.info("ģ�������{}��  JSONObject���Ͳ���ֵ:��{}��",ptp.getParamName(),json.toString());
                    } else if (ptp.getParamType() == 2) {
                        JSONArray jarr = JSONArray.parseArray(tempparam);
                        params.put(ptp.getParamName().replace("&quot;", "\""), jarr);
                        LogUtil.APP.info("ģ�������{}��  JSONArray���Ͳ���ֵ:��{}��",ptp.getParamName(),jarr.toString());
                    } else if (ptp.getParamType() == 3) {
                        File file = new File(tempparam);
                        params.put(ptp.getParamName().replace("&quot;", "\""), file);
                        LogUtil.APP.info("ģ�������{}��  File���Ͳ���ֵ:��{}��",ptp.getParamName(),file.getAbsolutePath());
                    } else if (ptp.getParamType() == 4) {
                        Double dp = Double.valueOf(tempparam);
                        params.put(ptp.getParamName().replace("&quot;", "\""), dp);
                        LogUtil.APP.info("ģ�������{}��  �������Ͳ���ֵ:��{}��",ptp.getParamName(),tempparam);
                    } else if (ptp.getParamType() == 5) {
                        Boolean bn = Boolean.valueOf(tempparam);
                        params.put(ptp.getParamName().replace("&quot;", "\""), bn);
                        LogUtil.APP.info("ģ�������{}��  Boolean���Ͳ���ֵ:��{}��",ptp.getParamName(),bn);
                    } else {
                        params.put(ptp.getParamName().replace("&quot;", "\""), ptp.getParamValue().replace("&quot;", "\""));
                        LogUtil.APP.info("ģ�������{}��  String���Ͳ���ֵ:��{}��",ptp.getParamName(),ptp.getParamValue().replace("&quot;", "\""));
                    }
                }

                if (functionname.toLowerCase().equals("httpurlpost")) {
                    result = HttpClientTools.sendHttpURLPost(packagename, params,headmsg,ppt);
                } else if (functionname.toLowerCase().equals("urlpost")) {
                    result = HttpClientTools.sendURLPost(packagename, params, headmsg,ppt);
                } else if (functionname.toLowerCase().equals("getandsavefile")) {
                    String fileSavePath = System.getProperty("user.dir") + "\\HTTPSaveFile\\";
                    result = HttpClientTools.sendGetAndSaveFile(packagename, params, fileSavePath, headmsg,ppt);
                } else if (functionname.toLowerCase().equals("httpurlget")) {
                    result = HttpClientTools.sendHttpURLGet(packagename, params, headmsg,ppt);
                } else if (functionname.toLowerCase().equals("urlget")) {
                    result = HttpClientTools.sendURLGet(packagename, params, headmsg,ppt);
                } else if (functionname.toLowerCase().equals("httpclientpost")) {
                    result = HttpClientTools.httpClientPost(packagename, params, headmsg , ppt);
                } else if (functionname.toLowerCase().equals("httpclientuploadfile")) {
                    result = HttpClientTools.httpClientUploadFile(packagename, params, headmsg , ppt);
                } else if (functionname.toLowerCase().equals("httpclientpostjson")) {
                    result = HttpClientTools.httpClientPostJson(packagename, params, headmsg , ppt);
                } else if (functionname.toLowerCase().equals("httpurldelete")) {
                    result = HttpClientTools.sendHttpURLDel(packagename, params, headmsg,ppt);
                } else if (functionname.toLowerCase().equals("httpclientdeletejson")) {
                    result = HttpClientTools.httpClientDeleteJson(packagename, params, headmsg,ppt);
                } else if (functionname.toLowerCase().equals("httpclientpatchjson")) {
                    result = HttpClientTools.httpClientPatchJson(packagename, params, headmsg, ppt);
                } else if (functionname.toLowerCase().equals("httpclientputjson")) {
                    result = HttpClientTools.httpClientPutJson(packagename, params, headmsg , ppt);
                } else if (functionname.toLowerCase().equals("httpclientput")) {
                    result = HttpClientTools.httpClientPut(packagename, params, headmsg , ppt);
                } else if (functionname.toLowerCase().equals("httpclientget")) {
                    result = HttpClientTools.httpClientGet(packagename, params, headmsg, ppt);
                } else if (functionname.toLowerCase().equals("httpclientpostxml")) {
                    result = HttpClientTools.httpClientPostXml(packagename, params, headmsg, ppt);
                } else {
                    LogUtil.APP.warn("����HTTP���������쳣����⵽�Ĳ���������:{}",functionname);
                    result = "�����쳣����鿴������־��";
                }
            } else if (steptype == 4) {
                String templateidstr = extend.substring(1, extend.indexOf("��"));
                String templatenamestr = extend.substring(extend.indexOf("��") + 1);
                LogUtil.APP.info("����ʹ��ģ�塾{}����ID:��{}�� ����SOCKET����",templatenamestr,templateidstr);

                ProjectProtocolTemplate ppt = GetServerApi.clientGetProjectProtocolTemplateByTemplateId(Integer.valueOf(templateidstr));
                if (null == ppt) {
                    LogUtil.APP.warn("Э��ģ��Ϊ�գ���������ʹ�õ�Э��ģ���Ƿ��Ѿ�ɾ����");
                    return "Э��ģ��Ϊ�գ���ȷ������ʹ�õ�ģ���Ƿ��Ѿ�ɾ����";
                }
                
                List<ProjectTemplateParams> paramslist = GetServerApi.clientGetProjectTemplateParamsListByTemplateId(Integer.valueOf(templateidstr));
                
                //����ͷ��
                Map<String, String> headmsg = new HashMap<String, String>(0);
                if (null != ppt.getHeadMsg() && !ppt.getHeadMsg().equals("") && ppt.getHeadMsg().indexOf("=") > 0) {
                    String headmsgtemp = ppt.getHeadMsg().replace("\\;", "!!!fhzh");
                    String[] temp = headmsgtemp.split(";", -1);
                    for (int i = 0; i < temp.length; i++) {
                        if (null != temp[i] && !temp[i].equals("") && temp[i].indexOf("=") > 0) {
                            String key = temp[i].substring(0, temp[i].indexOf("="));
                            String value = temp[i].substring(temp[i].indexOf("=") + 1);
                            value = value.replace("!!!fhzh",";");
                            headmsg.put(key, value);
                        }
                    }
                }

                //������������
                if (null != getParameterValues) {
                    String booleanheadmsg = "headmsg(";
                    String msgend = ")";
                    for (Object obp : getParameterValues) {
                        String paramob = obp.toString();
                        if(paramob.contains("#")){
                            String key = paramob.substring(0, paramob.indexOf("#"));
                            String value = paramob.substring(paramob.indexOf("#") + 1);
                            if (key.contains(booleanheadmsg) && key.contains(msgend)) {
                                String head = key.substring(key.indexOf(booleanheadmsg) + 8, key.lastIndexOf(msgend));
                                headmsg.put(head, value);
                                continue;
                            }
                            int replaceflag=0;
                            for (int i = 0; i < paramslist.size(); i++) {
                                ProjectTemplateParams ptp = paramslist.get(i);
                                if("_forTextJson".equals(ptp.getParamName())){
                                	if(ptp.getParamValue().indexOf("\""+key+"\":")>=0){
                                 		//���������滻���
                                		int index = 1;
                                		if (key.indexOf("[") >= 0 && key.endsWith("]")) {
                                			index = Integer.valueOf(key.substring(key.lastIndexOf("[") + 1, key.lastIndexOf("]")));
                                			key = key.substring(0, key.lastIndexOf("["));
                                			LogUtil.APP.info("׼���滻JSON�����еĲ���ֵ��δ��⵽ָ����������ţ�Ĭ���滻��1������...");
                                		} else {
                                			LogUtil.APP.info("׼���滻JSON�����еĲ���ֵ���滻ָ���ڡ�{}��������...",index);
                                		}
                                		
                                		Map<String,String> map=ChangString.changjson(ptp.getParamValue(), key, value,index);
                                		if("true".equals(map.get("boolean"))){
                                            ptp.setParamValue(map.get("json"));
                                            paramslist.set(i, ptp);
                                            replaceflag=1;
                                            LogUtil.APP.info("�滻������{}�����...",key);
                                            break;
                                		}
                                	}else if(ptp.getParamValue().indexOf(key)>=0){
                                		ptp.setParamValue(ptp.getParamValue().replace(key, value));
                                		paramslist.set(i, ptp);
                                        replaceflag=1;
                                        LogUtil.APP.info("��鵱ǰ�ı�������JSON,���ַ�����{}����ֱ�Ӱѡ�{}���滻�ɡ�{}��...",ptp.getParamValue(),key,value);
                                        break;
                                	}else{
                                		LogUtil.APP.warn("�������Ĵ��ı�ģ���Ƿ���������JSON��ʽ�����ı����Ƿ�������滻�Ĺؼ��֡�");
                                	}
                                }else{
                                    if (ptp.getParamName().equals(key)) {
                                        ptp.setParamValue(value);
                                        paramslist.set(i, ptp);
                                        replaceflag=1;
                                        LogUtil.APP.info("��ģ���в�����{}����ֵ���óɡ�{}��",key,value);
                                        break;
                                    }
                                }
                            }
                            if(replaceflag==0){
                            	LogUtil.APP.warn("���������{}��û����ģ�����ҵ����滻�Ĳ�����ӦĬ��ֵ��"
                            			+ "�����������ʧ�ܣ�����Э��ģ���д˲����Ƿ���ڡ�",key);
                            }
                        }else{
                        	LogUtil.APP.warn("�滻ģ�����ͷ�����ʧ�ܣ�ԭ������Ϊû�м�⵽#��"
                        			+ "ע��HTTP�����滻������ʽ�ǡ�headmsg(ͷ����#ͷ��ֵ)|������#����ֵ|������2#����ֵ2��");
                        }

                    }
                }
                //��������
                Map<String, Object> params = new HashMap<String, Object>(0);
                for (ProjectTemplateParams ptp : paramslist) {
                	String tempparam = "";
                	if(null!=ptp.getParamValue()){
                		tempparam =  ptp.getParamValue().replace("&quot;", "\"");
                	}
                    //������������
                    if (ptp.getParamType() == 1) {
                        JSONObject json = JSONObject.parseObject(tempparam);
                        params.put(ptp.getParamName().replace("&quot;", "\""), json);
                        LogUtil.APP.info("ģ�������{}��  JSONObject���Ͳ���ֵ:��{}��",ptp.getParamName(),json.toString());
                    } else if (ptp.getParamType() == 2) {
                        JSONArray jarr = JSONArray.parseArray(tempparam);
                        params.put(ptp.getParamName().replace("&quot;", "\""), jarr);
                        LogUtil.APP.info("ģ�������{}��  JSONArray���Ͳ���ֵ:��{}��",ptp.getParamName(),jarr.toString());
                    } else if (ptp.getParamType() == 3) {
                        File file = new File(tempparam);
                        params.put(ptp.getParamName().replace("&quot;", "\""), file);
                        LogUtil.APP.info("ģ�������{}��  File���Ͳ���ֵ:��{}��",ptp.getParamName(),file.getAbsolutePath());
                    } else if (ptp.getParamType() == 4) {
                        Double dp = Double.valueOf(tempparam);
                        params.put(ptp.getParamName().replace("&quot;", "\""), dp);
                        LogUtil.APP.info("ģ�������{}��  �������Ͳ���ֵ:��{}��",ptp.getParamName(),tempparam);
                    } else if (ptp.getParamType() == 5) {
                        Boolean bn = Boolean.valueOf(tempparam);
                        params.put(ptp.getParamName().replace("&quot;", "\""), bn);
                        LogUtil.APP.info("ģ�������{}��  Boolean���Ͳ���ֵ:��{}��",ptp.getParamName(),bn);
                    } else {
                        params.put(ptp.getParamName().replace("&quot;", "\""), ptp.getParamValue().replace("&quot;", "\""));
                        LogUtil.APP.info("ģ�������{}��  String���Ͳ���ֵ:��{}��",ptp.getParamName(),ptp.getParamValue().replace("&quot;", "\""));
                    }
                }


                if (functionname.toLowerCase().equals("socketpost")) {
                    result = HttpClientTools.sendSocketPost(packagename, params, ppt.getEncoding().toLowerCase(), headmsg);
                } else if (functionname.toLowerCase().equals("socketget")) {
                    result = HttpClientTools.sendSocketGet(packagename, params, ppt.getEncoding().toLowerCase(), headmsg);
                } else {
                    LogUtil.APP.warn("����SOCKET���������쳣����⵽�Ĳ���������:{}",functionname);
                    result = "�����쳣����鿴������־��";
                }
            }
        } catch (Throwable e) {
            LogUtil.APP.error("�����쳣����鿴������־��", e);
            return "�����쳣����鿴������־��";
        }
        return result;
    }

    public static Method getMethod(Method[] methods, String methodName, @SuppressWarnings("rawtypes") Class[] parameterTypes) {
        for (int i = 0; i < methods.length; i++) {
            if (!methods[i].getName().equals(methodName)) {
                continue;
            }
            if (compareParameterTypes(parameterTypes, methods[i].getParameterTypes())) {
                return methods[i];
            }

        }
        return null;
    }

    public static boolean compareParameterTypes(@SuppressWarnings("rawtypes") Class[] parameterTypes, @SuppressWarnings("rawtypes") Class[] orgParameterTypes) {
        // parameterTypes ���棬int->Integer
        // orgParameterTypes��ԭʼ��������
        if (parameterTypes == null && orgParameterTypes == null) {
            return true;
        }
        if (parameterTypes == null && orgParameterTypes != null) {
            if (orgParameterTypes.length == 0) {
                return true;
            } else {
                return false;
            }
        }
        if (parameterTypes != null && orgParameterTypes == null) {
            if (parameterTypes.length == 0) {
                return true;
            } else {
                return false;
            }

        }
        if (parameterTypes.length != orgParameterTypes.length) {
            return false;
        }

        return true;
    }
    
    /**
     * ����Python�ű�
     * @param packagename
     * @param functionname
     * @param getParameterValues
     * @return
     */
    private static String callPy(String packagename, String functionname, Object[] getParameterValues){
    	String result = "����Python�ű������쳣�����ؽ����null";
    	try {
    		// ���建��������������������������Ϣ�����
    		byte[] buffer = new byte[1024];
    		ByteArrayOutputStream outStream = new ByteArrayOutputStream();  
    		ByteArrayOutputStream outerrStream = new ByteArrayOutputStream();
    		int params=0;
    		if(getParameterValues!=null){
    			params=getParameterValues.length;
    		}
    		String[] args = new String[2+params];
    		args[0]="python";
            if(packagename.endsWith(File.separator)){
            	args[1]=packagename+functionname;
            	//args[1]="E:\\PycharmProjects\\untitled\\venv\\testaaa.py";
            }else{
            	args[1]=packagename+File.separator+functionname;
            	//args[1]="E:\\PycharmProjects\\untitled\\venv\\testaaa.py";
            }
            LogUtil.APP.info("����Python�ű�·��:{}",args[1]);
    		for(int i=0;i < params;i++){
    			args[2+i]=getParameterValues[i].toString();
    		}

            Process proc=Runtime.getRuntime().exec(args);
            InputStream errStream = proc.getErrorStream();
            InputStream stream = proc.getInputStream();
            
            // ����ȡ��д��
            int len = -1;  
            while ((len = errStream.read(buffer)) != -1) {  
                outerrStream.write(buffer, 0, len);  
            }  
            while ((len = stream.read(buffer)) != -1) {  
                outStream.write(buffer, 0, len);  
            }
            
            proc.waitFor();
            // ��ӡ����Ϣ
            if(outerrStream.toString().equals("")){
            	result = outStream.toString().trim();
            	LogUtil.APP.info("�ɹ�����Python�ű������ؽ��:{}",result);
            }else{
            	result = outerrStream.toString().trim();
            	if(result.indexOf("ModuleNotFoundError")>-1){
            		LogUtil.APP.warn("����Python�ű������쳣�������Pythonģ��δ���õ�������Python�ű���ע������ϵͳ����·��(��: sys.path.append(\"E:\\PycharmProjects\\untitled\\venv\\Lib\\site-packages\"))��"
            				+ "��ϸ������Ϣ:{}",result);
            	}else if(result.indexOf("No such file or directory")>-1){
            		LogUtil.APP.warn("����Python�ű������쳣����ָ��·����δ�ҵ�Python�ű���ԭ���п�����Python�ű�·��������Ǵ���Pythonָ������������һ�£���ϸ������Ϣ:{}",result);
            	}else{
            		LogUtil.APP.warn("����Python�ű������쳣��������Ϣ:{}",result);
            	}        	
            }
           } 
         catch (Exception e) {
        	 LogUtil.APP.error("����Python�ű������쳣,���飡",e);
            return result;
        }
    	return result;
    }

}