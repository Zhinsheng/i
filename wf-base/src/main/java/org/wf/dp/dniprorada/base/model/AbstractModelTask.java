package org.wf.dp.dniprorada.base.model;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import net.sf.jmimemagic.Magic;
import net.sf.jmimemagic.MagicException;
import net.sf.jmimemagic.MagicMatchNotFoundException;
import net.sf.jmimemagic.MagicParseException;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.Expression;
import org.activiti.engine.form.FormData;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.form.StartFormData;
import org.activiti.engine.task.Attachment;
import org.activiti.redis.model.ByteArrayMultipartFile;
import org.activiti.redis.service.RedisService;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.wf.dp.dniprorada.base.dao.BaseEntityDao;
import org.wf.dp.dniprorada.base.dao.FlowSlotDao;
import org.wf.dp.dniprorada.base.dao.FlowSlotTicketDao;
import org.wf.dp.dniprorada.base.util.JsonRestUtils;
import org.wf.dp.dniprorada.base.viewobject.flow.SaveFlowSlotTicketResponse;
import org.wf.dp.dniprorada.form.FormFileType;
import org.wf.dp.dniprorada.form.QueueDataFormType;
import org.wf.dp.dniprorada.model.MimiTypeModel;
import org.activiti.rest.controller.*;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

public abstract class AbstractModelTask {
	
	static final transient Logger LOG = LoggerFactory
			.getLogger(AbstractModelTask.class);
	
	public static final String LIST_KEY_PREFIX = "lst";
	public static final String LIST_KEY_DELIM = ":";

	@Autowired
	RedisService redisService;

        
   @Autowired
   private FlowSlotDao flowSlotDao;

   @Autowired
   private FlowSlotTicketDao oFlowSlotTicketDao;

   @Autowired
   private BaseEntityDao baseEntityDao;
        
        
	/**
	 * Получить список по ключу списка из execution
	 * @param listKey
	 * @param execution
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> List<T> getListVariable(String listKey, DelegateExecution execution) {
		List<T> result = new ArrayList<T>();
		
		String keyPrefix = LIST_KEY_PREFIX+LIST_KEY_DELIM+listKey;
		
		for (String execVarKey : execution.getVariableNames()) {
			if (execVarKey.startsWith(keyPrefix)) {
				result.add((T)execution.getVariable(execVarKey));
			}
		}
		return result;
	}
	
	/**
	 * Сохранить список обьектов в execution
	 * @param listKey
	 * @param list
	 * @param execution
	 */
	public <T extends ListKeyable> void setListVariable(String listKey, List<T> list, DelegateExecution execution) {
		for (ListKeyable listKeyable : list) {
			execution.setVariable(
					getExecutionVarKey(listKey, listKeyable.getKey()),
					listKeyable);
		}
	}
	
	
	/**
	 * Возвращает сложгый ключ переменной бизнес-процесса 
	 * @param listKey
	 * @param elementKey
	 * @return
	 */
	private static String getExecutionVarKey(String listKey, String elementKey) {
		return new StringBuilder().append(LIST_KEY_PREFIX)
			   .append(LIST_KEY_DELIM)
			   .append(listKey)
			   .append(LIST_KEY_DELIM)
			   .append(elementKey).toString();
	}
	
	
	/**
	 * Получение ContentType файла
	 * @param dataFile
	 * @return
	 */
	public static MimiTypeModel getMimiType(byte[] dataFile) {
		MimiTypeModel mimiTypeModel = new MimiTypeModel();
		try {
			String exe = Magic.getMagicMatch(dataFile).getExtension();
			if (exe != null && !exe.isEmpty()) {
				mimiTypeModel.setExtension(exe);
			} else {
				mimiTypeModel.setExtension("application/octet-stream");
			}
			String mimeType = Magic.getMagicMatch(dataFile).getMimeType();
			if (mimeType != null && !mimeType.isEmpty()) {
				mimiTypeModel.setMimiType(mimeType);
			} else {
				mimiTypeModel.setMimiType("application/octet-stream");
			}

		} catch (MagicParseException e) {
			LOG.warn("MagicParseException", e);
		} catch (MagicMatchNotFoundException e) {
			LOG.warn("MagicMatchNotFoundException", e);
		} catch (MagicException e) {
			LOG.warn("MagicException", e);
		}
		return mimiTypeModel;
	}
	
