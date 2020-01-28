/*
 * Copyright (C) 2017 Ace Explorer owned by Siju Sakaria
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.siju.acexplorer.picker;

import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.siju.acexplorer.R;
import com.siju.acexplorer.analytics.Analytics;
import com.siju.acexplorer.storage.modules.picker.types.PickerType;
import com.siju.acexplorer.storage.modules.picker.view.PickerFragment;
import com.siju.acexplorer.theme.Theme;

import static com.siju.acexplorer.settings.SettingsPreferenceFragment.PREFS_ANALYTICS;


public class TransparentActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean sendAnalytics = PreferenceManager.getDefaultSharedPreferences(this).
                getBoolean(PREFS_ANALYTICS, true);
        Analytics.getLogger().sendAnalytics(sendAnalytics);
        Analytics.getLogger().register(this);
        Analytics.getLogger().reportDeviceName();
        Intent intent = getIntent();
        handleIntent(intent);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case RingtoneManager.ACTION_RINGTONE_PICKER:
                    showPickerDialog(intent, PickerType.RINGTONE);
                    break;
                case Intent.ACTION_GET_CONTENT:
                    showPickerDialog(intent, PickerType.FILE);
                    break;
            }
        }
    }

    private void showPickerDialog(Intent intent, PickerType pickerType) {

        PickerFragment dialogFragment = PickerFragment.Companion.newInstance(this, checkTheme(),
                pickerType,
                intent.getIntExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, 0));
        String FRAGMENT_TAG = "Browse_Frag";
        dialogFragment.show(getSupportFragmentManager(), FRAGMENT_TAG);
    }


    private int checkTheme() {
        int theme = Theme.Companion.getUserThemeValue(this);

        if (theme == Theme.DARK.getValue()) {
            return R.style.TransparentTheme_DarkAppTheme_NoActionBar;
        } else {
            return R.style.TransparentTheme_AppTheme_NoActionBar;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        finish();
    }
}
