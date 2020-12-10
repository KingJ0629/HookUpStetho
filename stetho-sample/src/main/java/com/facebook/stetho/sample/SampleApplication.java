/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.stetho.sample;

import android.app.Application;

import com.facebook.stetho.dumpapp.DumpException;
import com.facebook.stetho.dumpapp.DumperContext;
import com.facebook.stetho.dumpapp.DumperPlugin;

public class SampleApplication extends Application {
  @Override
  public void onCreate() {
    super.onCreate();
  
//    Stetho.initializeWithDefaults(this);

//    Stetho.initialize(Stetho.newInitializerBuilder(this)
//            .enableDumpapp(new DumperPluginsProvider() {
//              @Override
//              public Iterable<DumperPlugin> get() {
//                return new Stetho.DefaultDumperPluginsBuilder(SampleApplication.this)
//                        .provide(new MyDumperPlugin())
//                        .finish();
//              }
//            })
//            .enableWebKitInspector(Stetho.defaultInspectorModulesProvider(this))
//            .build());
  }
  
  class MyDumperPlugin implements DumperPlugin {
  
    @Override
    public String getName() {
      return "MyDumperPlugin";
    }
  
    @Override
    public void dump(DumperContext dumpContext) throws DumpException {
    
    }
  }
}
