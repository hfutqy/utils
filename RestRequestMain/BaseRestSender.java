package com.tuniu.ams.common.base;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.tuniu.operation.platform.base.rest.RestClient;
import com.tuniu.operation.platform.tsg.base.core.utils.JsonUtil;

/**
 * Copyright © 2015-2018 Tuniu Inc. All rights reserved.
 *
 * @author huyinmiao
 * @datatime 2016-1-9
 * @description 基础
 */
@Service
public class BaseRestSender extends BaseSender {
    private static final int DEFALUTCONNECTTIMEOUT= 5;

    private static final int DEFALUTSOCKETTIMEOUT = 30;

    private static final Logger lOGGER = LoggerFactory.getLogger(BaseRestSender.class);

    /**
     * Description: <br>
     *
     * @author huyinmiao<br>
     * @taskId <br>
     * @param data
     * @param param 第一个是url ，第二个是method
     * @return <br>
     */
    @Override
    public String send(Object data, String... param) {
        String service = param[0];
        String method = param[1];
        String jsonData = JsonUtil.toString(data);
        RestClient restClient = new RestClient(service, method, jsonData);
        restClient.setConnectTimeout(DEFALUTCONNECTTIMEOUT);
        restClient.setSocketTimeout(DEFALUTSOCKETTIMEOUT);
        String result = null;
        try {
            lOGGER.info("request-->{}?{}", service, jsonData);
            result = restClient.execute();
            lOGGER.info("{}?request::{}---response::{}", service, jsonData, result);
            return result;
        } catch (Exception e) {
            lOGGER.error("call rest " + service + " exception ", e);
            return null;
        }
    }

    /**
     * Description: 上传文件<br>
     *
     * @author huyinmiao<br>
     * @taskId <br>
     * @param fileByte 文件字节
     * @param type 返回类型
     * @param url url
     * @return <br>
     */
    public <T> T upload(byte[] fileByte, TypeReference<T> type, String url, String filename) {
        HttpPost post = new HttpPost(url);
        MultipartEntity entity = new MultipartEntity();
        entity.addPart(filename, new ByteArrayBody(fileByte, filename));
        post.setEntity(entity);
        HttpClient httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, DEFALUTCONNECTTIMEOUT * 1000);
        httpclient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, DEFALUTSOCKETTIMEOUT * 1000);
        try {
            lOGGER.info("request-->{}", url);
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            String result = httpclient.execute(post, responseHandler);
            lOGGER.info("{}---response:{}", url, result);
            try {
                return JSON.parseObject(result, type);
            } catch (Exception e) {
                lOGGER.error(" response parse to bean exception ", e);
                return null;
            }
        } catch (ClientProtocolException e) {
            lOGGER.error("upload file exception ", e);
        } catch (IOException e) {
            lOGGER.error("upload file exception ", e);
        } finally {
            httpclient.getConnectionManager().shutdown();
        }
        return null;
    }
}
