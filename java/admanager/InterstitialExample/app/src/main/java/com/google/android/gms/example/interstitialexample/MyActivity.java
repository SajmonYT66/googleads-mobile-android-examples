/*
 * Copyright (C) 2013 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.gms.example.interstitialexample;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.CountDownTimer;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.admanager.AdManagerAdRequest;
import com.google.android.gms.ads.admanager.AdManagerInterstitialAd;
import com.google.android.gms.ads.admanager.AdManagerInterstitialAdLoadCallback;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;

/**
 * Main Activity. Inflates main activity xml.
 */
@SuppressLint("SetTextI18n")
public class MyActivity extends AppCompatActivity {

    private static final long GAME_LENGTH_MILLISECONDS = 3000;
    private static final String AD_UNIT_ID = "/6499/example/interstitial";
    private static final String TAG = "MyActivity";

    private AdManagerInterstitialAd interstitialAd;
    private CountDownTimer countDownTimer;
    private Button retryButton;
    private boolean gameIsInProgress;
    private boolean adIsLoading;
    private long timerMilliseconds;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        // Log the Mobile Ads SDK version.
        Log.d(TAG, "Google Mobile Ads SDK Version: " + MobileAds.getVersion());

        // Initialize the Mobile Ads SDK.
        MobileAds.initialize(
            this,
            new OnInitializationCompleteListener() {
              @Override
              public void onInitializationComplete(InitializationStatus status) {}
            });

    loadAd();

        // Create the "retry" button, which tries to show an interstitial between game plays.
        retryButton = findViewById(R.id.retry_button);
        retryButton.setVisibility(View.INVISIBLE);
        retryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showInterstitial();
            }
        });

        startGame();
    }

    private void createTimer(final long milliseconds) {
        // Create the game timer, which counts down to the end of the level
        // and shows the "retry" button.
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        final TextView textView = findViewById(R.id.timer);

        countDownTimer = new CountDownTimer(milliseconds, 50) {
            @Override
            public void onTick(long millisUnitFinished) {
                timerMilliseconds = millisUnitFinished;
                textView.setText("seconds remaining: " + ((millisUnitFinished / 1000) + 1));
            }

            @Override
            public void onFinish() {
                gameIsInProgress = false;
                textView.setText("done!");
                retryButton.setVisibility(View.VISIBLE);
            }
        };
    }

    @Override
    public void onResume() {
        // Start or resume the game.
        super.onResume();

        if (gameIsInProgress) {
            resumeGame(timerMilliseconds);
        }
    }

    @Override
    public void onPause() {
        // Cancel the timer if the game is paused.
        countDownTimer.cancel();
        super.onPause();
    }

  public void loadAd() {
    AdManagerAdRequest adRequest = new AdManagerAdRequest.Builder().build();
    AdManagerInterstitialAd.load(
        this,
        AD_UNIT_ID,
        adRequest,
        new AdManagerInterstitialAdLoadCallback() {
          @Override
          public void onAdLoaded(@NonNull AdManagerInterstitialAd interstitialAd) {
            // The mInterstitialAd reference will be null until
            // an ad is loaded.
            MyActivity.this.interstitialAd = interstitialAd;
            Log.i(TAG, "onAdLoaded");
            Toast.makeText(MyActivity.this, "onAdLoaded()", Toast.LENGTH_SHORT).show();
            interstitialAd.setFullScreenContentCallback(
                new FullScreenContentCallback() {
                  @Override
                  public void onAdDismissedFullScreenContent() {
                    // Called when fullscreen content is dismissed.
                    Log.d("TAG", "The ad was dismissed.");
                    // Make sure to set your reference to null so you don't
                    // show it a second time.
                    MyActivity.this.interstitialAd = null;
                  }

                  @Override
                  public void onAdFailedToShowFullScreenContent(AdError adError) {
                    // Called when fullscreen content failed to show.
                    Log.d("TAG", "The ad failed to show.");
                    // Make sure to set your reference to null so you don't
                    // show it a second time.
                    MyActivity.this.interstitialAd = null;
                  }

                  @Override
                  public void onAdShowedFullScreenContent() {
                    // Called when fullscreen content is shown.
                    Log.d("TAG", "The ad was shown.");
                  }
                });
          }

          @Override
          public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
            // Handle the error
            Log.i(TAG, loadAdError.getMessage());
            interstitialAd = null;
            adIsLoading = false;
            String error =
                String.format(
                    "domain: %s, code: %d, message: %s",
                    loadAdError.getDomain(), loadAdError.getCode(), loadAdError.getMessage());
            Toast.makeText(
                    MyActivity.this, "onAdFailedToLoad() with error: " + error, Toast.LENGTH_SHORT)
                .show();
          }
        });
  }

    private void showInterstitial() {
    // Show the ad if it's ready. Otherwise toast and restart the game.
    if (interstitialAd != null) {
      interstitialAd.show(this);

        } else {
            Toast.makeText(this, "Ad did not load", Toast.LENGTH_SHORT).show();
            startGame();
        }
    }

    private void startGame() {
    // Request a new ad if one isn't already loaded, hide the button, and kick off the timer.
    if (!adIsLoading) {
            adIsLoading = true;
      loadAd();
        }

        retryButton.setVisibility(View.INVISIBLE);
        resumeGame(GAME_LENGTH_MILLISECONDS);
    }

    private void resumeGame(long milliseconds) {
        // Create a new timer for the correct length and start it.
        gameIsInProgress = true;
        timerMilliseconds = milliseconds;
        createTimer(milliseconds);
        countDownTimer.start();
    }
}
