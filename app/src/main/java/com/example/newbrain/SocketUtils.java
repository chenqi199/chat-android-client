package com.example.newbrain;

import android.os.AsyncTask;
import android.util.Log;

import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * 作者：guoyzh
 * 时间：2019/5/15 11:02
 * 功能：处理socket的工具类
 */
public class SocketUtils {
    private Socket socket;
    private static final SocketUtils ourInstance = new SocketUtils();

    static SocketUtils getInstance() {
        return ourInstance;
    }

    private SocketUtils() {

    }

    /**
     * 向指定ip地址发送指令
     *
     * @param ipAddress
     * @param port
     * @param msg
     * @param listener
     */
    public void sendMsg(String ipAddress, int port, String msg, OnSocketResult listener) {
        MyAsyncTask asyncTask = new MyAsyncTask(listener);
        asyncTask.execute(ipAddress, String.valueOf(port), msg);
    }

    /**
     * 自定义asyncTask
     */
    public class MyAsyncTask extends AsyncTask<String, Integer, String> {

        private final OnSocketResult listener;

        public MyAsyncTask(OnSocketResult listener) {
            super();
            this.listener = listener;
        }

        @Override
        protected String doInBackground(String... strings) {

            String address = strings[0];
            int port = Integer.parseInt(strings[1]);
            String msg = strings[2];
            String data = "";
            try {
                 Log.w("tcp", "启动客户端");
                if (socket == null) {
                    socket = new Socket(address, port);
                }
                Log.w("tcp", "客户端连接成功");
                if (socket != null && socket.isConnected() && msg != null) {
                    socket.getOutputStream().write(msg.getBytes());
                    socket.getOutputStream().flush();

                    PrintWriter pw = new PrintWriter(socket.getOutputStream());
                    InputStream inputStream = socket.getInputStream();
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = inputStream.read(buffer)) != -1) {
                        data = new String(buffer, 0, len);
                        Log.w("tcp", "收到服务器的数据:" + data);
                        socket.close();
                    }
                    // Log.i("tcp", "客户端断开连接");
                    pw.close();
                }
            } catch (Exception EE) {
                EE.printStackTrace();
                // Log.i("tcp", "客户端无法连接服务器");
            } finally {
                try {
                    if (socket != null) {
                        socket.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                socket = null;
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            listener.onSuccess(result);
        }
    }

    /**
     * 获取socket结果的接口
     */
    public interface OnSocketResult {
        void onSuccess(String result);
    }
}
