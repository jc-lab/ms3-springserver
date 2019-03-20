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
package kr.jclab.cloud.ms3.server.spring.controller;

import kr.jclab.cloud.ms3.common.model.ObjectMetadata;
import kr.jclab.cloud.ms3.server.spring.MS3SpringServer;
import kr.jclab.cloud.ms3.server.spring.object.MS3ObjectFile;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

@Controller
@RequestMapping(path = "/public")
public class PublicController {
    private MS3SpringServer ms3SpringServer;

    public PublicController(MS3SpringServer ms3SpringServer) {
        this.ms3SpringServer = ms3SpringServer;
    }

    @RequestMapping(path =  "/{shareid}")
    public ResponseEntity<InputStreamResource> getByShareId(@PathVariable("shareid") String shareid) {
        MS3ObjectFile objectFile = ms3SpringServer.findMS3ObjectByShareid(shareid, ms3SpringServer.getMetadataObjectMapper());
        if(objectFile == null)
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        if(!objectFile.getDataFile().exists())
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        try {
            ObjectMetadata objectMetadata = ms3SpringServer.getObjectMetadata(objectFile);
            HttpHeaders httpHeaders = new HttpHeaders();
            if(objectMetadata != null) {
                String contentType = objectMetadata.getContentType();
                String contentEncoding = objectMetadata.getContentEncoding();
                if(contentType != null)
                    httpHeaders.set("Content-Type", contentType);
                if(contentEncoding != null)
                    httpHeaders.set("Content-Encoding", contentEncoding);
            }
            return new ResponseEntity<>(new InputStreamResource(new FileInputStream(objectFile.getDataFile())), httpHeaders, HttpStatus.OK);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }
}
