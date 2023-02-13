
package com.reactnativecommunity.cameraroll;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.facebook.react.TurboReactPackage;
import com.facebook.react.ViewManagerOnDemandReactPackage;
import com.facebook.react.bridge.ModuleSpec;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.module.annotations.ReactModuleList;
import com.facebook.react.module.model.ReactModuleInfo;
import com.facebook.react.module.model.ReactModuleInfoProvider;
import com.facebook.react.turbomodule.core.interfaces.TurboModule;
import com.facebook.react.uimanager.ViewManager;
import com.facebook.react.bridge.JavaScriptModule;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@ReactModuleList(
        nativeModules = {
                CameraRollModule.class,
        })
public class CameraRollPackage extends TurboReactPackage implements ViewManagerOnDemandReactPackage {

    /** {@inheritDoc} */
    @Override
    public List<String> getViewManagerNames(ReactApplicationContext reactContext) {
        return null;
    }

    @Override
    protected List<ModuleSpec> getViewManagers(ReactApplicationContext reactContext) {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public @Nullable
    ViewManager createViewManager(
            ReactApplicationContext reactContext, String viewManagerName) {
        return null;
    }

    @Override
    public NativeModule getModule(String name, @Nonnull ReactApplicationContext reactContext) {
        switch (name) {
            case CameraRollModule.NAME:
                return new CameraRollModule(reactContext);
            default:
                return null;
        }
    }

    @Override
    public ReactModuleInfoProvider getReactModuleInfoProvider() {
        try {
            Class<?> reactModuleInfoProviderClass =
                    Class.forName("com.reactnativecommunity.cameraroll.CameraRollPackage$$ReactModuleInfoProvider");
            return (ReactModuleInfoProvider) reactModuleInfoProviderClass.newInstance();
        } catch (ClassNotFoundException e) {
            // ReactModuleSpecProcessor does not run at build-time. Create this ReactModuleInfoProvider by
            // hand.
            return new ReactModuleInfoProvider() {
                @Override
                public Map<String, ReactModuleInfo> getReactModuleInfos() {
                    final Map<String, ReactModuleInfo> reactModuleInfoMap = new HashMap<>();

                    Class<? extends NativeModule>[] moduleList =
                            new Class[] {
                                    CameraRollModule.class,
                            };

                    for (Class<? extends NativeModule> moduleClass : moduleList) {
                        ReactModule reactModule = moduleClass.getAnnotation(ReactModule.class);

                        reactModuleInfoMap.put(
                                reactModule.name(),
                                new ReactModuleInfo(
                                        reactModule.name(),
                                        moduleClass.getName(),
                                        reactModule.canOverrideExistingModule(),
                                        reactModule.needsEagerInit(),
                                        reactModule.hasConstants(),
                                        reactModule.isCxxModule(),
                                        TurboModule.class.isAssignableFrom(moduleClass)));
                    }

                    return reactModuleInfoMap;
                }
            };
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(
                    "No ReactModuleInfoProvider for com.reactnativecommunity.cameraroll.CameraRollPackage$$ReactModuleInfoProvider", e);
        }
    }

    @Override
    public List<NativeModule> createNativeModules(ReactApplicationContext reactContext) {
        return Arrays.<NativeModule>asList(new CameraRollModule(reactContext));
    }

    // Deprecated from RN 0.47
    public List<Class<? extends JavaScriptModule>> createJSModules() {
        return Collections.emptyList();
    }

    @Override
    public List<ViewManager> createViewManagers(ReactApplicationContext reactContext) {
        return Collections.emptyList();
    }
}