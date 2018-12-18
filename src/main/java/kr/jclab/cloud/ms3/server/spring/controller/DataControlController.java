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

import kr.jclab.cloud.ms3.common.dto.ListObjectsDTO;
import kr.jclab.cloud.ms3.common.dto.GenerateUriDTO;
import kr.jclab.cloud.ms3.server.spring.MS3SpringServer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.AbstractController;

import javax.servlet.http.HttpServletRequest;
import java.io.File;

@RestController
@RequestMapping(path = "/api")
public class DataControlController {
    private MS3SpringServer ms3SpringServer;

    public DataControlController(MS3SpringServer ms3SpringServer) {
        this.ms3SpringServer = ms3SpringServer;
    }

    @RequestMapping(path = "/bucket/list/{bucket}", method = RequestMethod.GET)
    public ResponseEntity<ListObjectsDTO.Response> buckerList(@PathVariable("bucket") String bucket) {
        ListObjectsDTO.Response responseBody = new ListObjectsDTO.Response();
        File bucketDir = ms3SpringServer.getBucketDirectory(bucket);

        if(!bucketDir.exists()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        for(File file : bucketDir.listFiles((File dir, String name) -> name.startsWith("data-"))) {
            ListObjectsDTO.ObjectSummary objectSummary = new ListObjectsDTO.ObjectSummary();
            objectSummary.bucketName = bucket;
            objectSummary.key = file.getName().substring(5);
            objectSummary.lastModified = file.lastModified();
            objectSummary.size = file.length();
            responseBody.list.add(objectSummary);
        }

        return new ResponseEntity<>(responseBody, HttpStatus.OK);
    }

    @RequestMapping(path = "/bucket/generateuri/{bucket}/{key:.+}", method = RequestMethod.GET)
    public ResponseEntity<GenerateUriDTO.Response> generateUri(@PathVariable("bucket") String bucket, @PathVariable("key") String key) {
        GenerateUriDTO.Response responseBody = new GenerateUriDTO.Response();
        String shareid = ms3SpringServer.generateShareId(bucket, key);
        if(shareid == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        responseBody.code = HttpStatus.OK.value();
        responseBody.uri = "public/" + shareid;

        return new ResponseEntity<>(responseBody, HttpStatus.OK);
    }
}
