/*
 * Copyright 2018 JC-Lab. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kr.jclab.cloud.ms3.server.spring.object;

import java.io.File;

public class MS3ObjectFile {
    public File resourceDirectory;
    public String bucket;
    public String key;

    public File dataFile;
    public File metadataFile;

    public MS3ObjectFile(File resourceDirectory, String bucket, String key) {
        this.resourceDirectory = resourceDirectory;
        this.bucket = bucket;
        this.key = key;

        this.dataFile = new File(this.resourceDirectory, "data/" + bucket + "/data-" + key);
        this.metadataFile = new File(this.resourceDirectory, "data/" + bucket + "/meta-" + key);
    }
}
