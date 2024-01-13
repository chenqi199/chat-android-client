package com.example.newbrain;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.google.protobuf.ByteString;
import com.taoyiyi.chat.client.ChatClient;
import com.taoyiyi.chat.proto.TalkToOther;
import com.taoyiyi.chat.proto.TransportMessageOuterClass;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;

public class ChatUiActivity extends AppCompatActivity {

    private final int VIEW_TYPE = 0xb01;
    private final int VIEW_TYPE_LEFT = -10;
    private final int VIEW_TYPE_RIGHT = -11;
    private final int MESSAGE = 0xb02;
    private ArrayList<HashMap<Integer, Object>> items = new ArrayList<HashMap<Integer, Object>>();
    private MyAdapter myAdapter;
    // Handler 对象
    private Handler mHandler;
    private ListView lstView;
    private ChatClient chatClient;
    private Button btn_send;
    private EditText editText_send;
    private ImageButton imgBtn_more;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_ui);

        lstView = (ListView) findViewById(android.R.id.list);
        editText_send = (EditText) findViewById(R.id.edit_send);
        btn_send = (Button) findViewById(R.id.btn_send);
        imgBtn_more = (ImageButton) findViewById(R.id.btn_more);

        myAdapter = new MyAdapter(this, -1);
        Log.i("oncreate", "view create");
        lstView.setAdapter(myAdapter);

        // 在 onCreate 方法中初始化 Handler
        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                // 处理接收到的消息并更新 UI
                String receivedMessage = (String) msg.obj;
                // 更新 UI 视图
                onRec(receivedMessage);
            }
        };
        chatClient = ChatClient.get();
        chatClient.setmHandler(mHandler);

        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = editText_send.getText() + "";
                if (msg == "") return;
                HashMap<Integer, Object> map = new HashMap<Integer, Object>();
                map.put(VIEW_TYPE, VIEW_TYPE_RIGHT);
                map.put(MESSAGE, msg);
                items.add(map);
                myAdapter.notifyDataSetChanged();
                // 发送后清空输入框内容
                editText_send.setText(null);
                // 将ListView滚动到最底部
                lstView.setSelection(ListView.FOCUS_DOWN);
                // 发送一个信息
                TalkToOther.TalkToOtherMessage.Builder talkBuild = TalkToOther.TalkToOtherMessage.newBuilder().setToUser(TestLocalMsgConstant.toUser)
                        .setFormUser(TestLocalMsgConstant.fromUser)
                        .setContentType(TransportMessageOuterClass.EnumContentType.Text)
                        .setContent(ByteString.copyFrom(msg.getBytes(StandardCharsets.UTF_8)));
                ChatClient.get().msgSender.sendMsgSync(TransportMessageOuterClass.EnumMsgType.TalkToOther, talkBuild.build(), true);


            }
        });

        //如果软键盘收起，将ListView滚动到最底部
        KeyboardChangeListener softKeyboardStateHelper = new KeyboardChangeListener(this);
        softKeyboardStateHelper.setKeyBoardListener(new KeyboardChangeListener.KeyBoardListener() {
            @Override
            public void onKeyboardChange(boolean isShow, int keyboardHeight) {
                if (isShow) {
                    //键盘的弹出
                    editText_send.setCursorVisible(true);
                    lstView.setSelection(ListView.FOCUS_DOWN);
                } else {
                    //键盘的收起
                    editText_send.setCursorVisible(false);
                }
            }
        });

    }

    public void onRec(String result) {
        Log.w("tcp", "__________enter onSuccess");
        HashMap<Integer, Object> map = new HashMap<Integer, Object>();
        map.put(VIEW_TYPE, VIEW_TYPE_LEFT);
        map.put(MESSAGE, result);
        items.add(map);
        myAdapter.notifyDataSetChanged();
        // 将ListView滚动到最底部
        lstView.setSelection(ListView.FOCUS_DOWN);
    }

    private class MyAdapter extends ArrayAdapter {

        private LayoutInflater layoutInflater;

        public MyAdapter(Context context, int resource) {
            super(context, resource);
            layoutInflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int pos, View convertView, ViewGroup parent) {
            int type = getItemViewType(pos);
            String msg = getItem(pos);
            switch (type) {
                case VIEW_TYPE_LEFT:
                    convertView = layoutInflater.inflate(R.layout.base_left_usr, null);
                    convertView.setAlpha(0.7f);
                    TextView textLeft = (TextView) convertView.findViewById(R.id.usr_msg);
                    textLeft.setText(msg + "");
                    break;
                case VIEW_TYPE_RIGHT:
                    convertView = layoutInflater.inflate(R.layout.base_right_usr, null);
                    convertView.setAlpha(0.7f);
                    TextView textRight = (TextView) convertView.findViewById(R.id.usr_msg);
                    textRight.setText(msg + "");
                    break;
            }
            return convertView;
        }

        @Override
        public String getItem(int pos) {
            String s = items.get(pos).get(MESSAGE) + "";
            return s;
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public int getItemViewType(int pos) {
            int type = (Integer) items.get(pos).get(VIEW_TYPE);
            return type;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }
    }
}