/*
 * Copyright (C) 2022 Shauli Bracha for Firefds Kit Project (Firefds@xda)
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
package sb.firefds.t.firefdskit.actionViewModels;

import android.hardware.input.InputManager;
import android.os.SystemClock;
import android.view.KeyEvent;

import androidx.core.content.res.ResourcesCompat;

import de.robv.android.xposed.XposedHelpers;
import sb.firefds.t.firefdskit.R;

import static sb.firefds.t.firefdskit.XSysUIGlobalActions.getResources;
import static sb.firefds.t.firefdskit.utils.Constants.SCREENSHOT_ACTION;

public class ScreenShotActionViewModel extends FirefdsKitActionViewModel {

    ScreenShotActionViewModel() {

        super();
        getActionInfo().setName(SCREENSHOT_ACTION);
        getActionInfo().setLabel(getResources().getString(R.string.screenshot));
        setDrawableIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.tw_ic_do_screenshot_stock, null));
    }

    @Override
    public void onPress() {

        getGlobalActions().dismissDialog(false);
        takeScreenshot();
    }

    @Override
    public void onPressSecureConfirm() {
        takeScreenshot();
    }

    private void takeScreenshot() {
        int[] arrayOfInt = new int[2];
        arrayOfInt[1] = 1;
        for (int k : arrayOfInt) {
            long l = SystemClock.uptimeMillis();
            final InputManager inputManager = (InputManager) XposedHelpers.callStaticMethod(InputManager.class,
                    "getInstance");
            XposedHelpers.callMethod(inputManager,
                    "injectInputEvent",
                    new KeyEvent(l, l, k, 120, 0, 0, -1, 0, 268435464, 257), 0);
        }
    }
}
