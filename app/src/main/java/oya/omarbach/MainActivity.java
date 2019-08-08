package oya.omarbach;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;

public class MainActivity extends Activity implements SensorEventListener {
    static int length;
    int flag;
    boolean accFinished = false;
    ArrayList<Double> samples = new ArrayList<>();
    int s;
    double magnitude;
    ArrayList<Long> time = new ArrayList<>();
    private float timestampOld;
    private String name;
    private int i = 0;
    private SensorManager sensorManager;
    private Sensor senAccelerometer;
    private float[] filteredAcceleration = new float[3];

    public static double DTW(ArrayList<Double> template, ArrayList<Double> sample) {
        Double[] s = template.toArray(new Double[template.size()]), t = sample.toArray(new Double[sample.size()]);
        double[][] DTW = new double[s.length][t.length];
        DTW[0][0] = 0;
        for (int i = 1; i < s.length; i++) {
            DTW[i][0] = Math.abs(s[i] - t[0]) + DTW[i - 1][0];
        }
        for (int i = 1; i < t.length; i++) {
            DTW[0][i] = Math.abs(s[0] - t[i]) + DTW[0][i - 1];
        }
        for (int i = 1; i < s.length; i++) {
            for (int j = 1; j < t.length; j++) {
                double cost = Math.abs(s[i] - t[j]);
                DTW[i][j] = cost + Math.min(DTW[i - 1][j], Math.min(DTW[i][j - 1], DTW[i - 1][j - 1]));
            }
        }
        double sum = DTW[s.length - 1][t.length - 1];
        int i = s.length - 1;
        int j = t.length - 1;
        double min;
        while (i > 0 && j > 0) {
            if (DTW[i - 1][j - 1] < DTW[i - 1][j] && DTW[i - 1][j - 1] < DTW[i][j - 1]) {
                min = DTW[i - 1][j - 1];
                i--;
                j--;
            } else if (DTW[i - 1][j] < DTW[i][j - 1] && DTW[i - 1][j] < DTW[i - 1][j - 1]) {
                min = DTW[i - 1][j];
                i--;
            } else {
                min = DTW[i][j - 1];
                j--;
            }
            sum += min;
        }
        if (i == 0) {
            while (j != 0) {
                j--;
                sum += DTW[i][j];
            }
            return sum;
        } else if (j == 0) {
            while (i != 0) {
                i--;
                sum += DTW[i][j];
            }
            return sum;
        }
        return sum;
    }

    // estimation of cycle length
    public static int estimateCycleLength(ArrayList<Double> samples) {
        int diff = 30; //window size
        ArrayList<Double> score = new ArrayList<>();  //for absolute distance between windows
        ArrayList<Double> baseline = new ArrayList<>();
        ArrayList<Integer> minimas = new ArrayList<>();  //indices of the local minimas of the score array
        ArrayList<Integer> differences = new ArrayList<>();
        //get baseline
        for (int i = (samples.size() / 2) - (diff / 2); i < (samples.size() / 2) + (diff / 2); i++) {
            baseline.add(samples.get(i));
        }
        //compute absolute distance
        for (int i = 1; i < ((samples.size() / 2) - (diff / 2)) / diff; i++) {
            score.add(getAbsoluteDistance(true, i, baseline, samples, diff));
        }
        for (int i = 1; i < ((samples.size() / 2) - (diff / 2)) / diff; i++) {
            score.add(getAbsoluteDistance(false, i, baseline, samples, diff));
        }

        //compute local minimas
        for (int i = 2; i < score.size() - 2; i++) {
            if (score.get(i) < score.get(i + 1) && score.get(i) < score.get(i + 2) && score.get(i) < score.get(i - 1)
                    && score.get(i) < score.get(i - 2))
                minimas.add(i);
        }

        //difference between adjacent elements

        for (int i = 0; i < minimas.size() - 1; i += 2) {
            differences.add(Math.abs(minimas.get(i) - minimas.get(i + 1)));
        }

        //mode
        return diff * getMode(differences);

    }

    public static double getAbsoluteDistance(boolean forwards, int nth, ArrayList<Double> baseline, ArrayList<Double> samples, int diff) {
        //To check whether we're moving forwards or backwards with the samples
        double sum = 0;
        if (forwards) {
            //if moving forwards
            int currentStartingIndex = (samples.size() / 2) - (diff / 2) + diff * nth;
            for (int i = 0; i < diff; i++) {
                sum += Math.abs((baseline.get(i) - samples.get(currentStartingIndex + i)));
            }
        } else {
            //if moving backwards
            int currentStartingIndex = (samples.size() / 2) - (diff / 2) - diff * nth;
            for (int i = 0; i < diff; i++) {
                sum += Math.abs((baseline.get(i) - samples.get(currentStartingIndex + i)));
            }
        }
        return sum;
    }

