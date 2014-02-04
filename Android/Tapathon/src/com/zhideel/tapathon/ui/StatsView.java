package com.zhideel.tapathon.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.squareup.otto.Bus;
import com.zhideel.tapathon.Config;
import com.zhideel.tapathon.R;
import com.zhideel.tapathon.Stopwatch;
import com.zhideel.tapathon.logic.CommunicationBus;
import com.zhideel.tapathon.logic.GameLogicController;

import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Adeel on 15/11/13.
 */
public class StatsView implements CommunicationBus.BusManager {
    private Activity mContext;
    private final Bus mBus;
    private boolean isPaused;
    private ArrayList<Float> operands;
    private String operator;
    public TextView tvScore, tvQuestion, tvTimer;
    private Stopwatch stopwatch;
    Timer timerTask;
    private Random rand = new Random();
    private int randomQuestion, correctAnswerCount, totalQuestions;
    public int time = 60;
    private boolean isFirstQuestionAsked = false;

    public StatsView(Context context, ViewGroup viewGroup) {
        mContext = (Activity) context;
        this.mBus = CommunicationBus.getInstance();
        View.inflate(mContext, R.layout.view_stats, viewGroup);
        isPaused = true;
        operands = new ArrayList<Float>();
        tvScore = (TextView) viewGroup.findViewById(R.id.tv_multipler);
        tvQuestion = (TextView) viewGroup.findViewById(R.id.tv_qns);
        tvTimer = (TextView) viewGroup.findViewById(R.id.tv_timer);

      /*  Typeface fontFace = Typeface.createFromAsset(Config.context.getAssets(), "Crayon.ttf");
        Typeface face = Typeface.create(fontFace, Typeface.BOLD);

        tvTimer.setTypeface(face);
        tvTimer.setTextSize(60);
        tvQuestion.setTypeface(face);
        tvQuestion.setTextSize(60);
        tvScore.setTypeface(face);
        tvScore.setTextSize(60);*/

        correctAnswerCount = 0;
        totalQuestions = 0;
        tvScore.setText("0");
        tvTimer.setText(Integer.toString(time));
        newQuestion();
    }

    public void setTime(int time) {
        this.time = time;
        tvTimer.setText(Integer.toString(time));
    }

    public int getTime() {
        return time;
    }

    private void timer() {
        timerTask = new Timer();
        timerTask.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (isPaused == false) {
                    mContext.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (StatsView.this.time != 0) {

                                StatsView.this.time--;
                                setTime(StatsView.this.time);
                                if (stopwatch.elapsed() >= PadView.maxNextQuestionDelay) {
                                    ((GamePadActivity) mContext).flashNextQuestionView();
                                    newQuestion();
                                }
                            } else {
                                ((GamePadActivity) mContext).getGameBoard().setPaused(true);
                                timerTask.cancel();
                                ((GamePadActivity) mContext).showGameEndView();
                                mBus.post(GameLogicController.EndGameEvent.INSTANCE);
                            }
                        }

                    });
                }
            }
        }, 0, 1000);
    }

    public int randInt(int min, int max) {
        int randomNum = rand.nextInt((max - min) + 1) + min;
        return randomNum;
    }

    public void addOperand(Float operand) {
        if (operands.size() < 2) {
            operands.add(operand);
        }
    }

    public ArrayList<Float> getOperands() {
        return operands;
    }

    public void setOperator(String operator) {
        if (this.operator == null) {
            this.operator = operator;
        }
    }

    public String getOperator() {
        return this.operator;
    }

    public void doCalc() {
        float op1 = operands.get(0);
        float op2 = operands.get(1);
        float result;
        try {
            Log.d(StatsView.class.getName(), "Trying to calculate: " + operands.get(0) + " " + operator + " " + operands.get(1));
            if (operator.equalsIgnoreCase("X")) {
                result = op1 * op2;
            } else if (operator.equalsIgnoreCase("/")) {
                result = op1 / op2;
            } else if (operator.equalsIgnoreCase("-")) {
                result = op1 - op2;
            } else {
                result = op1 + op2;
            }
        } catch (ArithmeticException e) {
            result = 0.0f;
        }

        //If answered correctly
        if (((int) result) == randomQuestion) {
            congratulate();
            int currentScore = Integer.parseInt(tvScore.getText().toString());
            int elapsedTime = stopwatch.elapsed();
            int reward = Math.round(((((float) PadView.maxNextQuestionDelay - elapsedTime) / PadView.maxNextQuestionDelay * 100) + ((float) correctAnswerCount / totalQuestions * 100)) / 200 * 100);
            currentScore = Integer.valueOf(Math.round(((float) currentScore * totalQuestions + reward) / ((totalQuestions + 1) * 100) * 100));
            tvScore.setText(Integer.toString(currentScore));
            correctAnswerCount++;
            newQuestion();
        } else {
            criticize();
        }
    }

    private void congratulate() {
        MediaPlayer mp = MediaPlayer.create(mContext, R.raw.correct_answer);
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.release();
            }

        });
        mp.start();
        Vibrator v = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);

        // Start without a delay
        // Each element then alternates between vibrate, sleep, vibrate, sleep...
        long[] pattern = {0, 150, 150, 500};

        // The '-1' here means to vibrate once
        // '0' would make the pattern vibrate indefinitely
        v.vibrate(pattern, -1);

        ((GamePadActivity) mContext).flashCorrectAnswerView();
    }

    private void criticize() {
        MediaPlayer mp = MediaPlayer.create(mContext, R.raw.wrong_answer);
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.release();
            }

        });
        mp.start();
        Vibrator v = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
        // Vibrate for 500 milliseconds
        v.vibrate(500);
        ((GamePadActivity) mContext).flashWrongAnswerView();
    }

    public void newQuestion() {
        operands.clear();
        operator = null;
        randomQuestion = randInt(1, getMaxQuestionLimit());
        tvQuestion.setText(Integer.toString(randomQuestion));
        stopwatch = Stopwatch.start();
        totalQuestions++;
        if (!isFirstQuestionAsked) {
            isFirstQuestionAsked = true;
        } else {
            ((GamePadActivity) mContext).getGameBoard().resetBoard();
        }
    }

    private int getMaxQuestionLimit() {
        if (PadView.selectedLevel == PadView.GameLevel.MEDIUM) {
            return 35;
        } else if (PadView.selectedLevel == PadView.GameLevel.HARD) {
            return 50;
        }

        return 20;
    }

    public void setPaused(Boolean paused) {
        this.isPaused = paused;
        if (isPaused == false) {
            timer();
        } else {
            timerTask.cancel();
        }
    }

    @Override
    public void startBus() {
        mBus.register(this);
    }

    @Override
    public void stopBus() {
        mBus.unregister(this);
    }
}
