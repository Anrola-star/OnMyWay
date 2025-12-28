package com.example.onmyway.Utils;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class MyRequest {
    private static final String TAG = "MYLOG_MyRequest";
    private InputStream inputStream = null;
    private BufferedReader reader = null;

    public enum Method {
        GET, POST, PUT, DELETE
    }

    /**
     * 获取连接
     * @param urlString 请求地址
     * @param method    请求方式
     * @param isUseJson  是否使用JSON
     * @return 连接
     */
    public HttpURLConnection getConnection(String urlString, Method method, Boolean isUseJson) {
        HttpURLConnection connection = null;


        URL url = null;
        try {
            url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();

            // 设置请求参数
            connection.setRequestMethod(method.toString());
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            if (isUseJson) {
                connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                connection.setDoOutput(true);
            } else {
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
                connection.setDoOutput(false);
            }
            return connection;
        } catch (Exception e) {
            Log.e(TAG, "getConnection: "+ e);
            return null;
        }
    }

    /**
     * 获取输出流
     * @param connection 连接
     * @param Json       JSON数据
     */
    public void doOutputStream(HttpURLConnection connection, JSONObject Json) {
        if (!connection.getDoOutput()){
            return;
        }

        try {
            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(Json.toString().getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
            Log.e(TAG, "doOutputStream: "+ e);
        }
    }

    /**
     * 获取响应
     * @param connection 链接
     * @return 响应
     */
    public String getResponse(HttpURLConnection connection)  {
        try {
            int responseCode = connection.getResponseCode();
            StringBuilder response = null;
            if (responseCode == HttpURLConnection.HTTP_OK) {
                inputStream = connection.getInputStream();
                reader = new BufferedReader(new InputStreamReader(inputStream));

                // 读取响应
                response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                Log.d(TAG, "getResponse: "+ response);
                return response.toString();
            }else {
                // 响应码错误
                Log.e(TAG, "getResponse: "+ responseCode);
                return null;
            }
        } catch (Exception e) {
            // 响应错误
            Log.e(TAG, "getResponse: "+ e);
            return null;
        }

    }

    public void closeConnection(HttpURLConnection connection) {
            connection.disconnect();
    }

    public void closeStream() {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void get(String urlString,Handler handler, int what) {
        new Thread(() -> {
            // 获取连接
            HttpURLConnection connection = getConnection(urlString, Method.GET, false);
            String response = getResponse(connection);

            // 发送消息给主线程
            Message message = new Message();
            message.what = what;
            message.obj = response;
            handler.sendMessage(message);

            // 关闭连接和流
            closeConnection(connection);
            closeStream();
            //Log.d(TAG, "get: "+ response);
        }).start();
    }

    public void post(String urlString, JSONObject json, Handler handler, int what) {
        new Thread(() -> {
            // 获取连接
            HttpURLConnection connection = getConnection(urlString, Method.POST, true);
            doOutputStream(connection, json);
            String response = getResponse(connection);

            // 创建消息对象
            Message message = new Message();
            message.what = what;
            message.obj = response;

            // 发送消息给主线程
            handler.sendMessage(message);

            // 关闭连接和流
            closeConnection(connection);
            closeStream();
            //Log.d(TAG, "post: "+ response);
        }).start();
    }

    public InputStream getInputStream() {
        return inputStream;
    }
    public BufferedReader getReader() {
        return reader;
    }
}