    public static int getMode(ArrayList<Integer> differences) {
        int mode = 0;
        int maxCount = 0;
        for (int i = 0; i < differences.size(); i++) {
            int count = 0;
            for (int j = 0; j < differences.size(); j++) {
                if (differences.get(i).equals(differences.get(j)))
                    count++;
            }
            if (count > maxCount) {
                maxCount = count;
                mode = differences.get(i);
            }
        }
        if (maxCount > 1) {
            return mode;
        }
        int avg = 0;
        for (int i = 0; i < differences.size(); i++) {
            avg += differences.get(i);
        }
        avg /= differences.size();
        return avg;
    }
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        Sensor mySensor = sensorEvent.sensor;

        if (mySensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION && !accFinished) {

            filteredAcceleration = lowPass(sensorEvent.values.clone(), filteredAcceleration);
            time.add(sensorEvent.timestamp);
            magnitude = Math.sqrt(filteredAcceleration[0] * filteredAcceleration[0] + filteredAcceleration[1] * filteredAcceleration[1] + filteredAcceleration[2] * filteredAcceleration[2]);
            samples.add(magnitude);
            i++;

        } else if (flag == 0) {  //register
//            samples = interpolate(samples);
            sensorManager.unregisterListener(this);
            s = i;
            length = estimateCycleLength(samples);
            samples.subList(0, length).clear();
            ArrayList<Double> newSamples = skipIrregularCycles(samples, cycleStarts(samples));
            final double templateDistance = average(distances(newSamples, cycleStarts(newSamples)));
            System.out.println(templateDistance);
            final ArrayList<Double> Template = cycle(newSamples, cycleStarts(newSamples));
            System.out.println(Template.size());
            if (Double.isNaN(templateDistance)) {
                ((TextView) findViewById(R.id.speed)).setText("Sorry. please register again, and walk properly this time");
                return;
            }
            ((TextView) findViewById(R.id.speed)).setText("\n" +"registered"+ "\n" + "samples taken :" + i);
//            SharedPreferences sharedPref = getSharedPreferences("mypref", 0);
//            SharedPreferences.Editor editor = sharedPref.edit();
//            editor.putString("name", name);
//            putDouble(editor, "DTW", templateDistance);
            String json = new Gson().toJson(Template);
//            editor.putString("Template", json);
//            editor.apply();

            Gait gait = new Gait();
            gait.setDistance(templateDistance);
            gait.setName(name);
            gait.setTemplate(json);
            GaitDatabase.getDatabase(getApplicationContext()).daoAccess().insertGait(gait);
            GraphView graph = (GraphView) findViewById(R.id.r1);
            LineGraphSeries<DataPoint> series = new LineGraphSeries<>();
            for (int i = 0; i < samples.size(); i++) {
                series.appendData(new DataPoint(i, samples.get(i)), true, samples.size());
            }
            graph.addSeries(series);
            graph = (GraphView) findViewById(R.id.r2);
            series = new LineGraphSeries<>();
            for (int i = 0; i < Template.size(); i++) {
                series.appendData(new DataPoint(i, Template.get(i)), true, Template.size());
            }
            graph.addSeries(series);
            makeSound();

        } else if (flag == 1) {  //login
//            samples = interpolate(samples);
            sensorManager.unregisterListener(this);
            length = estimateCycleLength(samples);
            samples.subList(0, length).clear();
            ArrayList<Double> newSamples = skipIrregularCycles(samples, cycleStarts(samples));
            ArrayList<Double> template = cycle(newSamples, cycleStarts(newSamples));
            System.out.println(template.size());
            if (newSamples.isEmpty()) {
                ((TextView) findViewById(R.id.speed)).setText("Sorry. please login again, and walk properly this time");
                return;
            }
//            SharedPreferences sharedPref = getSharedPreferences("mypref", 0);
//            String savedName = sharedPref.getString("name", "");
//            String json = sharedPref.getString("Template", null);
//            ArrayList<Double> template2 = gson.fromJson(json, type);
//             double templateDistance = getDouble(sharedPref, "DTW", 0);
            Gait gait = GaitDatabase.getDatabase(getApplicationContext()).daoAccess().fetchByName(name);
            if (gait == null){
                ((TextView) findViewById(R.id.speed)).setText("User does not exist");
                return;
            }
            double templateDistance = gait.getDistance();
            String json = gait.getTemplate();
            Gson gson = new Gson();
            Type type = new TypeToken<ArrayList<Double>>() {
            }.getType();
            ArrayList<Double> template2 = gson.fromJson(json, type);
             double ratio = DTW(template2, template) / templateDistance;
            System.out.println("dtw " + DTW(template2, template));
            if (ratio<1.4) {
                ((TextView) findViewById(R.id.speed)).setText("\n"+"Login Successful"+"\n"+"Ratio: "+ratio);
            } else {
                ((TextView) findViewById(R.id.speed)).setText("\n"+"Login Failure"+"\n"+"Ratio: "+ratio);
            }

            GraphView graph = (GraphView) findViewById(R.id.l1);
            LineGraphSeries<DataPoint> series = new LineGraphSeries<>();
            for (int i = 0; i < samples.size(); i++) {
                series.appendData(new DataPoint(i, samples.get(i)), true, samples.size());
            }
            graph.addSeries(series);
            graph = (GraphView) findViewById(R.id.l2);
            series = new LineGraphSeries<>();
            for (int i = 0; i < template.size(); i++) {
                series.appendData(new DataPoint(i, template.get(i)), true, template.size());
            }
            graph.addSeries(series);
            makeSound();
        }
    }

    protected float[] lowPass(float[] input, float[] output) {
        float timestamp = System.nanoTime();
        float timeConstant = 0.297f;
        float dt = 1 / (i / ((timestamp - timestampOld) / 1000000000.0f));
        float alpha = timeConstant / (timeConstant + dt);
        if (output == null) return input;
        for (int i = 0; i < input.length; i++) {
            output[i] = alpha * output[i] + (1 - alpha) * input[i];
        }
        return output;
    }

    public void makeSound() {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            r.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public SharedPreferences.Editor putDouble(final SharedPreferences.Editor edit, final String key, final double value) {
        return edit.putLong(key, Double.doubleToRawLongBits(value));
    }

    public double getDouble(final SharedPreferences prefs, final String key, final double defaultValue) {
        return Double.longBitsToDouble(prefs.getLong(key, Double.doubleToLongBits(defaultValue)));
    }

    // start points of cycles
    public ArrayList<Integer> cycleStarts(ArrayList<Double> samples) {
        double offset = 0.2;
        int start = minAtCenter(samples, length);
        ArrayList<Integer> indices = new ArrayList<>();
        indices.add(start);
        double min;
        int index;
        for (int i = 0; i < ((samples.size() / 2) / length); i++) {
            start -= length;
            index = start;
            try {
                min = samples.get(index);
            } catch (IndexOutOfBoundsException e) {
                break;
            }
            for (int j = (int) (start - offset * length); j < start + offset * length; j++) {
                try {
                    if (samples.get(j) < min) {
                        min = samples.get(j);
                        index = j;
                    }
                } catch (IndexOutOfBoundsException e) {
                    break;
                }
            }
            start = index;
            indices.add(index);
        }

        Collections.reverse(indices);
        start = minAtCenter(samples, length);
        for (int i = 0; i < ((samples.size() / 2) / length); i++) {
            start += length;
            index = start;
            try {
                min = samples.get(index);
            } catch (IndexOutOfBoundsException e) {
                break;
            }
            for (int j = (int) (start - offset * length); j < start + offset * length; j++) {
                try {
                    if (samples.get(j) < min) {
                        min = samples.get(j);
                        index = j;
                    }
                } catch (IndexOutOfBoundsException e) {
                    break;
                }
            }
            start = index;
            indices.add(index);
        }
        System.out.println("starts indices " + indices);
        return indices;
    }

    public int minAtCenter(ArrayList<Double> samples, int length) {
        double min = samples.get(samples.size() / 2);
        int index = 0;
        for (int i = (samples.size() / 2) - length; i < (samples.size() / 2) + length; i++) {
            if (samples.get(i) < min) {
                index = i;
                min = samples.get(i);
            }
        }
        return index;
    }


    // remove unusual cycles
    public ArrayList<Double> skipIrregularCycles(ArrayList<Double> samples, ArrayList<Integer> indices) {
        ArrayList<Double> averages = distances(samples, indices);
        System.out.println("after first distances: " + averages);
        double diffAverage = average(averages);
        //This function returns all the indices which are to be omitted from further calculations
        return removeFromSamples(samples, indices, averages, diffAverage);
    }

    public ArrayList<Double> distances(ArrayList<Double> samples, ArrayList<Integer> indices) {
        ArrayList<Double> averages = new ArrayList<>();
        for (int index : indices) {
            if (index == indices.get(indices.size() - 1))
                break;
            ArrayList<Double> window0 = new ArrayList<>(samples.subList(index, indices.get(indices.indexOf(index) + 1)));
            ArrayList<Double> differences = new ArrayList<>();
            for (int _index : indices) {
                //We don't want to compare it to itself
                if (!(_index == index) && !(_index == indices.get(indices.size() - 1))) {

                    ArrayList<Double> window1 = new ArrayList<>(samples.subList(_index, indices.get(indices.indexOf(_index) + 1)));
                    differences.add(DTW(window0, window1));
                }
            }
            averages.add(average(differences));
        }
        if (averages.isEmpty()) {
            System.out.println("EMPTY");
        }
        return averages;
    }


    public double average(ArrayList<Double> doubles) {
        double sum = 0;
        for (double d : doubles)
            sum += d;
        return sum / doubles.size();
    }

    public ArrayList<Double> removeFromSamples(ArrayList<Double> samples, ArrayList<Integer> indices, ArrayList<Double> averages, double average) {
        ArrayList<Integer> removedIndices = new ArrayList<>();
        ArrayList<Integer> list = new ArrayList<>();
        for (int i = 0; i < indices.size() - 1; i++) {
            if (averages.get(i) < average * 0.8 || averages.get(i) > average * 1.2) {
                removedIndices.add(i);
            }
        }
        if (removedIndices.size() > 0 && indices.size() > 3) {
            for (int i = 0; i < removedIndices.size(); i++) {
                for (int j = indices.get(removedIndices.get(i)); j < indices.get(removedIndices.get(i) + 1); j++) {
                    list.add(j);
                }
            }
            Collections.sort(list, Collections.reverseOrder());
            for (int i : list)
                samples.remove(i);
            indices = cycleStarts(samples);
            averages = distances(samples, indices);
            System.out.println("after samples removed" + averages);
            System.out.println("cycle starts" + indices);

            return removeFromSamples(samples, indices, averages, average(averages));

        } else
            return samples;
    }


    public ArrayList<Double> cycle(ArrayList<Double> samples, ArrayList<Integer> indices) {
        ArrayList<Double> cycle = new ArrayList<>();
        ArrayList<Double> averages = distances(samples, indices);
        int minIndex = averages.indexOf(Collections.min(averages));
        for (int i = indices.get(minIndex); i < indices.get(minIndex + 1); i++) {
            cycle.add(samples.get(i));
        }
        return cycle;
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        final Button register = findViewById(R.id.start);
        register.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                register();
            }
        });


        final Button login = findViewById(R.id.stop);
        login.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                login();
            }
        });
    }


    private void register() {
        time = new ArrayList<>();
        samples = new ArrayList<>();
        accFinished = false;
        filteredAcceleration = new float[3];
        i = 0;
        flag = 0;
        EditText edittext = findViewById(R.id.name);
        name = edittext.getText().toString();
        ((EditText) findViewById(R.id.name)).setText("");

        try {
            //sleep 2 seconds
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    sleep(30000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                accFinished = true;

            }

        }.start();
        timestampOld = System.nanoTime();
        sensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        ((TextView) findViewById(R.id.speed)).setText("Recording...");

    }

    private void login() {
        time = new ArrayList<>();
        samples = new ArrayList<>();
        accFinished = false;
        filteredAcceleration = new float[3];
        i = 0;
        flag = 1;
        EditText edittext = findViewById(R.id.name);
        String s = edittext.getText().toString();
        name = s;
        ((EditText) findViewById(R.id.name)).setText("");
        try {
            //sleep 2 seconds
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    sleep(30000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                accFinished = true;
            }

        }.start();
        timestampOld = System.nanoTime();
        sensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        ((TextView) findViewById(R.id.speed)).setText("Recording...");

    }

    protected void onPause(Bundle savedInstanceState) {
        super.onPause();
        sensorManager.unregisterListener(this);
    }
    protected void onResume(Bundle savedInstanceState) {
        super.onResume();
        sensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
    }

}