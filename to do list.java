// MainActivity.java
package com.example.todolist;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TaskAdapter taskAdapter;
    private ArrayList<Task> taskList;
    private TaskDatabase taskDatabase;
    private TextView tvNoTasks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerView);
        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);
        tvNoTasks = findViewById(R.id.tvNoTasks);

        taskDatabase = new TaskDatabase(this);
        taskList = new ArrayList<>();

        loadTodayTasks();

        taskAdapter = new TaskAdapter(this, taskList, task -> {
            Intent intent = new Intent(MainActivity.this, AddEditTaskActivity.class);
            intent.putExtra("TASK_ID", task.getId());
            startActivity(intent);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(taskAdapter);

        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddEditTaskActivity.class);
            startActivity(intent);
        });
    }

    private void loadTodayTasks() {
        taskList.clear();
        taskList.addAll(taskDatabase.getTodayTasks());

        if (taskList.isEmpty()) {
            tvNoTasks.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvNoTasks.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTodayTasks();
        taskAdapter.notifyDataSetChanged();
    }
}

// AddEditTaskActivity.java
package com.example.todolist;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;

public class AddEditTaskActivity extends AppCompatActivity {

    private EditText etTaskTitle, etTaskDescription, etTaskDate, etTaskTime;
    private Spinner spTaskPriority, spTaskStatus;
    private Button btnSave;

    private TaskDatabase taskDatabase;
    private int taskId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_task);

        etTaskTitle = findViewById(R.id.etTaskTitle);
        etTaskDescription = findViewById(R.id.etTaskDescription);
        etTaskDate = findViewById(R.id.etTaskDate);
        etTaskTime = findViewById(R.id.etTaskTime);
        spTaskPriority = findViewById(R.id.spTaskPriority);
        spTaskStatus = findViewById(R.id.spTaskStatus);
        btnSave = findViewById(R.id.btnSave);

        taskDatabase = new TaskDatabase(this);

        if (getIntent().hasExtra("TASK_ID")) {
            taskId = getIntent().getIntExtra("TASK_ID", -1);
            Task task = taskDatabase.getTask(taskId);
            if (task != null) {
                etTaskTitle.setText(task.getTitle());
                etTaskDescription.setText(task.getDescription()); 
                
                etTaskDate.setText(task.getDate());
                etTaskTime.setText(task.getTime());
                spTaskPriority.setSelection(task.getPriority());
                spTaskStatus.setSelection(task.getStatus());
            }
        }

        btnSave.setOnClickListener(v -> saveTask());
    }

    private void saveTask() {
        String title = etTaskTitle.getText() .toString().trim();
        String description = etTaskDescription .getText().toString().trim();
        String date = etTaskDate.getText() .toString().trim();
        String time = etTaskTime.getText() .toString().trim();
        int priority = spTaskPriority.getSelectedItemPosition();
        int status = spTaskStatus.getSelectedItemPosition();

        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a title", Toast.LENGTH_SHORT).show();
            return;
        }

        if (taskId  ==  -1) {
            taskId = taskDatabase.addTask(new Task(title, description, date, time, priority, status));
            setReminder(taskId, date, time);
            Toast.makeText(this, "Task added", Toast.LENGTH_SHORT).show();
        } else {
            taskDatabase.updateTask(new Task(taskId, title, description, date, time, priority, status));
            setReminder(taskId ,  date,  time);
            Toast.makeText(this, "Task updated", Toast.LENGTH_SHORT).show();
        }

        finish();
    }

    private void setReminder(int taskId, String date, String time) {
        Calendar calendar = Calendar.getInstance();
        String[] dateParts = date.split("-");
        String[] timeParts = time.split(":");

        calendar.set(Calendar.YEAR, Integer.parseInt(dateParts[0]));
        calendar.set(Calendar.MONTH, Integer.parseInt(dateParts[1]) - 1);
        calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(dateParts[2]));
        calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeParts[0]));
        calendar.set(Calendar.MINUTE, Integer.parseInt(timeParts[1]));
        calendar.set(Calendar.SECOND, 0);

        Intent intent = new Intent(this, ReminderReceiver.class);
        intent.putExtra("TASK_ID", taskId);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, taskId, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
    }
}

// ReminderReceiver.java
package com.example.todolist;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class ReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "TASK_REMINDER_CHANNEL";

    @Override
    public void onReceive(Context context, Intent intent) {
        int taskId = intent.getIntExtra("TASK_ID", -1);

        Intent notificationIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, taskId, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Task Reminder", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_task)
                .setContentTitle("Task Reminder")
                .setContentText("You have a task to complete.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        notificationManager.notify(taskId, builder.build());
    }
}
