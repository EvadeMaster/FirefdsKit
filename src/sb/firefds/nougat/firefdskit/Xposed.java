/*
 * Copyright (C) 2016 Mohamed Karami for XTouchWiz Project (Wanam@xda)
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
package sb.firefds.nougat.firefdskit;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import sb.firefds.nougat.firefdskit.utils.Packages;
import sb.firefds.nougat.firefdskit.utils.Utils;

public class Xposed implements IXposedHookZygoteInit, IXposedHookInitPackageResources, IXposedHookLoadPackage {

	private static String MODULE_PATH = null;
	private static XSharedPreferences prefs;

	@Override
	public void initZygote(StartupParam startupParam) {

		// Do not load if Not a Touchwiz Rom
		if (!Utils.isSamsungRom())
			return;

		MODULE_PATH = startupParam.modulePath;

		if (prefs == null) {
			try {
				prefs = new XSharedPreferences(Packages.FIREFDSKIT, XTouchWizActivity.class.getSimpleName());
			} catch (Throwable e) {
				XposedBridge.log(e);
			}
		}

	}

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {

		// Do not load if Not a Touchwiz Rom
		if (!Utils.isSamsungRom())
			return;

		if (lpparam.packageName.equals(Packages.FIREFDSKIT)) {
			if (prefs != null) {
				try {
					XposedHelpers.findAndHookMethod(Packages.FIREFDSKIT + ".XposedChecker", lpparam.classLoader,
							"isActive", XC_MethodReplacement.returnConstant(Boolean.TRUE));
				} catch (Throwable t) {
					XposedBridge.log(t);
				}
			} else {
				XposedBridge.log("Xposed cannot read XTouchWiz preferences!");
			}
		}

		if (lpparam.packageName.equals(Packages.ANDROID)) {

			try {
				XPM23.initZygote(prefs, lpparam.classLoader);
			} catch (Exception e1) {
				XposedBridge.log(e1);
			}

			try {

				XSystemWide.doHook(MODULE_PATH, prefs, lpparam.classLoader);

			} catch (Throwable e) {
				XposedBridge.log(e.toString());

			}

			try {
				XAndroidPackage.doHook(prefs, lpparam.classLoader);
			} catch (Throwable e) {
				XposedBridge.log(e.toString());

			}

			try {
				XFrameworkWidgetPackage.doHook(prefs, lpparam.classLoader);
			} catch (Throwable e) {
				XposedBridge.log(e.toString());

			}

			/*
			try {
				XGlobalActions.init(prefs, lpparam.classLoader);
			} catch (Throwable e) {
				XposedBridge.log(e.toString());

			}
			*/
		}

		if (lpparam.packageName.equals(Packages.SAMSUNG_INCALLUI)) {
			try {
				XInCallUIPackage.doHook(prefs, lpparam.classLoader);
			} catch (Throwable e) {
				XposedBridge.log(e.toString());

			}
		}

		if (lpparam.packageName.equals(Packages.NFC)) {
			try {
				XNfcPackage.doHook(prefs, lpparam.classLoader);
			} catch (Throwable e) {
				XposedBridge.log(e.toString());

			}
		}

		if (lpparam.packageName.equals(Packages.SYSTEM_UI)) {
			try {
				XSysUIPackage.doHook(prefs, lpparam.classLoader);
			} catch (Throwable e) {
				XposedBridge.log(e.toString());

			}
		}

		if (lpparam.packageName.equals(Packages.SETTINGS)) {
			try {
				XSecSettingsPackage.doHook(prefs, lpparam.classLoader);
			} catch (Throwable e) {
				XposedBridge.log(e.toString());

			}
		}

		if (lpparam.packageName.equals(Packages.EMAIL)) {
			try {
				XSecEmailPackage.doHook(prefs, lpparam.classLoader);
			} catch (Throwable e) {
				XposedBridge.log(e.toString());

			}
		}

		if (lpparam.packageName.equals(Packages.CAMERA)) {
			try {
				XSecCameraPackage.doHook(prefs, lpparam.classLoader);
			} catch (Throwable e) {
				XposedBridge.log(e.toString());

			}
		}

		if (lpparam.packageName.equals(Packages.SYS_SCOPE)) {
			try {
				XSysScopePackage.doHook(prefs, lpparam.classLoader);
			} catch (Throwable e) {
				XposedBridge.log(e.toString());

			}
		}

		if (lpparam.packageName.equals(Packages.TOUCHWIZ)) {

			try {
				XTouchwizLauncher.doHook(prefs, lpparam.classLoader);
			} catch (Exception e1) {
				XposedBridge.log(e1);
			}

		}
	}

	@Override
	public void handleInitPackageResources(InitPackageResourcesParam resparam) throws Throwable {

		// Do not load if Not a Touchwiz Rom
		if (!Utils.isSamsungRom())
			return;

		final android.content.res.XModuleResources moduleResources = android.content.res.XModuleResources
				.createInstance(MODULE_PATH, resparam.res);

		if (resparam.packageName.equals(Packages.CONTACTS)) {
			try {
				XSecContactsResources.doHook(prefs, resparam, moduleResources);
			} catch (Throwable e) {
				XposedBridge.log(e.toString());

			}
		}

	}

}
