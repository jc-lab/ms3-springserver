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

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.jclab.cloud.ms3.common.model.ObjectMetadata;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;

public class MS3ObjectFile {
    private final ObjectMapper objectMapper;

    private File resourceDirectory;
    private final String bucket;
    private final String key;

    public final File metadataFile;
    private File dataFile = null;

    private ObjectMetadata objectMetadata = null;
    private boolean useDirectorySpliter = false;

    public MS3ObjectFile(File resourceDirectory, String bucket, String key, ObjectMapper objectMapper, boolean newFile) throws IOException {
        this.objectMapper = objectMapper;
        this.resourceDirectory = resourceDirectory;
        this.bucket = bucket;
        this.key = key;

        this.metadataFile = new File(this.resourceDirectory, "data/" + bucket + "/meta-" + key);
        if(!newFile && this.metadataFile.exists()) {
            this.objectMetadata = this.objectMapper.readValue(this.metadataFile, ObjectMetadata.class);
            setUseDirectorySpliter(checkUseDirectorySpliter(this.objectMetadata));
        }
    }

    public void setUseDirectorySpliter(boolean value) {
        this.useDirectorySpliter = value;
        if(value)
        {
            try {
                final String HexString = "0123456789abcdef";
                byte[] digest = MessageDigest.getInstance("SHA-1").digest(this.key.getBytes("UTF-8"));
                StringBuilder prefix = new StringBuilder();
                prefix.append(HexString.charAt((digest[0] >> 4) & 0xf));
                prefix.append(HexString.charAt((digest[0] >> 0) & 0xf));
                this.dataFile = new File(this.resourceDirectory, "data/" + bucket + "/dsdata-" + prefix.toString() + "/" + key);
                File temp = this.dataFile.getParentFile();
                if (!temp.exists())
                    temp.mkdirs();
            } catch(Exception e) {
                throw new RuntimeException(e);
            }
        }else{
            this.dataFile = new File(this.resourceDirectory, "data/" + bucket + "/data-" + key);
        }
    }

    public boolean isUseDirectorySpliter() {
        return useDirectorySpliter;
    }

    public ObjectMetadata getObjectMetadata() {
        return objectMetadata;
    }

    public void setAndSaveObjectMetadata(ObjectMetadata objectMetadata) throws IOException {
        this.objectMetadata = objectMetadata;
        if(this.objectMetadata != null) {
            this.objectMapper.writeValue(this.metadataFile, this.objectMetadata);
            setUseDirectorySpliter(checkUseDirectorySpliter(this.objectMetadata));
        }
    }

    public File getDataFile() {
        if(this.dataFile == null)
            setUseDirectorySpliter(false);
        return this.dataFile;
    }

    private static boolean checkUseDirectorySpliter(ObjectMetadata objectMetadata) {
        String directorySpliter = objectMetadata.getUserMetaDataOf("kr.jclab.cloud.ms3.directoryspliter");
        if(directorySpliter != null) {
            if(directorySpliter.equalsIgnoreCase("true") || directorySpliter.equalsIgnoreCase("1"))
            {
                return true;
            }
        }
        return false;
    }

}
