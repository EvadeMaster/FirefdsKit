/*
 * Copyright (C) 2019 Shauli Bracha for FirefdsKit Project (firefds@xda)
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

package sb.firefds.pie.firefdskit;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;

import com.samsung.android.globalactions.presentation.SecGlobalActions;
import com.samsung.android.globalactions.presentation.SecGlobalActionsPresenter;
import com.samsung.android.globalactions.presentation.view.ResourceFactory;
import com.samsung.android.globalactions.presentation.viewmodel.ActionInfo;
import com.samsung.android.globalactions.presentation.viewmodel.ActionViewModel;
import com.samsung.android.globalactions.presentation.viewmodel.ActionViewModelFactory;
import com.samsung.android.globalactions.presentation.viewmodel.ViewType;
import com.samsung.android.globalactions.util.KeyGuardManagerWrapper;
import com.samsung.android.globalactions.util.ResourcesWrapper;
import com.samsung.android.globalactions.util.ToastController;
import com.samsung.android.globalactions.util.UtilFactory;

import java.util.HashMap;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import sb.firefds.pie.firefdskit.actionViewModels.RestartActionViewModel;
import sb.firefds.pie.firefdskit.actionViewModels.ScreenShotActionViewModel;
import sb.firefds.pie.firefdskit.utils.Packages;
import sb.firefds.pie.firefdskit.utils.Utils;

public class XSysUIGlobalActions {

    private static final int RECOVERY_RESTART_ACTION = 3;
    private static final int DOWNLOAD_RESTART_ACTION = 4;
    private static final String GLOBAL_ACTIONS_PACKAGE = "com.samsung.android.globalactions";
    private static final String SEC_GLOBAL_ACTIONS_PRESENTER =
            GLOBAL_ACTIONS_PACKAGE + ".presentation.SecGlobalActionsPresenter";
    private static final String DEFAULT_ACTION_VIEW_MODEL_FACTORY =
            GLOBAL_ACTIONS_PACKAGE + ".presentation.viewmodel.DefaultActionViewModelFactory";
    private static final String GLOBAL_ACTIONS_FEATURE_FACTORY =
            Packages.SYSTEM_UI + ".globalactions.presentation.features.GlobalActionsFeatureFactory";
    private static final String GLOBAL_ACTION_ITEM_VIEW =
            GLOBAL_ACTIONS_PACKAGE + ".presentation.view.GlobalActionItemView";
    private static SecGlobalActionsPresenter mSecGlobalActionsPresenter;
    private static Map<String, Object> actionViewModelDefaults;
    private static String mRecoveryStr;
    private static String mDownloadStr;
    private static String mScreenshotStr;
    private static Drawable mRecoveryIcon;
    private static Drawable mDownloadIcon;
    private static Drawable mScreenshotIcon;
    private static String mRebootConfirmRecoveryStr;
    private static String mRebootConfirmDownloadStr;

    public static void doHook(final XSharedPreferences prefs, final ClassLoader classLoader) {

        final Class<?> globalActionsFeatureFactoryClass =
                XposedHelpers.findClass(GLOBAL_ACTIONS_FEATURE_FACTORY, classLoader);

        if (prefs.getBoolean("enableAdvancedPowerMenu", false)) {
            try {
                XposedBridge.hookAllConstructors(globalActionsFeatureFactoryClass,
                        new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                                Context ctx = (Context) param.args[0];
                                Resources res = ctx.getResources();
                                Context gbContext = Utils.getGbContext(ctx, res.getConfiguration());

                                mRecoveryStr = gbContext.getString(R.string.reboot_recovery);
                                mDownloadStr = gbContext.getString(R.string.reboot_download);
                                mScreenshotStr = gbContext.getString(R.string.screenshot);

                                mRecoveryIcon = gbContext
                                        .getDrawable(R.drawable.tw_ic_do_recovery_stock);
                                mDownloadIcon = gbContext
                                        .getDrawable(R.drawable.tw_ic_do_download_stock);
                                mScreenshotIcon = gbContext
                                        .getDrawable(R.drawable.tw_ic_do_screenshot_stock);

                                mRebootConfirmRecoveryStr = gbContext
                                        .getString(R.string.reboot_confirm_recovery);
                                mRebootConfirmDownloadStr = gbContext
                                        .getString(R.string.reboot_confirm_download);

                            }
                        });

                XposedHelpers.findAndHookMethod(SEC_GLOBAL_ACTIONS_PRESENTER,
                        classLoader,
                        "createDefaultActions",
                        new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                ActionViewModelFactory actionViewModelFactory =
                                        (ActionViewModelFactory) XposedHelpers
                                                .getObjectField(param.thisObject,
                                                        "mViewModelFactory");
                                mSecGlobalActionsPresenter =
                                        (SecGlobalActionsPresenter) param.thisObject;
                                mSecGlobalActionsPresenter
                                        .addAction(actionViewModelFactory.createActionViewModel(
                                                (SecGlobalActionsPresenter) param.thisObject,
                                                "recovery"));
                                mSecGlobalActionsPresenter
                                        .addAction(actionViewModelFactory.createActionViewModel(
                                                (SecGlobalActionsPresenter) param.thisObject,
                                                "download"));
                                if (prefs.getBoolean("enableDataMode", false)) {
                                    mSecGlobalActionsPresenter
                                            .addAction(actionViewModelFactory.createActionViewModel(
                                                    (SecGlobalActionsPresenter) param.thisObject,
                                                    "data_mode"));
                                }
                                if (prefs.getBoolean("enableScreenshot", false)) {
                                    mSecGlobalActionsPresenter
                                            .addAction(actionViewModelFactory.createActionViewModel(
                                                    (SecGlobalActionsPresenter) param.thisObject,
                                                    "screenshot"));
                                }
                            }
                        });

                XposedHelpers.findAndHookMethod(DEFAULT_ACTION_VIEW_MODEL_FACTORY,
                        classLoader,
                        "createActionViewModel",
                        SecGlobalActions.class,
                        String.class,
                        new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                setActionViewModelDefaults(param);
                                RestartActionViewModel restartActionViewModel;
                                switch ((String) param.args[1]) {
                                    case ("recovery"):
                                        restartActionViewModel = setRestartActionViewModel("recovery",
                                                mRecoveryStr,
                                                mRebootConfirmRecoveryStr,
                                                RECOVERY_RESTART_ACTION);
                                        param.setResult(restartActionViewModel);
                                        break;
                                    case ("download"):
                                        restartActionViewModel = setRestartActionViewModel("download",
                                                mDownloadStr,
                                                mRebootConfirmDownloadStr,
                                                DOWNLOAD_RESTART_ACTION);
                                        param.setResult(restartActionViewModel);
                                        break;
                                    case ("screenshot"):
                                        ScreenShotActionViewModel screenShotActionView =
                                                setScreenShotActionViewModel();
                                        param.setResult(screenShotActionView);
                                        break;
                                }
                            }
                        });

                XposedHelpers.findAndHookMethod(GLOBAL_ACTION_ITEM_VIEW,
                        classLoader,
                        "setViewAttrs",
                        View.class,
                        boolean.class,
                        new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                ActionViewModel actionViewModel = (ActionViewModel) XposedHelpers.
                                        getObjectField(param.thisObject, "mViewModel");
                                ResourceFactory resourceFactory = (ResourceFactory) XposedHelpers
                                        .getObjectField(param.thisObject, "mResourceFactory");
                                ImageView localImageView = ((View) param.args[0])
                                        .findViewById(resourceFactory
                                                .getResourceID("sec_global_actions_icon"));
                                switch (actionViewModel.getActionInfo().getName()) {
                                    case "recovery":
                                        localImageView.setImageDrawable(mRecoveryIcon);
                                        break;
                                    case "download":
                                        localImageView.setImageDrawable(mDownloadIcon);
                                        break;
                                    case "screenshot":
                                        localImageView.setImageDrawable(mScreenshotIcon);
                                        break;
                                }
                            }
                        });

            } catch (Throwable e) {
                XposedBridge.log(e);
            }
        }
    }

    private static RestartActionViewModel setRestartActionViewModel(String actionName,
                                                                    String actionLabel,
                                                                    String actionDescription,
                                                                    int rebootAction) {

        RestartActionViewModel restartActionViewModel =
                new RestartActionViewModel(actionViewModelDefaults, rebootAction);
        ActionInfo actionInfo = setActionInfo(actionName,
                actionLabel,
                actionDescription);
        XposedHelpers.callMethod(restartActionViewModel,
                "setActionInfo",
                actionInfo);
        return restartActionViewModel;
    }

    private static ScreenShotActionViewModel setScreenShotActionViewModel() {
        ScreenShotActionViewModel screenShotActionViewModel =
                new ScreenShotActionViewModel(actionViewModelDefaults);
        ActionInfo actionInfo = setActionInfo("screenshot",
                mScreenshotStr,
                null);
        screenShotActionViewModel.setActionInfo(actionInfo);
        return screenShotActionViewModel;
    }

    private static void setActionViewModelDefaults(XC_MethodHook.MethodHookParam param) throws Throwable {
        Map<String, Object> actionViewModelDefaults = new HashMap<>();

        UtilFactory mUtilFactory = (UtilFactory) XposedHelpers.getObjectField(param.thisObject, "mUtilFactory");
        KeyGuardManagerWrapper mKeyGuardManagerWrapper =
                (KeyGuardManagerWrapper) XposedHelpers.callMethod(mUtilFactory,
                        "get",
                        KeyGuardManagerWrapper.class);

        actionViewModelDefaults.put("mContext",
                XposedHelpers.getObjectField(mKeyGuardManagerWrapper, "mContext"));
        actionViewModelDefaults.put("mSecGlobalActionsPresenter",
                mSecGlobalActionsPresenter);
        actionViewModelDefaults.put("mConditionChecker",
                XposedHelpers.getObjectField(param.thisObject, "mConditionChecker"));
        actionViewModelDefaults.put("mFeatureFactory",
                XposedHelpers.getObjectField(param.thisObject, "mFeatureFactory"));
        actionViewModelDefaults.put("ToastController",
                XposedHelpers.callMethod(mUtilFactory, "get", ToastController.class));
        actionViewModelDefaults.put("mKeyGuardManagerWrapper",
                mKeyGuardManagerWrapper);
        actionViewModelDefaults.put("ResourcesWrapper",
                XposedHelpers.callMethod(mUtilFactory, "get", ResourcesWrapper.class));
        actionViewModelDefaults.put("mUtilFactory",
                mUtilFactory);

        XSysUIGlobalActions.actionViewModelDefaults = actionViewModelDefaults;
    }

    private static ActionInfo setActionInfo(String actionName,
                                            String actionLabel,
                                            String actionDescription) {
        ActionInfo actionInfo = new ActionInfo();
        actionInfo.setName(actionName);
        actionInfo.setLabel(actionLabel);
        actionInfo.setDescription(actionDescription);
        actionInfo.setViewType(ViewType.CENTER_ICON_3P_VIEW);
        return actionInfo;
    }
}