package com.tpsoft.notifyclient;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.tpsoft.pushnotification.model.MyMessage;

public class SendMessageActivity extends Activity implements
		View.OnClickListener {

	private static int nextMsgId = 1;

	private Button sendButton;
	private EditText receiverView, msgTitleView, msgBodyView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.send_message);

		sendButton = (Button) findViewById(R.id.sendMsg);
		sendButton.setOnClickListener(this);

		receiverView = (EditText) findViewById(R.id.msgReceiver);
		msgTitleView = (EditText) findViewById(R.id.msgTitle2);
		msgBodyView = (EditText) findViewById(R.id.msgBody2);
	}

	@Override
	public void onClick(View view) {
		MyMessage msg = new MyMessage();
		msg.setTitle(msgTitleView.getText().toString());
		msg.setBody(msgBodyView.getText().toString());

		Intent i = new Intent();
		i.setAction("com.tpsoft.pushnotification.ServiceController");
		i.putExtra("command", "send");
		i.putExtra("msgId", Integer.toString(nextMsgId++));
		i.putExtra("receiver", receiverView.getText().toString());
		// i.putExtra("secure", false);
		i.putExtra("com.tpsoft.pushnotification.MyMessage", msg.getBundle());
		sendBroadcast(i);

		finish();
	}
}
