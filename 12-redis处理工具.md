```
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class QCloudJedis {

    private static final Logger log = LoggerFactory.getLogger(QCloudJedis.class);
    
    private static final Properties properties = ClassesConfigLoader.getProperties();
    private static final String host = properties.getProperty("redis.host");
    private static final int port = Integer.valueOf(properties.getProperty("redis.port"));
    private static final String instanceid = properties.getProperty("redis.instanceid");
    private static final String password = properties.getProperty("redis.password");
    private static final int timeout = Integer.valueOf(properties.getProperty("redis.timeout", "2000"));
    private static final int maxTotal = Integer.valueOf(properties.getProperty("redis.maxTotal", "1024"));
    private static final int maxIdle = Integer.valueOf(properties.getProperty("redis.maxIdle", "10"));
    private static final int maxWaitMillis = Integer.valueOf(properties.getProperty("redis.maxWaitMillis", "3000"));
    private static final boolean testOnIdle = Boolean.valueOf(properties.getProperty("redis.testOnIdle", "true"));  //是否checkIdle
    private static final int timeCheckIdle = Integer.valueOf(properties.getProperty("redis.timeCheckIdle", "60000"));   //每隔多少秒check一次
    private static final int idleTimeout = Integer.valueOf(properties.getProperty("redis.idleTimeout", "300000"));     //超时时间
    private static final int numTestsPerEvictionRun = Integer.valueOf(properties.getProperty("redis.numTestsPerEvictionRun", "1024")); //一次驱逐过程中，最多驱逐对象的个数
    
    private static JedisPool pool = null;
    //private static Jedis jedis = null;
    private static Object lock = new Object();
    
    
    static {
    	init();
    }
    
    private static void init() {
    	if (null == pool) {
    		GenericObjectPoolConfig config = new GenericObjectPoolConfig();
            config.setMaxTotal(maxTotal);
			config.setMaxIdle(maxIdle);
			config.setMaxWaitMillis(maxWaitMillis);

			config.setTestWhileIdle(testOnIdle);
			config.setTimeBetweenEvictionRunsMillis(timeCheckIdle);
			config.setMinEvictableIdleTimeMillis(idleTimeout);
			config.setNumTestsPerEvictionRun(numTestsPerEvictionRun);
			synchronized (lock) {
				if (null == pool) {
					try {			            
			            
			            pool = new JedisPool(config, host, port, timeout, instanceid + ":" + password);
			            log.info("init jedis pool successful!");
			        } catch (Exception e) {
			            log.error("", e);
			        }
				}
			}
		}
    	   	
    }

//    public static Jedis getInstance() {
//    	init();
//        
//        return jedis;
//    }
    
    /**
	 * 获取一个jedis 对象
	 * 
	 * @return
	 */
	private static Jedis getJedis() {
		if (pool == null) {
			init();
		}
		return pool.getResource();
	}

	/**
	 * 返还到连接池
	 * 
	 * @param pool
	 * @param redis
	 */
	private static void returnResource(Jedis redis) {
		if (redis != null) {
			redis.close();
		}
	}
	
	// 删除key
	public static Long del(String key) {
		Jedis jedis = null;
		try {
			jedis = getJedis();
			return jedis.del(key);
		} catch (Exception e) {
			log.error("操作Redis发生异常，异常详情：", e);
			return -1L;
		} finally {
			// 返还到连接池
			returnResource(jedis);
		}
		
	}
	
	public static Boolean exists(String key) {
		Jedis jedis = null;
		Boolean flag = false;
		try {
			jedis = getJedis();
			flag = jedis.exists(key);
		} catch (Exception e) {
			log.error("操作Redis发生异常，异常详情：", e);
		} finally {
			// 返还到连接池
			returnResource(jedis);
		}

		return flag;
	}

	/**
	 * 获取数据
	 * 
	 * @param key
	 * @return
	 */
	public static String get(String key) {
		String value = null;
		Jedis jedis = null;
		try {
			jedis = getJedis();
			value = jedis.get(key);
		} catch (Exception e) {
			log.error("操作Redis发生异常，异常详情：", e);
		} finally {
			// 返还到连接池
			returnResource(jedis);
		}
		return value;
	}

	// 设置
	public static String set(String key, String value) {
		Jedis jedis = null;
		try {
			jedis = getJedis();
			return jedis.set(key, value);
		} catch (Exception e) {
			log.error("操作Redis发生异常，异常详情：", e);
			return "";
		} finally {
			// 返还到连接池
			returnResource(jedis);
		}
	}
	
	// 获取Hash值
		public static String hget(String key, String field) {
			Jedis jedis = null;
			String value = null;
			try {
				jedis = getJedis();
				value = jedis.hget(key, field);
			} catch (Exception e) {
				log.error("操作Redis发生异常，异常详情：", e);
			} finally {
				// 返还到连接池
				returnResource(jedis);
			}
			return value;
		}

		public static Map<String, String> hgetAll(String key) {
			Jedis jedis = null;
			Map<String, String> value = null;
			try {
				jedis = getJedis();
				value = jedis.hgetAll(key);
			} catch (Exception e) {
				log.error("操作Redis发生异常，异常详情：", e);
			} finally {
				// 返还到连接池
				returnResource(jedis);
			}
			return value;
		}

		// 设置Hash值
		public static Long hset(String key, String field, String value) {
			Jedis jedis = null;
			try {
				jedis = getJedis();
				return jedis.hset(key, field, value);
			} catch (Exception e) {
				log.error("操作Redis发生异常，异常详情：", e);
				return -1L;
			} finally {
				// 返还到连接池
				returnResource(jedis);
			}
		}
		
		// 删除Hash值
		public static Long hdel(String key, String... fields) {
			Jedis jedis = null;
			try {
				jedis = getJedis();
				return jedis.hdel(key, fields);
			} catch (Exception e) {
				log.error("操作Redis发生异常，异常详情：", e);
				return -1L;
			} finally {
				// 返还到连接池
				returnResource(jedis);
			}
		}
		
		public static Long expire(String key, int seconds) {
			Jedis jedis = null;
			try {
				jedis = getJedis();
				return jedis.expire(key, seconds);
			} catch (Exception e) {
				log.error("操作Redis发生异常，异常详情：", e);
				return -1L;
			} finally {
				// 返还到连接池
				returnResource(jedis);
			}
		}

    public static void main(String[] args) {
        try {            
            
            while (true) {
            	String key = "ftc-ump-mid";
                //System.out.println("before setting: " + jedis.hget(key, "attr"));
                System.out.println(">>" + QCloudJedis.hset(key, "attr", "0"));
                QCloudJedis.expire(key, 5);
                         
                System.out.println("after setting: " + QCloudJedis.hget(key, "attr"));
                System.out.println(">>" + QCloudJedis.hdel(key, "attr"));
                
                System.out.println("after delete: " + QCloudJedis.hget(key, "attr"));
                
                Thread.sleep(1000 * 10);
            }
            

            //关闭退出
            //jedis.quit();
            //jedis.close();
            

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```
