package sg.edu.nus.memorygame;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MyAsyncTask extends AsyncTask<String, Integer, String>
{
    private List<String> imgUrls = new ArrayList<String>();
    private ICallback callback = null;

    public interface ICallback
    {
        void onAsyncTaskProgress(int progress);

        void onAsyncTaskCompleted(String s);

    }

    public MyAsyncTask(ICallback callback)
    {
        this.callback = callback;
    }

    @Override
    protected String doInBackground(String... params)
    {
        InputStream is;
        BufferedReader br;
        String line;
        Bitmap bitmap;

        int i = 0;
        try
        {
            URL url1 = new URL(params[0]);
            is = url1.openStream();
            br = new BufferedReader(new InputStreamReader(is));

            while ((line = br.readLine()) != null)
            {
                if (line.contains("img"))
                {
                    String[] sp = line.split("[\"]");
                    for (String s : sp)
                    {
                        if (s.contains("https") && s.endsWith(".jpg"))
                            imgUrls.add(s);
                    }
                    if (imgUrls.size() >= 20)
                        break;
                }
            }

            for (String img : imgUrls)
            {
                URL url2 = new URL(img);
                bitmap = BitmapFactory.decodeStream(url2.openConnection().getInputStream());
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                byte[] byteArray = stream.toByteArray();
                String path = params[1] + "/img" + i + ".jpg";
                OutputStream out = new FileOutputStream(path);
                out.write(byteArray);
                out.flush();
                out.close();
                publishProgress(i);
                i++;
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    protected void onProgressUpdate(Integer... values)
    {
        if (callback != null)
            callback.onAsyncTaskProgress(values[0]);
    }

    @Override
    protected void onPostExecute(String s)
    {
        super.onPostExecute(s);
        if (callback != null)
            callback.onAsyncTaskCompleted("Completed");
    }
}