package sg.edu.nus.memorygame;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements MyAsyncTask.ICallback
{
    private Button fetchBtn;
    private EditText urlInput;
    private String urlString = "https://stocksnap.io";
    private ProgressBar progBar;
    private TextView progBarText;
    private Button startBtn;

    private final int numOfImages = 20;
    private ImageView[] imageViews = new ImageView[numOfImages];
    private String[] dlImagesPaths = new String[numOfImages];
    private List<Integer> selectedViews = new ArrayList<Integer>();

    private AsyncTask mytask;
    private String asyncStatus = "";

    private MediaPlayer mediaPlayer;
    private boolean isPaused = false;
    private final float BKGD_LEFT_VOL = 1.0f;
    private final float BKGD_RIGHT_VOL = 1.0f;
    private ImageButton musicBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null)
        {
            urlString = savedInstanceState.getString("urlInput");
            dlImagesPaths = savedInstanceState.getStringArray("dlImagesPaths");
            selectedViews = savedInstanceState.getIntegerArrayList("selectedViews");
            asyncStatus = savedInstanceState.getString("asyncStatus");
            isPaused = savedInstanceState.getBoolean("isPaused");
        }

        urlInput = findViewById(R.id.urlInput);
        urlInput.setText(urlString);
        fetchBtn = findViewById(R.id.fetchBtn);
        startBtn = findViewById(R.id.startBtn);

        for (int i = 0; i < numOfImages; i++)
        {
            int imgId = getResources().getIdentifier("img" + i, "id", getPackageName());
            imageViews[i] = findViewById(imgId);
        }

        if (dlImagesPaths.length != 0)
        {
            for (int i = 0; i < dlImagesPaths.length; i++)
            {
                final Integer index = i;
                Bitmap imgBmp = BitmapFactory.decodeFile(dlImagesPaths[i]);
                imageViews[i].setImageBitmap(imgBmp);
                imageViews[i].setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        SelectImage(view, index);
                    }
                });
            }
        }

        if (selectedViews.size() != 0)
        {
            for (int i = 0; i < selectedViews.size(); i++)
                findViewById(getResources().getIdentifier("img" + selectedViews.get(i), "id", getPackageName())).setAlpha(0.2f);

            if (selectedViews.size() == 6)
                startBtn.setVisibility(View.VISIBLE);
        }

        fetchBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                final String urlResult = urlInput.getText().toString();
                if (mytask == null && asyncStatus.equals(""))
                    fetch(urlResult);
                else
                {
                    if (mytask != null && mytask.getStatus() != AsyncTask.Status.FINISHED)
                    {
                        mytask.cancel(true);
                    }
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            ClearDownloadedImages();
                            startBtn.setVisibility(View.INVISIBLE);
                            fetch(urlResult);
                        }
                    }, 500);
                }
                //InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                //inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        });

        progBar = findViewById(R.id.progressBar);
        progBarText = findViewById(R.id.tv);

        startBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                startGame();
            }
        });

        mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.home);
        mediaPlayer.setLooping(true);
        mediaPlayer.setVolume(BKGD_LEFT_VOL, BKGD_RIGHT_VOL);

        musicBtn = findViewById(R.id.musicBtn);
        musicBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                StartStopMusic();
            }
        });
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        if (!isPaused)
            mediaPlayer.start();
        else
            musicBtn.setImageResource(R.drawable.music_off);
    }

    public void onResume()
    {
        super.onResume();
        if (!isPaused)
            mediaPlayer.start();
        else
            musicBtn.setImageResource(R.drawable.music_off);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        mediaPlayer.pause();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        mediaPlayer.stop();
        mediaPlayer.release();
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putString("urlInput", urlInput.getText().toString());
        outState.putStringArray("dlImagesPaths", dlImagesPaths);
        outState.putIntegerArrayList("selectedViews", (ArrayList<Integer>) selectedViews);
        if (mytask == null && asyncStatus.equals("")) // First instantiation
            outState.putString("asyncStatus", "");
        else if (mytask != null && mytask.getStatus() != AsyncTask.Status.FINISHED) // Cancel download
            outState.putString("asyncStatus", "Cancelled");
        else if (mytask != null && mytask.getStatus() == AsyncTask.Status.FINISHED) // Completed download
            outState.putString("asyncStatus", "Finished");
        else if (mytask == null && asyncStatus.equals("Finished")) // Completed download and n flips
            outState.putString("asyncStatus", "Finished");
        outState.putBoolean("isPaused", isPaused);
    }

    private void fetch(String urlResult)
    {
        String fileDir = getFilesDir().toString();
        progBar.setProgress(0);
        progBarText.setText("Downloading 0 of 20 Images... ");
        progBar.setVisibility(View.VISIBLE);
        progBarText.setVisibility(View.VISIBLE);
        dlImagesPaths = new String[numOfImages];
        selectedViews = new ArrayList<Integer>();
        mytask = new MyAsyncTask(this).execute(urlResult, fileDir);
    }

    public void onAsyncTaskProgress(int i)
    {
        if (progBar != null)
        {
            progBar.setProgress(i);
            int count = i + 1;
            progBarText.setText("Downloading " + count + " of 20 Images... ");
        }
        String imgPath = getFilesDir().toString() + "/img" + i + ".jpg";
        dlImagesPaths[i] = imgPath;
        Integer index = i;
        Bitmap imgBmp = BitmapFactory.decodeFile(imgPath);
        imageViews[i].setImageBitmap(imgBmp);
        imageViews[i].setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                SelectImage(view, index);
            }
        });
    }

    public void onAsyncTaskCompleted(String s)
    {
        if (s.equals("Completed"))
        {
            progBar.setVisibility(View.INVISIBLE);
            progBarText.setVisibility(View.INVISIBLE);
        }
    }

    private void StartStopMusic()
    {
        if (!isPaused)
        {
            mediaPlayer.pause();
            isPaused = true;
            musicBtn.setImageResource(R.drawable.music_off);
        } else
        {
            mediaPlayer.start();
            isPaused = false;
            musicBtn.setImageResource(R.drawable.music_on);
        }
    }

    private void SelectImage(View view, Integer index)
    {
        if (selectedViews.contains(index))
        {
            selectedViews.remove(index);
            view.setAlpha(1.0f);
        } else
        {
            if (selectedViews.size() < 6)
            {
                selectedViews.add(index);
                view.setAlpha(0.2f);
            }
        }
        if (selectedViews.size() == 6)
            startBtn.setVisibility(View.VISIBLE);
        else
            startBtn.setVisibility(View.INVISIBLE);
    }

    private void ClearDownloadedImages()
    {
        for (int i = 0; i < 20; i++)
        {
            imageViews[i].setImageDrawable(null);
            imageViews[i].setAlpha(1.0f);
        }
    }

    private void startGame()
    {
        Intent intent = new Intent(MainActivity.this, GameActivity.class); //change the class to our game class
        intent.putIntegerArrayListExtra("selectedViews", (ArrayList<Integer>) selectedViews);
        startActivity(intent);
    }
}