package com.example.temperatureview;

//画图
import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

//蓝牙
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import android.content.res.AssetManager;
import android.media.SoundPool;
import android.os.Bundle;

import android.view.View;
import android.widget.Button;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.view.Menu;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.example.temperatureview.R;

public class MainActivity extends AppCompatActivity {


    private RecyclerView recyclerView;
    Button butblte=null;                //定义一个按键控件,控制蓝牙搜索的

    EditText Tempareture=null;//通道1显示

    boolean enable=false;
    boolean blecon=true;
    boolean bThread=false;
    private List<String> mBuffer;       //定义一个阻塞队列
    String showstr="";
    private InputStream inputStream;    //输入流，用来接收蓝牙数据
    BluetoothSocket socket = null;      // 蓝牙通信socket
    BluetoothDevice device = null;
    BluetoothAdapter adapter=BluetoothAdapter.getDefaultAdapter();  //获取蓝牙
    private final static int REQUEST_CONNECT_DEVICE = 1;            //宏定义查询设备句柄
    private static final String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";

    List<Integer> data;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        data = new ArrayList<>();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        butblte  = (Button)findViewById(R.id.BUTBLTE);  //蓝牙控制按键
        butblte.setOnClickListener(new setclick());     //定义按钮事件

        Tempareture  = (EditText)findViewById(R.id.editText);//左
        if(adapter.isEnabled())//蓝牙可用
            enable=true;//表明蓝牙开启
        recyclerView = findViewById(R.id.recycler_view);
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerView.setLayoutManager(manager);
    }
    /*
     *
     * i
     */
    public class setclick implements OnClickListener
    {
        @Override
        public void onClick(View arg0) {
            if(adapter!=null)
            {
                if(!adapter.isEnabled())
                {
                    Toast.makeText(MainActivity.this,"开启蓝牙"	, 0).show();
                    adapter.enable();//将把蓝牙打开
                }
            }
            else
            {
                Toast.makeText(MainActivity.this,"蓝牙不存在"	, 0).show();
            }
            if(!enable)//如果蓝牙失败，需要重新连接
                blecon=true;
            new BLEThread().start();// 开启蓝牙线程
        }
    }
    /*
     *
     */
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            String a;

            int temperature;
            switch (msg.what) {
                case 1:
                    new BLEInput().start();// 开启蓝牙接收线程

                    break;
                case 2:
                    String result = msg.getData().get("msg").toString();
                    showstr = showstr + result;
                    if (showstr.length() >=7)// 接收到的数据大于7个，表明数据正常
                    {
                        a = showstr.substring(0, 1);
                        if (a.equals("T"))// java字符串从0开始,如果第一个字符是T，表明接收到的数据是正确的
                        {
                            Tempareture.setText(showstr.substring(1,4)+"."+showstr.substring(4,6)+"℃");
                            temperature =Integer.parseInt(showstr.substring(1, 6));
                            data.add(temperature);
                            Hour_Adapter adapter = new Hour_Adapter(MainActivity.this,data);
                            recyclerView.setAdapter(adapter);
                            //更新图上点
                        }
                        showstr = "";
                    }
                    break;
                default:
                    break;
            }
            System.out.println(data);
        }
    };

    //开辟一个线程 ,线程不允许更新UI
    public class BLEThread extends Thread
    {
        public void run()
        {
            while(blecon)
            {
                if(adapter.isEnabled())//蓝牙可用，
                {
                    enable=true;
                    blecon=false;//只进行检测一次
                }
                if(enable)//蓝牙被正常开启了
                {
                    Intent serverIntent = new Intent(MainActivity.this, DeviceListActivity.class); // 跳转程序设置
                    startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE); // 设置返回宏定义
                    enable=false;
                }
            }
        }
    }
    // 接收活动结果，响应startActivityForResult() 安卓回调函数
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {

        switch(requestCode)
        {
            case REQUEST_CONNECT_DEVICE:
                if (resultCode == Activity.RESULT_OK) //搜索到蓝牙，将进行配对
                {
                    String address = data.getExtras()
                            .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    // 得到蓝牙设备句柄
                    device = adapter.getRemoteDevice(address);//远端的
                    try {
                        socket= device.createRfcommSocketToServiceRecord(UUID
                                .fromString(SPP_UUID));//通过socket测试是否连接成功
                    } catch (IOException e) {
                        Toast.makeText(this, "连接失败！", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                    try {
                        socket.connect();
                        butblte.setText("连接成功");
                        Toast.makeText(this, "连接"+device.getName()+"成功！", Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        Toast.makeText(this, "连接失败！", Toast.LENGTH_SHORT).show();

                        e.printStackTrace();
                    }
                    try {
                        inputStream  = socket.getInputStream();
                        new BLEInput().start();//开启蓝牙接收线程
                    } catch (IOException e) {
                        e.printStackTrace();
                    }   //得到蓝牙数据输入流
                }
                break;
            default:break;
        }
    }

    /*
     * 开辟线程来做数据接收使用
     */
    // 开辟一个线程 ,线程不允许更新UI
    public class BLEInput extends Thread {
        String str;
        // 得到一个消息对象，Message类是有Android操作系统提供
        int num;

        public void run() {
            while (true) {

                byte buffer[] = new byte[1024];// 定义1024个字节
                try {
                    num = inputStream.read(buffer);
                    str = new String(buffer, 0, num);
                    Message msg = new Message();
                    msg.what = 2;
                    Bundle data = new Bundle();
                    data.putString("msg", str);
                    msg.setData(data);
                    mHandler.sendMessage(msg);// 发送数据给handler，让其进行数据更新*/
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
}
