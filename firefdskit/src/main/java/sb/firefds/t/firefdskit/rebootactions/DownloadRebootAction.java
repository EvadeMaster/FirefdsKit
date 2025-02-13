/*
 * Copyright (C) 2022 Shauli Bracha for FirefdsKit Project (firefds@xda)
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
package sb.firefds.t.firefdskit.rebootactions;

import android.content.Context;

import sb.firefds.t.firefdskit.utils.Utils;

import static sb.firefds.t.firefdskit.utils.Constants.DOWNLOAD_ACTION;

public class DownloadRebootAction implements RebootAction {

    @Override
    public void reboot(Context context) {
        Utils.rebootEPM(context, DOWNLOAD_ACTION);
    }
}
