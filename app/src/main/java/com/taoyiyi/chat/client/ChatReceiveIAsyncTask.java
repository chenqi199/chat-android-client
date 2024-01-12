package com.taoyiyi.chat.client;

import android.os.AsyncTask;

public class ChatReceiveIAsyncTask  extends AsyncTask<String, Integer, String>{

    private ChatReceive receive;


    public ChatReceiveIAsyncTask(ChatReceive receive) {
        this.receive = receive;
    }

    @Override
    protected String doInBackground(String... strings) {
        // todo 注册设备
        // todo 发送拉消息
        // todo 标记会话已读

        return null;
    }
    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        receive.onRec(result);
    }


}
