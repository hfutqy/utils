```
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.bson.Document;

import com.alibaba.fastjson.JSONObject;
import com.github.walker.mybatis.paginator.PageList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;


public class MongoManager {
	
	 
	    private static MongoClient  client = null;  
	  
	    private MongoManager() 
	    { 
	    	
	    }  
	  
	    static {  
	        initDBPrompties();  
	    }  
	  	  
	    /** 
	     * 初始化连接池 
	     */  
	    private static void initDBPrompties() {  	    	    		
	    	 String url=PropertyHolder.getProperty("mongodb.url");
	    	 String dbName=PropertyHolder.getProperty("mongodb.dbName"); 
	    	 String userName=PropertyHolder.getProperty("mongodb.userName");
	    	 String password=PropertyHolder.getProperty("mongodb.password");
	    	 int connectionsPerHost = Integer.valueOf(PropertyHolder.getProperty("mongodb.connectionsPerHost"));
	    	 int threads = Integer.valueOf(PropertyHolder.getProperty("mongodb.threads"));
	    	 int maxWaitTime=Integer.valueOf(PropertyHolder.getProperty("mongodb.maxWaitTime"));
	    	 int socketTimeout=Integer.valueOf(PropertyHolder.getProperty("mongodb.socketTimeout"));
	    	 int maxConnectionLifeTime=Integer.valueOf(PropertyHolder.getProperty("mongodb.maxConnectionLifeTime"));
	    	 int connectTimeout = Integer.valueOf(PropertyHolder.getProperty("mongodb.connectTimeout"));
	    	 
	    	 List<MongoCredential> credentials = new ArrayList<MongoCredential>();
	    	 ServerAddress address = new ServerAddress(url);
	    	 MongoCredential credential = MongoCredential.createCredential(userName,dbName,password.toCharArray());
			 credentials.add(credential);
	    	 
	    	 MongoClientOptions.Builder build = new MongoClientOptions.Builder();  	         
	         build.connectionsPerHost(connectionsPerHost);
	         build.maxWaitTime(maxWaitTime);
	         build.maxConnectionLifeTime(maxConnectionLifeTime);
	         build.connectTimeout(connectTimeout);
	         build.threadsAllowedToBlockForConnectionMultiplier(threads);
	         build.socketTimeout(socketTimeout);
	         MongoClientOptions options = build.build();  
	         client = new MongoClient(address, credentials, options);  
	    }  
	    
	    /**
	     * 获取数据库
	     * @param dbName 数据库
	     * @return
	     */
		public static MongoDatabase getDB(String dbName) {  
	        return client.getDatabase(dbName);
	    }
	    
	    /**
	     * 获取表
	     * @param dbName 数据库
	     * @param collectionName 集合
	     * @return
	     */
	    public static MongoCollection<Document>  getCollection(String dbName,String collectionName)
	    {
	    	 MongoCollection<Document>  collection = getDB(dbName).getCollection(collectionName);
	    	 return collection;
	    }
	    
	    /**
	     * 插入表数据
	     * @param dbName 数据库
	     * @param collectionName 集合
	     * @param json 待入库json
	     */
	    public static void insert(String dbName,String collectionName,String json)
	    {
	    	MongoCollection<Document> collection = getCollection(dbName, collectionName);
	        Document document = Document.parse(json);
	    	collection.insertOne(document);
	    }
	    
	    /**
	     * 分页查询用户操作日志
	     * @param dbName 数据库
	     * @param collectionName 集合
	     * @param acctNo 账户号
	     * @param start 
	     * @param pageSize
	     * @return
	     */
	    public static PageList<UserOpLog> findUserOpLog(String dbName,String collectionName,String acctNo,String tenantId,String keyWord,String startDate,String endDate,int start,int pageSize)
	    {   
	    	List<UserOpLog> logList = new ArrayList<UserOpLog>();
	    	MongoCollection<Document> collection = getCollection(dbName, collectionName);
	        BasicDBObject queryObject = new BasicDBObject();
	        BasicDBObject tmpObject = new BasicDBObject();
	        BasicDBObject dateObject = new BasicDBObject();	       
	        if(StringUtils.isNotEmpty(acctNo))
	        {   
	        	
	        	queryObject.put("acctNo", acctNo);		        
	        }
	        if(tenantId!=null)
	        {
	        	queryObject.put("tenantId", tenantId);		        
	        }
	        if(StringUtils.isNotEmpty(keyWord))
	        {   
	        	Pattern pattern = Pattern.compile("^.*"+keyWord+".*$", Pattern.CASE_INSENSITIVE);
	        	queryObject.put("opDesc", pattern);		        
	        }
	        
	        tmpObject.put("$gte", startDate); //大于
	        dateObject = tmpObject.append("$lte", endDate);	//小于      	               	        
	        queryObject.put("opTime", dateObject);	      
	        FindIterable<Document> iterator= collection.find(queryObject).sort((new BasicDBObject("opTime",-1)));		       
	        int count = 0;
	        MongoCursor<Document> cursor= iterator.iterator();
	        while(cursor.hasNext()) { 
	        	Document doc = cursor.next();
	        	if(count>=start && count<=pageSize+start-1)
	        	{	        		
	                UserOpLog userOpLog = new UserOpLog();
	                userOpLog.setAcctNo(doc.getString("acctNo"));
	                userOpLog.setClasz(doc.getString("clasz"));
	                userOpLog.setErrorMsg(doc.getString("errorMsg"));
	                userOpLog.setMethod(doc.getString("method"));
	                userOpLog.setName(doc.getString("name"));
	                userOpLog.setOpDesc(doc.getString("opDesc"));
	                userOpLog.setOpResult(doc.getInteger("opResult"));
	                userOpLog.setOpTime(doc.getString("opTime"));
	                userOpLog.setUri(doc.getString("uri"));
	                userOpLog.setTenantId(doc.getString("tenantId"));
	                logList.add(userOpLog);
	        	}
                count++;
            }  
	        cursor.close();
	        PageList<UserOpLog> pageList = new PageList<UserOpLog>(logList,count);
	        return pageList;	        
	    }
	    
	    /**
	     * 分页查询接口调用日志
	     * @param dbName 数据库
	     * @param collectionName 集合
	     * @param tenantId 商户ID
	     * @param appId 应用ID
	     * @param startDate 开始日期
	     * @param endDate 结束日期
	     * @param start
	     * @param pageSize
	     * @return
	     */
	    public static PageList<UserCallLog> findUserCallLog(String dbName,String collectionName,String tenantId,String appId,String startDate,String endDate,int start,int pageSize)
	    {   
	    	List<UserCallLog> logList = new ArrayList<UserCallLog>();
	    	MongoCollection<Document> collection = getCollection(dbName, collectionName);
	        BasicDBObject queryObject = new BasicDBObject();
	        BasicDBObject tmpObject = new BasicDBObject();
	        BasicDBObject dateObject = new BasicDBObject();	       
	        if(StringUtils.isNotEmpty(tenantId))
	        {
	        	queryObject.put("tenantId", tenantId);	
	        	
	        }
	        if(StringUtils.isNotEmpty(appId))
	        {
	        	queryObject.put("appId", appId);		        
	        }	       
	        
	        tmpObject.put("$gte", startDate); //大于
	        dateObject = tmpObject.append("$lte", endDate);	//小于      	               	        
	        queryObject.put("reqTime", dateObject);	      
	        FindIterable<Document> iterator= collection.find(queryObject) ;		       
	        int count = 0;
	        MongoCursor<Document> cursor= iterator.iterator();
	        while(cursor.hasNext()) { 
	        	Document doc = cursor.next();
	        	if(count>=start && count<=pageSize+start-1)
	        	{	        		
	        		UserCallLog userCallLog = new UserCallLog();
	        		userCallLog.setAppId(doc.getString("appId"));
	                userCallLog.setClientHost(doc.getString("clientHost"));
	                userCallLog.setClientIp(doc.getString("clientIp"));
	                userCallLog.setClientPort(doc.getInteger("clientPort"));
	                userCallLog.setErrorCode(doc.getString("errorCode"));
	                userCallLog.setErrorMsg(doc.getString("errorMsg"));
	                userCallLog.setFlowNo(doc.getString("flowNo"));
	                userCallLog.setInterfaceClasz(doc.getString("interfaceClasz"));
	                userCallLog.setInterfaceId(doc.getString("interfaceId"));
	                userCallLog.setMethodId(doc.getString("methodId"));
	                userCallLog.setMethodName(doc.getString("methodName"));
	                userCallLog.setReqBytes(doc.getInteger("reqBytes"));
	                userCallLog.setReqTime(doc.getString("reqTime"));
	                userCallLog.setResBytes(doc.getInteger("resBytes"));
	                userCallLog.setResTime(doc.getString("resTime"));
	                userCallLog.setSvcId(doc.getString("svcId"));
	                userCallLog.setSvcInterface(doc.getString("svcInterface"));
	                userCallLog.setTenantId(doc.getString("tenantId"));
	                userCallLog.setToken(doc.getString("token"));
	                userCallLog.setUri(doc.getString("uri"));
	                logList.add(userCallLog);
	        	}
                count++;
            }  
	        cursor.close();
	        PageList<UserCallLog> pageList = new PageList<UserCallLog>(logList,count);
	        return pageList;	        
	    }
	    
	   
}
```
