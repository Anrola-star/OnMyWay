package com.anrola.onmyway.Utils;

import static com.anrola.onmyway.Value.GlobalConstants.REQUEST_TIMEOUT;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.anrola.onmyway.R;

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
    private Context context;
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
            connection.setConnectTimeout(REQUEST_TIMEOUT);
            connection.setReadTimeout(REQUEST_TIMEOUT);

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
    public HttpURLConnection getConnection(String urlString, Method method, Boolean isUseJson, String token) {
        HttpURLConnection connection = null;


        URL url = null;
        try {
            url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();

            // 设置请求参数
            connection.setRequestMethod(method.toString());
            connection.setRequestProperty("Authorization", "Bearer " + token);
            connection.setConnectTimeout(REQUEST_TIMEOUT);
            connection.setReadTimeout(REQUEST_TIMEOUT);
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

    public String getResponse(HttpURLConnection connection) {
        // 初始化响应体容器
        StringBuilder response = new StringBuilder();

        try {
            // 获取响应码（先判断connection是否为空，避免空指针）
            if (connection == null) {
                Log.e(TAG, "getResponse: connection is null");
                return null;
            }

            int responseCode = connection.getResponseCode();
            Log.d(TAG, "getResponse: 响应码 = " + responseCode);

            inputStream = (responseCode == HttpURLConnection.HTTP_OK)
                    ? connection.getInputStream()       // 成功响应流
                    : connection.getErrorStream();      // 错误响应流

            // 读取响应体（流不为空时）
            if (inputStream != null) {
                reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)); // 指定编码，避免乱码
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                Log.d(TAG, "getResponse: 响应体 = " + response);
            } else {
                Log.e(TAG, "getResponse: 响应流为空，响应码 = " + responseCode);
                return null;
            }

            // 返回响应体（无论状态码是否200，都返回内容供上层判断）
            return response.toString();

        } catch (Exception e) {
            // 捕获所有异常，打印详细日志
            Log.e(TAG, "getResponse: 读取响应异常", e);
            return null;

        } finally {
            // 关闭所有资源，避免泄漏（无论是否异常）
            try {
                if (reader != null) {
                    reader.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (Exception e) {
                Log.e(TAG, "getResponse: 关闭资源异常", e);
            }
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
            //closeConnection(connection);
            //closeStream();
            //Log.d(TAG, "get: "+ response);
        }).start();
    }
    public void get(String urlString,Handler handler, int what, String token) {
        new Thread(() -> {
            // 获取连接
            HttpURLConnection connection = getConnection(urlString, Method.GET, false,token);
            String response = getResponse(connection);

            // 发送消息给主线程
            Message message = new Message();
            message.what = what;
            message.obj = response;
            handler.sendMessage(message);

            // 关闭连接和流
            //closeConnection(connection);
            //closeStream();
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
            //closeConnection(connection);
            //closeStream();
            //Log.d(TAG, "post: "+ response);
        }).start();
    }
    public void post(String urlString, JSONObject json, Handler handler, int what, String token) {
        new Thread(() -> {
            // 获取连接
            HttpURLConnection connection = getConnection(urlString, Method.POST, true);
            connection.setRequestProperty("Authorization", "Bearer " + token);  // 添加token
            doOutputStream(connection, json);
            String response = getResponse(connection);

            // 创建消息对象
            Message message = new Message();
            message.what = what;
            message.obj = response;

            // 发送消息给主线程
            handler.sendMessage(message);

            // 关闭连接和流
            //closeConnection(connection);
            //closeStream();
            //Log.d(TAG, "post: "+ response);
        }).start();
    }
    public InputStream getInputStream() {
        return inputStream;
    }
    public BufferedReader getReader() {
        return reader;
    }

    public String getBaseURL(Context context) {
        return String.format("http://%s:%s",context.getString(R.string.request_ip), context.getString(R.string.request_port));
    }
}