	/**
	 * Конверт Byte To String 
	 * @param contentbyte
	 * @return
	 */
	public static String contentByteToString(byte[] contentbyte) {
		BASE64Encoder encoder = new BASE64Encoder();
		 String byteToStringContent = encoder.encode(contentbyte);
		return byteToStringContent;
	}
	
	/**
	 * Конверт String to Byte
	 * @param contentString
	 * @return
	 * @throws java.io.IOException
	 */
	public static byte[] contentStringToByte(String contentString) throws IOException {
		BASE64Decoder decoder = new BASE64Decoder();
		byte[] contentbyte = decoder.decodeBuffer(contentString);
		return contentbyte;
	}
	
	
	public static String getStringFromFieldExpression(Expression expression,
			DelegateExecution execution) {
		if (expression != null) {
			Object value = expression.getValue(execution);
			if (value != null) {
				return value.toString();
			}
		}
		return null;
	}

	/**
	 * Получить 
	 * @param keyRedis
	 * @return
	 */
	public static List<String> getListKeysRedis(String keyRedis) {
		List<String> listKeys = new ArrayList<String>();
		if(keyRedis!=null&&!keyRedis.isEmpty()){
		String[] keys = keyRedis.split(";");
		listKeys = Arrays.asList(keys);
		return listKeys;
		}
		return listKeys;
	}
	
	/**
	 * Получить значения полей с кастомным типом file
	 * @param execution
	 * @param filedTypeFile
	 * @return
	 */
	public static List<String> getValueFieldWithCastomTypeFile(DelegateExecution execution, List<String> filedTypeFile) {
		List<String> listValueKeys = new ArrayList<String>();
		if (!filedTypeFile.isEmpty()) {
			Map<String, Object> variables = execution.getEngineServices()
					.getRuntimeService()
					.getVariables(execution.getProcessInstanceId());
			for (String fieldId : filedTypeFile) {
				if (variables.containsKey(fieldId)) {				
					listValueKeys.add(String.valueOf(variables.get(fieldId)));
				}
			}
		}
		return listValueKeys;
	}

	/**
	 * Получить ид поля с кастомным типом file
	 * @param oFormData
	 * @return
	 */
	public static List<String> getListFieldCastomTypeFile(FormData oFormData) {
		List<String>asFieldID = new ArrayList<String>();
		List<FormProperty> aFormProperty = oFormData.getFormProperties();
		if(!aFormProperty.isEmpty()){
                    for (FormProperty oFormProperty : aFormProperty) {
                            if(oFormProperty.getType() instanceof FormFileType){
                                    asFieldID.add(oFormProperty.getId());
                            }
                    }
		}
		return asFieldID;
	}

	/**
	 * Получить ид поля с кастомным типом file
	 * @param oFormData
	 * @return
	 */
	public static List<String> getListField_QueueDataFormType(FormData oFormData) {
		List<String>asFieldID = new ArrayList<String>();
		List<FormProperty> aFormProperty = oFormData.getFormProperties();
		if(!aFormProperty.isEmpty()){
                    for (FormProperty oFormProperty : aFormProperty) {
                            if(oFormProperty.getType() instanceof QueueDataFormType){
                                    asFieldID.add(oFormProperty.getId());
                            }
                    }
		}
		return asFieldID;
	}
        
	/**
	 * Получить имя поля 
	 * @param startformData
	 * @return
	 */
	public static List<String> getListCastomFieldName(FormData startformData) {
		List<String>filedName = new ArrayList<String>();
		List<FormProperty> startformDataList = startformData.getFormProperties();
		if(!startformDataList.isEmpty()){
		for (FormProperty prop : startformDataList) {
			if(prop.getType() instanceof FormFileType){
				filedName.add(prop.getName());
			}
		}
		}
		return filedName;
	}
	
