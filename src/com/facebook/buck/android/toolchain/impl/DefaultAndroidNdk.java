/*
 * Copyright 2017-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.facebook.buck.android.toolchain.impl;

import com.facebook.buck.android.AndroidBuckConfig;
import com.facebook.buck.android.AndroidDirectoryResolver;
import com.facebook.buck.android.toolchain.AndroidNdk;
import java.util.Optional;

public class DefaultAndroidNdk implements AndroidNdk {
  private final AndroidBuckConfig androidBuckConfig;
  private final AndroidDirectoryResolver androidDirectoryResolver;

  public DefaultAndroidNdk(
      AndroidBuckConfig androidBuckConfig, AndroidDirectoryResolver androidDirectoryResolver) {
    this.androidBuckConfig = androidBuckConfig;
    this.androidDirectoryResolver = androidDirectoryResolver;
  }

  @Override
  public String getNdkVersion() {
    // If a NDK version isn't specified, we've got to reach into the runtime environment to find
    // out which one we will end up using.
    Optional<String> ndkVersion =
        androidBuckConfig
            .getNdkVersion()
            .map(Optional::of)
            .orElseGet(androidDirectoryResolver::getNdkVersion);
    if (!ndkVersion.isPresent()) {
      throw new IllegalStateException("Cannot detect NDK version");
    }
    return ndkVersion.get();
  }
}