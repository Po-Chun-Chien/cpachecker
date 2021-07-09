/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.core.algorithm.tiger.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class TigerTestUtil {
  @SuppressWarnings("StringSplitter")
  public static Map<String, String> getConfigurationFromPropertiesFile(
      File propertiesFile) {
    Map<String, String> configuration = new HashMap<>();
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(new FileInputStream(propertiesFile), "UTF-8"))) {
      String line = null;
      while ((line = reader.readLine()) != null) {
        line = line.trim();
        if (line.startsWith("# ") || line.isEmpty()) {
          continue;
        }

        if (line.startsWith("#include")) {
          String[] pair = line.split(" ");
          if (pair.length != 2) {
            continue;
          } else {
            pair[0] = pair[0].trim();
            pair[1] = pair[1].trim();
          }

          configuration.putAll(getConfigurationFromPropertiesFile(
              new File(propertiesFile.getPath().substring(0,
                  propertiesFile.getPath().lastIndexOf(File.separator)) + "/" + pair[1])));
        }

        String[] pair = line.split("=");
        if (pair.length != 2) {
          continue;
        } else {
          pair[0] = pair[0].trim();
          pair[1] = pair[1].trim();
        }

        if (pair[0].equals("tiger.algorithmConfigurationFile")) {
          pair[1] = "config/" + pair[1];
        }

        configuration.put(pair[0], pair[1]);
      }
    } catch (IOException e) {
      return null;
    }

    return configuration;
  }

}
