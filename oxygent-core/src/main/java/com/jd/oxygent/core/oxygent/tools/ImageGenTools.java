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
package com.jd.oxygent.core.oxygent.tools;

import com.jd.oxygent.core.oxygent.oxy.function_tools.FunctionHub;

public class ImageGenTools extends FunctionHub {

    public ImageGenTools() {
        super("image_gen_tools");
        this.setDesc("Image generation Tools, returns image URL");
    }

    @Tool(
            name = "gen_image",
            description = "An image generation service that takes text descriptions as input and returns a URL of the image,text descriptions only accept English, so you need to translate the description into English in advance.",
            paramMetas = {
                    @ParamMetaAuto(
                            name = "description",
                            type = "String",
                            description = "image text descriptions"
                    )
            }
    )
    public String genImage(String description) {
        return String.format("https://image.pollinations.ai/prompt/%s?nologo=true", description);
    }

    public static void main(String[] args) {
        var imageGenTools = new ImageGenTools();

        System.out.println("=== Image Generation URL Service Tool ===");

        var imageUrl = imageGenTools.call("gen_image", "big women");
        System.out.println("Image generation URL: " + imageUrl);
    }
}
