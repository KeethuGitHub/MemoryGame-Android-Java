package sg.edu.nus.memorygame;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScoreboardActivity extends AppCompatActivity
{
    private EditText nameEdit;
    private Long score;
    private Button getName;
    private LinearLayout enterName;
    private List<Score> scores = null;
    private TextView n1;
    private TextView n2;
    private TextView n3;
    private TextView n4;
    private TextView n5;
    private TextView s1;
    private TextView s2;
    private TextView s3;
    private TextView s4;
    private TextView s5;
    private LinearLayout ll;
    private Button homeBtn;
    private String filepath;
    private LinearLayout con;
    private String checkState = "";
    private String name = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scoreboard);

        filepath = getFilesDir().toString() + "/score.txt";
        enterName = findViewById(R.id.score_layer);
        n1 = findViewById(R.id.name1);
        s1 = findViewById(R.id.score1);
        n2 = findViewById(R.id.name2);
        s2 = findViewById(R.id.score2);
        n3 = findViewById(R.id.name3);
        s3 = findViewById(R.id.score3);
        n4 = findViewById(R.id.name4);
        s4 = findViewById(R.id.score4);
        n5 = findViewById(R.id.name5);
        s5 = findViewById(R.id.score5);
        nameEdit = findViewById(R.id.enterName);
        getName = findViewById(R.id.getName);
        homeBtn = findViewById(R.id.homeBtn);

        ll = findViewById(R.id.scoreView);
        con = findViewById(R.id.congrats);

        if (savedInstanceState != null)
        {
            checkState = savedInstanceState.getString("state");
            score = savedInstanceState.getLong("score");
            name = savedInstanceState.getString("nameEdit");
            if (checkState.equals("scoreboard"))
            {
                checkState = "scoreboard";
                scores = readFromCSV();
                displayScoreBoard();
            } else if (checkState.equals("setname"))
            {
                enterName.setVisibility(View.VISIBLE);
                con.setVisibility(View.VISIBLE);
                checkState = "setname";
            }
        } else
        {
            File tempFile = new File(filepath);
            if (!tempFile.exists())
                setdefaultscores();
            Intent callerIntent = getIntent();
            score = callerIntent.getLongExtra("userscore", 0);

            if (checkHighScore(score))
            {
                enterName.setVisibility(View.VISIBLE);
                con.setVisibility(View.VISIBLE);
                checkState = "setname";
            } else
            {
                displayScoreBoard();
                checkState = "scoreboard";
            }
        }

        getName.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                String name = nameEdit.getText().toString();
                Score s = new Score(name, score);
                updateScore(s);
                enterName.setVisibility(View.GONE);
                con.setVisibility(View.GONE);
                displayScoreBoard();
                checkState = "scoreboard";
            }
        });

        homeBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(ScoreboardActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putString("state", checkState);
        outState.putString("nameEdit", nameEdit.getText().toString());
        outState.putLong("score", score);
    }

    public List<Score> readFromCSV()
    {
        String data = "";
        List<Score> Scores = null;
        try
        {
            FileInputStream fis = new FileInputStream(filepath);
            DataInputStream in = new DataInputStream(fis);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));

            String strLine;
            while ((strLine = br.readLine()) != null)
            {
                data = data + strLine;
            }
            in.close();

            String[] individualScore = data.split(",");
            Scores = new ArrayList<Score>();
            for (int i = 0; i < 10; i = i + 2)
            {
                Scores.add(new Score(individualScore[i], Integer.parseInt(individualScore[i + 1])));
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        return Scores;
    }

    public Boolean checkHighScore(long score)
    {
        scores = readFromCSV();
        if (scores.get(4).getScore() < score)
            return false;
        return true;
    }

    public void updateScore(Score score)
    {
        List<Score> scorelist = readFromCSV();
        scorelist.remove(4);
        scorelist.add(score);
        scores = scorelist;
        String sorteddata = sorting(scorelist);
        writeToCSV(sorteddata);
    }

    public void writeToCSV(String data)
    {
        filepath = getFilesDir().toString() + "/score.txt";
        File file = new File(filepath);
        try
        {
            FileOutputStream fos = new FileOutputStream(filepath);
            fos.write(data.getBytes());
            fos.close();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static String sorting(List<Score> scores)
    {
        Collections.sort(scores);
        String output = "";
        for (Score s : scores)
        {
            output = output + s.toString() + ",";
        }
        return output;
    }

    public void displayScoreBoard()
    {
        if (scores != null)
        {
            ll.setVisibility(View.VISIBLE);
            n1.setText(scores.get(0).getName());
            s1.setText(Long.toString(scores.get(0).getScore()));
            n2.setText(scores.get(1).getName());
            s2.setText(Long.toString(scores.get(1).getScore()));
            n3.setText(scores.get(2).getName());
            s3.setText(Long.toString(scores.get(2).getScore()));
            n4.setText(scores.get(3).getName());
            s4.setText(Long.toString(scores.get(3).getScore()));
            n5.setText(scores.get(4).getName());
            s5.setText(Long.toString(scores.get(4).getScore()));
        }
    }

    public void setdefaultscores()
    {
        Score s1 = new Score("player 1", 15000);
        Score s2 = new Score("player 2", 20000);
        Score s3 = new Score("player 3", 25000);
        Score s4 = new Score("player 4", 30000);
        Score s5 = new Score("player 5", 40000);
        String scorestring = s1.toString() + "," + s2.toString() + "," + s3.toString() + ","
                + s4.toString() + "," + s5.toString();
        File file = new File(filepath);
        try
        {
            FileOutputStream fos = new FileOutputStream(filepath);
            fos.write(scorestring.getBytes());
            fos.close();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}