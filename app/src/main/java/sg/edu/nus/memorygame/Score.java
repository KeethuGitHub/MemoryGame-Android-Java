package sg.edu.nus.memorygame;

import java.util.Objects;

public class Score implements Comparable<Score>
{
    private String name;
    private long score;

    public Score()
    {
    }

    public Score(String name, long score)
    {
        this.name = name;
        this.score = score;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public long getScore()
    {
        return score;
    }

    public void setScore(int score)
    {
        this.score = score;
    }

    @Override
    public String toString()
    {
        return name + "," + score;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Score score1 = (Score) o;
        return score == score1.score &&
                Objects.equals(name, score1.name);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(name, score);
    }

    public int compareTo(Score other)
    {
        return (int) (score - other.getScore());
    }
}