// MainActivity.java
package com.example.todolist;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TaskAdapter taskAdapter;
    private ArrayList<Task> taskList;
    private TaskApi taskApi;
    private TextView tvNoTasks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        taskApi = RetrofitClient.getClient().create(TaskApi.class);

        recyclerView = findViewById(R.id.recyclerView);
        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);
        tvNoTasks = findViewById(R.id.tvNoTasks);

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
        taskApi.getTodayTasks().enqueue(new Callback<List<Task>>() {
            @Override
            public void onResponse(Call<List<Task>> call, Response<List<Task>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    taskList.clear();
                    taskList.addAll(response.body());
                    taskAdapter.notifyDataSetChanged();

                    if (taskList.isEmpty()) {
                        tvNoTasks.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        tvNoTasks.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Task>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Failed to load tasks", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTodayTasks();
    }
}

// AddEditTaskActivity.java
package com.example.todolist;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddEditTaskActivity extends AppCompatActivity {

    private EditText etTaskTitle, etTaskDescription, etTaskDate, etTaskTime;
    private Spinner spTaskPriority, spTaskStatus;
    private Button btnSave;
    private TaskApi taskApi;
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

        taskApi = RetrofitClient.getClient().create(TaskApi.class);

        if (getIntent().hasExtra("TASK_ID")) {
            taskId = getIntent().getIntExtra("TASK_ID", -1);
            loadTask(taskId);
        }

        btnSave.setOnClickListener(v -> saveTask());
    }

    private void loadTask(int taskId) {
        taskApi.getTask(taskId).enqueue(new Callback<Task>() {
            @Override
            public void onResponse(Call<Task> call, Response<Task> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Task task = response.body();
                    etTaskTitle.setText(task.getTitle());
                    etTaskDescription.setText(task.getDescription());
                    etTaskDate.setText(task.getDate());
                    etTaskTime.setText(task.getTime());
                    spTaskPriority.setSelection(task.getPriority());
                    spTaskStatus.setSelection(task.getStatus());
                }
            }

            @Override
            public void onFailure(Call<Task> call, Throwable t) {
                Toast.makeText(AddEditTaskActivity.this, "Failed to load task", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveTask() {
        String title = etTaskTitle.getText().toString().trim();
        String description = etTaskDescription.getText().toString().trim();
        String date = etTaskDate.getText().toString().trim();
        String time = etTaskTime.getText().toString().trim();
        int priority = spTaskPriority.getSelectedItemPosition();
        int status = spTaskStatus.getSelectedItemPosition();

        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a title", Toast.LENGTH_SHORT).show();
            return;
        }

        Task task = new Task(title, description, date, time, priority, status);

        if (taskId == -1) {
            taskApi.addTask(task).enqueue(new Callback<Task>() {
                @Override
                public void onResponse(Call<Task> call, Response<Task> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(AddEditTaskActivity.this, "Task added", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }

                @Override
                public void onFailure(Call<Task> call, Throwable t) {
                    Toast.makeText(AddEditTaskActivity.this, "Failed to add task", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            taskApi.updateTask(taskId, task).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(AddEditTaskActivity.this, "Task updated", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    Toast.makeText(AddEditTaskActivity.this, "Failed to update task", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}

// RetrofitClient.java
package com.example.todolist;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static final String BASE_URL = "https://your-api-url.com/";
    private static Retrofit retrofit;

    public static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}

// TaskApi.java to do list

package com.example.todolist;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface TaskApi {
    @GET("tasks/today")
    Call<List<Task>> getTodayTasks();

    @GET("tasks/{id}")
    Call<Task> getTask(@Path("id") int id);

    @POST("tasks")
    Call<Task> addTask(@Body Task task);

    @PUT("tasks/{id}")
    Call<Void> updateTask(@Path("id") int id, @Body Task task);
}

// Task.java تو دو لیست
package com.example.todolist;

public class Task {
    private int id;
    private String title;
    private String description;
    private String date;
    private String time;
    private int priority;
    private int status;

    public Task(String title, String description, String date, String time, int priority, int status) {
        this.title = title;
        this.description = description;
        this.date = date;
        this.time = time;
        this.priority = priority;
        this.status = status;
    }

    public Task(int id, String title, String description, String date, String time, int priority, int status) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.date = date;
        this.time = time;
        this.priority = priority;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getDate() {
        return date;
    }

    public String getTime() {
        return time;
    }

    public int getPriority() {
        return priority;
    }

    public int getStatus() {
        return status;
    }
}