	/**
	 * multipartFile To ByteArray
	 * @param file
	 * @return
	 * @throws java.io.IOException
	 */
	public static ByteArrayOutputStream multipartFileToByteArray(MultipartFile file)
			throws IOException {
            
                String sFilename = new String(file.getOriginalFilename().getBytes(),"Cp1251");//UTF-8
                System.out.println("sFilename=" + sFilename);
                
                String sFilename1 = new String(file.getOriginalFilename().getBytes(Charset.forName("UTF-8")));//UTF-8
                System.out.println("sFilename1=" + sFilename1);
                String sFilename2 = new String(file.getOriginalFilename().getBytes(),"UTF-8");//UTF-8
                System.out.println("sFilename2=" + sFilename2);
                String sFilename3 = new String(file.getOriginalFilename().getBytes(Charset.forName("Cp1251")));//UTF-8
                System.out.println("sFilename3=" + sFilename3);
                String sFilename4 = new String(file.getOriginalFilename().getBytes());//UTF-8
                System.out.println("sFilename4=" + sFilename4);
                

                String sFilename0 = file.getOriginalFilename();//UTF-8
                System.out.println("sFilename0=" + sFilename0);
                
                //sFilename = Renamer.sRenamed(sFilename0);
                String sFilenameNew0 = Renamer.sRenamed(sFilename0);
                System.out.println("sFilenameNew0=" + sFilenameNew0);
                String sFilenameNew1 = Renamer.sRenamed(sFilename1);
                System.out.println("sFilenameNew1=" + sFilenameNew1);
                String sFilenameNew2 = Renamer.sRenamed(sFilename2);
                System.out.println("sFilenameNew2=" + sFilenameNew2);
                String sFilenameNew3 = Renamer.sRenamed(sFilename3);
                System.out.println("sFilenameNew3=" + sFilenameNew3);
                String sFilenameNew4 = Renamer.sRenamed(sFilename4);
                System.out.println("sFilenameNew4=" + sFilenameNew4);
                
                //sFilename=sFilenameNew;
                System.out.println("sFilename(new)=" + sFilename);
            
		ByteArrayMultipartFile byteArrayMultipartFile  
				= new ByteArrayMultipartFile(
						file.getBytes(), file.getName(), sFilename, file.getContentType());
		 ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		 ObjectOutputStream oos = new ObjectOutputStream(byteArrayOutputStream);
		 oos.writeObject(byteArrayMultipartFile);
		 oos.flush();
		 oos.close();
		return byteArrayOutputStream;
	}

