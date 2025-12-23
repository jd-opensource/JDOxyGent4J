/*
 * Copyright 2025 JD.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this project except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jd.oxygent.core.oxygent.schemas;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * <h3>File Information Bean</h3>
 *
 * <p>FilesInfo is a data transfer object used in the OxyGent framework to represent file information.
 * This class contains basic file property information and is used to pass file-related data between modules.</p>
 *
 * <h3>Design Purpose</h3>
 * <ul>
 *   <li><strong>Decoupling</strong>: Abstracts file information as an independent DTO, avoiding cyclic dependencies between modules</li>
 *   <li><strong>Standardization</strong>: Provides a unified data structure for file information</li>
 *   <li><strong>Extensibility</strong>: Supports adding more file attribute fields in the future</li>
 *   <li><strong>Serialization Friendly</strong>: Supports JSON serialization and deserialization</li>
 * </ul>
 *
 * <h3>Field Description</h3>
 * <ul>
 *   <li><strong>fileName</strong>: File name, including file extension</li>
 *   <li><strong>fileUrl</strong>: URL address for file access</li>
 *   <li><strong>fileType</strong>: File type identifier, used to determine processing strategy</li>
 * </ul>
 *
 * <h3>Usage Scenarios</h3>
 * <ul>
 *   <li><strong>Multimodal Processing</strong>: Handles file resources in MultimodalResourceType</li>
 *   <li><strong>File Upload</strong>: Receives and passes uploaded file information</li>
 *   <li><strong>File Download</strong>: Provides basic information needed for file download</li>
 *   <li><strong>API Transfer</strong>: Acts as request/response object for REST APIs</li>
 * </ul>
 *
 * <h3>JSON Serialization Support</h3>
 * <p>This class supports both FastJSON and Jackson serialization frameworks to ensure compatibility across different environments.</p>
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
public class FilesInfo {

    /**
     * File name
     * <p>Includes the complete file name and extension, e.g.: document.pdf, image.jpg</p>
     */
    @JsonProperty("file_name")
    private String fileName;

    /**
     * File URL address
     * <p>The complete access path of the file, can be HTTP/HTTPS URL or local file path</p>
     */
    @JsonProperty("file_url")
    private String fileUrl;

    /**
     * File type
     * <p>Type identifier of the file, used to determine specific processing strategy, e.g.: image, pdf, document</p>
     */
    @JsonProperty("file_type")
    private String fileType;
}