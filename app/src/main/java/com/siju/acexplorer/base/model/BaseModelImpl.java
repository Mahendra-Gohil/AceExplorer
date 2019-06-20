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

package com.siju.acexplorer.base.model;

import android.content.Context;

import com.siju.acexplorer.AceApplication;
import com.siju.acexplorer.theme.Theme;


public class BaseModelImpl implements BaseModel {

    private Context context;

    public BaseModelImpl() {
        context = AceApplication.Companion.getAppContext();
    }


    @Override
    public Theme getTheme() {
        return Theme.Companion.getTheme(context);
    }
}