	/**
	 * ByteArray To multipartFile
	 * @param byteFile
	 * @return
	 * @throws java.io.IOException
	 * @throws ClassNotFoundException
	 */
	public static ByteArrayMultipartFile getByteArrayMultipartFileFromRedis(
			byte[] byteFile) throws IOException, ClassNotFoundException {
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteFile);
		ObjectInputStream ois = new ObjectInputStream(byteArrayInputStream);
		  ByteArrayMultipartFile contentMultipartFile = (ByteArrayMultipartFile) ois.readObject();
		ois.close();
		return contentMultipartFile;
	}


    /**
     * Adds Attachemnts based on formData to task.
     * @param formData FormData from task where we search file fields.
     * @param oTask where we add Attachments.
     */

    public void addAttachmentsToTask(FormData formData, DelegateTask oTask) {
        DelegateExecution execution = oTask.getExecution();
        
        LOG.info("SCAN:file");
        List<String> asFieldID = getListFieldCastomTypeFile(formData);
        LOG.info("[addAttachmentsToTask]");
        LOG.info("asFieldID="+asFieldID.toString());
        List<String> asFieldValue = getValueFieldWithCastomTypeFile(execution, asFieldID);
        LOG.info("asFieldValue="+asFieldValue.toString());
        List<String> asFieldName = getListCastomFieldName(formData);
        LOG.info("asFieldName="+asFieldName.toString());
        if (!asFieldValue.isEmpty()) {
            int n = 0;
            for (String sKeyRedis : asFieldValue) {
                LOG.info("sKeyRedis=" + sKeyRedis);
                if (sKeyRedis != null && !sKeyRedis.isEmpty() && !"".equals(sKeyRedis.trim()) && !"null".equals(sKeyRedis.trim()) && sKeyRedis.length() > 15) {
                    byte[] aByteFile = getRedisService().getAttachments(sKeyRedis);
                    ByteArrayMultipartFile oByteArrayMultipartFile = null;
                    try {
                        oByteArrayMultipartFile = getByteArrayMultipartFileFromRedis(aByteFile);
                    } catch (ClassNotFoundException | IOException e1) {
                        throw new ActivitiException(e1.getMessage(), e1);
                    }
                    if (oByteArrayMultipartFile != null) {
                        InputStream oInputStream = null;
                        try {
                            oInputStream = oByteArrayMultipartFile.getInputStream();
                        } catch (Exception e) {
                            throw new ActivitiException(e.getMessage(), e);
                        }
                        String sFileName = null;
                        try {
                            sFileName = new String(oByteArrayMultipartFile
                                    .getOriginalFilename().getBytes(
                                            "ISO-8859-1"), "UTF-8");
                        } catch (java.io.UnsupportedEncodingException e) {
                            throw new ActivitiException(e.getMessage(), e);
                        }
                        LOG.info("sFileName=" + sFileName);
                        if (!asFieldName.isEmpty() && n < asFieldName.size()) {
                            //String sDescription = asFieldName.get((asFieldName.size() - 1) - n);
                            String sDescription = asFieldName.get(n);
                            LOG.info("sDescription=" + sDescription);
                            String sID_Field = asFieldID.get(n);
                            LOG.info("sID_Field=" + sID_Field);
                            Attachment oAttachment = execution
                                    .getEngineServices()
                                    .getTaskService()
                                    .createAttachment(
                                            oByteArrayMultipartFile.getContentType()
                                                    + ";"
                                                    + oByteArrayMultipartFile.getExp(),
                                            oTask.getId(),
                                            execution.getProcessInstanceId(),
                                            sFileName,
                                            sDescription, oInputStream);
                            if(oAttachment!=null){
                                String nID_Attachment = oAttachment.getId();
                                //LOG.info("nID_Attachment=" + nID_Attachment);
                                LOG.info("Try set variable(sID_Field) '" + sID_Field + "' with the value(nID_Attachment) '" + nID_Attachment + "', for new attachment...");
                                execution.getEngineServices().getRuntimeService().setVariable(execution.getProcessInstanceId(), sID_Field, nID_Attachment);
                                LOG.info("Finished setting new value for variable with attachment(sID_Field) '" + sID_Field + "'");
                            }else{
                                LOG.error("Can't add attachment to oTask.getId()=" + oTask.getId());
                            }
                        }else{
                            LOG.error("asFieldName has nothing! asFieldName=" + asFieldName);
                        }
                    }else{
                        LOG.error("oByteArrayMultipartFile==null! aByteFile="+aByteFile.toString());
                    }
                }else{
                    LOG.error("Invalid Redis Key!!! sKeyRedis=" + sKeyRedis);
                }
                n++;
            }
        }
        
        
        LOG.info("SCAN:queueData");
        asFieldID = getListField_QueueDataFormType(formData);
        LOG.info("asFieldID="+asFieldID.toString());
        asFieldValue = getValueFieldWithCastomTypeFile(execution, asFieldID);
        LOG.info("asFieldValue="+asFieldValue.toString());
        //asFieldName = getListCastomFieldName(formData);
        //LOG.info("asFieldName="+asFieldName.toString());
        if (!asFieldValue.isEmpty()) {
            String sValue = asFieldValue.get(0);
            LOG.info("sValue=" + sValue);
            long nID_FlowSlotTicket=0;
            int nAt=sValue.indexOf(":");
            int nTo=sValue.indexOf(",");
            String s=sValue.substring(nAt+1,nTo);
            LOG.info("s=" + s);
            try{
                nID_FlowSlotTicket = Long.valueOf(s);
                LOG.info("Ok!");
            }catch(Exception oException){
                LOG.error(oException.getMessage());
                nID_FlowSlotTicket=1;
            }
            LOG.info("nID_FlowSlotTicket=" + nID_FlowSlotTicket);
            
            try{
                
                /*
                FlowSlotTicket oFlowSlotTicket = baseEntityDao.getById(FlowSlotTicket.class, nID_FlowSlotTicket);
                if (oFlowSlotTicket == null) {
                    LOG.error("FlowSlotTicket with id=" + nID_FlowSlotTicket + " is not found!");
                }else{
                    long nID_FlowSlot=oFlowSlotTicket.getoFlowSlot().getId();
                    LOG.error("nID_FlowSlot="+nID_FlowSlot);
                    long nID_Subject = oFlowSlotTicket.getnID_Subject();
                    LOG.error("nID_Subject="+nID_Subject);
                    long nID_Task_Activiti = 1; //TODO set real ID!!!
                    oFlowSlotTicket.setnID_Task_Activiti(nID_Task_Activiti);
                    baseEntityDao.saveOrUpdate(oFlowSlotTicket);
                    LOG.info("JSON:" + JsonRestUtils.toJsonResponse(new SaveFlowSlotTicketResponse(oFlowSlotTicket.getId())));
                }
                */
            }catch(Exception oException){
                LOG.error(oException.getMessage());
            }
            
        }
        
       
    }

    public RedisService getRedisService() {
            return redisService;
    }
}
