package sg.edu.nus.memorygame;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class GameActivity extends AppCompatActivity implements View.OnClickListener
{
    private final int numOfImages = 6;
    private final int numOfViews = 12;
    private String[] imgPaths = new String[numOfImages];
    private List<Integer> imgOrders = new ArrayList<Integer>();
    private ImageView[] qnsViews = new ImageView[numOfViews];
    private ImageView[] imgViews = new ImageView[numOfViews];
    private HashMap<Integer, Integer> mapQnsImgIds = new HashMap<Integer, Integer>();
    private List<Integer> flippedQnsIds = new ArrayList<Integer>();
    private List<Integer> clearedViewsIds = new ArrayList<Integer>();

    private int numOfFlips = 0;
    private int numOfPairs = 0;

    private TextView tryCounter;
    private int numOfTries = 0;

    private Chronometer chronometer;
    private long timeElapsed = 0;

    private MediaPlayer mediaPlayer;
    private boolean isPaused = false;
    private final float BKGD_LEFT_VOL = 1.0f;
    private final float BKGD_RIGHT_VOL = 1.0f;
    private ImageButton musicBtn;

    private SoundPool gameSound;
    private HashMap<Integer, Integer> soundsMap = new HashMap<Integer, Integer>();
    private final int SUCCESS = 1;
    private final int FAIL = 2;
    private final int CHEER = 3;
    private final float GAME_LEFT_VOL = 0.4f;
    private final float GAME_RIGHT_VOL = 0.4f;
    private final int PRIORITY = 1;
    private final int LOOP = 0;
    private final float RATE = 2.0f;

    private Button saveScoreBtn;
    private boolean isGameFinished = false;
    private long totalScore = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        if (savedInstanceState != null)
        {
            imgPaths = savedInstanceState.getStringArray("imgPaths");
            imgOrders = savedInstanceState.getIntegerArrayList("imgOrders");
            flippedQnsIds = savedInstanceState.getIntegerArrayList("flippedQnsIds");
            clearedViewsIds = savedInstanceState.getIntegerArrayList("clearedViewsIds");
            numOfFlips = savedInstanceState.getInt("numOfFlips");
            numOfPairs = savedInstanceState.getInt("numOfPairs");
            numOfTries = savedInstanceState.getInt("numOfTries");
            timeElapsed = savedInstanceState.getLong("timeElapsed");
            isGameFinished = savedInstanceState.getBoolean("isGameFinished");
            totalScore = savedInstanceState.getLong("totalScore");
            isPaused = savedInstanceState.getBoolean("isPaused");
        } else
        {
            final Intent intentStarter = getIntent();
            List<Integer> selectedViews = null;
            if (intentStarter != null)
            {
                final Bundle extraBundle = intentStarter.getExtras();
                if (extraBundle != null)
                    selectedViews = extraBundle.getIntegerArrayList("selectedViews");
            }

            if (selectedViews != null)
            {
                for (int i = 0; i < selectedViews.size(); i++)
                    imgPaths[i] = getFilesDir().toString() + "/img" + selectedViews.get(i) + ".jpg";
            }

            List<Integer> range = IntStream.range(0, numOfImages)
                    .boxed().collect(Collectors.toList());
            List<Integer> l1 = new ArrayList<Integer>(range);
            Collections.shuffle(l1);
            List<Integer> l2 = new ArrayList<Integer>(range);
            Collections.shuffle(l2);
            imgOrders.addAll(l1);
            imgOrders.addAll(l2);
        }

        Bitmap qnsBmp = BitmapFactory.decodeResource(getApplicationContext().getResources(),
                R.drawable.qnsmark);
        for (int i = 0; i < numOfViews; i++)
        {
            int qnsId = getResources().getIdentifier("qns" + i, "id", getPackageName());
            qnsViews[i] = findViewById(qnsId);
            qnsViews[i].setImageBitmap(qnsBmp);
            qnsViews[i].setOnClickListener(this);

            Bitmap imgBmp = BitmapFactory.decodeFile(imgPaths[imgOrders.get(i)]);
            int imgId = getResources().getIdentifier("img" + i, "id", getPackageName());
            imgViews[i] = findViewById(imgId);
            imgViews[i].setImageBitmap(imgBmp);

            mapQnsImgIds.put(qnsId, imgId);
        }

        if (flippedQnsIds.size() != 0)
        {
            for (int i = 0; i < flippedQnsIds.size(); i++)
                findViewById(flippedQnsIds.get(i)).setVisibility(View.INVISIBLE);
        }

        if (clearedViewsIds.size() != 0)
        {
            for (int i = 0; i < clearedViewsIds.size(); i++)
                findViewById(clearedViewsIds.get(i)).setVisibility(View.INVISIBLE);
        }

        tryCounter = findViewById(R.id.try_counter);
        tryCounter.setText(String.format("Tries: %d", numOfTries));

        saveScoreBtn = findViewById(R.id.save_score);
        saveScoreBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (isGameFinished)
                    SaveScore(totalScore);
            }
        });

        chronometer = findViewById(R.id.chronometer);

        mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.game);
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

        AudioAttributes gameSoundAttr = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        gameSound = new SoundPool.Builder()
                .setAudioAttributes(gameSoundAttr)
                .setMaxStreams(5)
                .build();

        soundsMap.put(SUCCESS, gameSound.load(this, R.raw.success, 1));
        soundsMap.put(FAIL, gameSound.load(this, R.raw.fail, 1));
        soundsMap.put(CHEER, gameSound.load(this, R.raw.cheer, 1));
    }

    @Override
    public void onStart()
    {
        super.onStart();
        if (!isGameFinished)
        {
            chronometer.setBase(SystemClock.elapsedRealtime() - timeElapsed);
            chronometer.start();
        }
        if (!isPaused)
            mediaPlayer.start();
        else
            musicBtn.setImageResource(R.drawable.music_off);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!isGameFinished)
        {
            chronometer.setBase(SystemClock.elapsedRealtime() - timeElapsed);
            chronometer.start();
        }
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
        timeElapsed = SystemClock.elapsedRealtime() - chronometer.getBase();
        chronometer.stop();
    }

    @Override
    public void onStop()
    {
        super.onStop();
        mediaPlayer.pause();
        timeElapsed = SystemClock.elapsedRealtime() - chronometer.getBase();
        chronometer.stop();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        mediaPlayer.stop();
        mediaPlayer.release();
        gameSound.release();
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putStringArray("imgPaths", imgPaths);
        outState.putIntegerArrayList("imgOrders", (ArrayList<Integer>) imgOrders);
        outState.putIntegerArrayList("flippedQnsIds", (ArrayList<Integer>) flippedQnsIds);
        outState.putIntegerArrayList("clearedViewsIds", (ArrayList<Integer>) clearedViewsIds);
        outState.putInt("numOfFlips", numOfFlips);
        outState.putInt("numOfPairs", numOfPairs);
        outState.putInt("numOfTries", numOfTries);
        outState.putLong("timeElapsed", timeElapsed);
        outState.putBoolean("isGameFinished", isGameFinished);
        outState.putLong("totalScore", totalScore);
        outState.putBoolean("isPaused", isPaused);
    }

    @Override
    public void onClick(View v)
    {
        v.setVisibility(View.INVISIBLE);
        numOfFlips++;
        flippedQnsIds.add(v.getResources().getIdentifier(v.getContentDescription().toString(), "id", getPackageName()));

        if (numOfFlips == 2)
        {
            boolean isSame = CheckImages(flippedQnsIds);
            if (isSame)
            {
                PlaySound(SUCCESS);
                final List<Integer> oldFlippedQnsIds = new ArrayList<Integer>(flippedQnsIds);
                RemoveSameImages(oldFlippedQnsIds);
                numOfPairs++;
                clearedViewsIds.add(oldFlippedQnsIds.get(0));
                clearedViewsIds.add(oldFlippedQnsIds.get(1));
                clearedViewsIds.add(mapQnsImgIds.get(oldFlippedQnsIds.get(0)));
                clearedViewsIds.add(mapQnsImgIds.get(oldFlippedQnsIds.get(1)));
            } else
            {
                PlaySound(FAIL);
                final List<Integer> oldFlippedQnsIds = new ArrayList<Integer>(flippedQnsIds);
                UnflipDifferentImages(oldFlippedQnsIds);
            }
            numOfTries++;
            tryCounter.setText(String.format(Locale.ENGLISH, "Tries: %d", numOfTries));
            numOfFlips = 0;
            flippedQnsIds.clear();
        }

        if (numOfPairs == numOfImages)
        {
            timeElapsed = SystemClock.elapsedRealtime() - chronometer.getBase();
            chronometer.stop();
            PlaySound(CHEER);
            isGameFinished = true;
            totalScore = numOfTries * 1000 + timeElapsed;
            Toast.makeText(this, "Well Done!", Toast.LENGTH_LONG).show();
        }
    }

    private boolean CheckImages(List<Integer> flippedQnsIds)
    {
        boolean isSame = false;
        ImageView imageView1 = findViewById(mapQnsImgIds.get(flippedQnsIds.get(0)));
        ImageView imageView2 = findViewById(mapQnsImgIds.get(flippedQnsIds.get(1)));
        Bitmap bitmap1 = ((BitmapDrawable) imageView1.getDrawable()).getBitmap();
        Bitmap bitmap2 = ((BitmapDrawable) imageView2.getDrawable()).getBitmap();

        if (bitmap1.sameAs(bitmap2))
            isSame = true;

        return isSame;
    }

    private void RemoveImages(List<Integer> oldFlippedQnsIds)
    {
        ImageView imageView1 = findViewById(mapQnsImgIds.get(oldFlippedQnsIds.get(0)));
        ImageView imageView2 = findViewById(mapQnsImgIds.get(oldFlippedQnsIds.get(1)));
        imageView1.setVisibility(View.INVISIBLE);
        imageView2.setVisibility(View.INVISIBLE);
    }

    private void UnflipImages(List<Integer> oldFlippedQnsIds)
    {
        ImageView imageView1 = findViewById(oldFlippedQnsIds.get(0));
        ImageView imageView2 = findViewById(oldFlippedQnsIds.get(1));
        imageView1.setVisibility(View.VISIBLE);
        imageView2.setVisibility(View.VISIBLE);
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

    private void PlaySound(int type)
    {
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                gameSound.play(soundsMap.get(type), GAME_LEFT_VOL, GAME_RIGHT_VOL, PRIORITY, LOOP, RATE);
            }
        }).start();
    }

    private void RemoveSameImages(List<Integer> oldFlippedQnsIds)
    {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                RemoveImages(oldFlippedQnsIds);
            }
        }, 500);
    }

    private void UnflipDifferentImages(List<Integer> oldFlippedQnsIds)
    {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                UnflipImages(oldFlippedQnsIds);
            }
        }, 500);
    }

    private void SaveScore(long totalScore)
    {
        Intent intent = new Intent(GameActivity.this, ScoreboardActivity.class);
        intent.putExtra("userscore", totalScore);
        startActivity(intent);
    }
}