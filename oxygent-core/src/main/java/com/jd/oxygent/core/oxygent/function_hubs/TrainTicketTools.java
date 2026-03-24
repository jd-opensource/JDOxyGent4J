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
package com.jd.oxygent.core.oxygent.function_hubs;

import com.fasterxml.jackson.databind.JsonNode;
import com.jd.oxygent.core.oxygent.oxy.function_tools.FunctionHub;
import com.jd.oxygent.core.oxygent.tools.ParamMetaAuto;
import com.jd.oxygent.core.oxygent.tools.Tool;
import com.jd.oxygent.core.oxygent.utils.JsonUtils;
import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.RequestOptions;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 列车Ticket查询工具
 */
public class TrainTicketTools extends FunctionHub {

    public TrainTicketTools() {
        super("train_ticket_tools");
        this.setDesc("A tool that can query train tickets and station information.");
    }

    // StationData model
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StationData {
        //station_id
        private String stationId;
        //车站名称station_name
        private String stationName;
        //车站代码station_code
        private String stationCode;
        //拼音
        private String stationPinyin;
        //拼音简写
        private String stationShort;
        //station_index
        private String stationIndex;
        //车站编号
        private String code;
        //所属城市
        private String city;
    }

    // Ticket model
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Ticket {
        //车次
        private String trainNo;
        //出发站名
        private String fromStationName;
        //到达站名
        private String toStationName;
        //出发时间
        private String startTime;
        //到达时间
        private String arriveTime;
        //历时
        private String duration;
        //是否可购买
        private String canWebBuy;

        // Price information
        //商务座价格
        private String businessSeatPrice;
        //一等座价格
        private String firstClassPrice;
        //二等座价格
        private String secondClassPrice;
        //软卧价格
        private String softSleeperPrice;
        //硬卧价格
        private String hardSleeperPrice;
        //软座价格
        private String softSeatPrice;
        //硬座价格
        private String hardSeatPrice;
        //无座价格
        private String noSeatPrice;

        // Ticket remaining information
        //商务座余票
        private String businessSeatNum;
        //一等座余票
        private String firstClassNum;
        //二等座余票
        private String secondClassNum;
        //软卧余票
        private String softSleeperNum;
        //硬卧余票
        private String hardSleeperNum;
        //软座余票
        private String softSeatNum;
        //硬座余票
        private String hardSeatNum;
        //无座余票
        private String noSeatNum;
    }

    @Tool(
            name = "get_stations_of_city",
            description = "通过中文城市名查询代表该城市的 station_code。此接口主要用于在用户提供城市名作为出发地或到达地时，为接口准备station_code 参数。",
            paramMetas = {
                    @ParamMetaAuto(
                            name = "city_names",
                            type = "String",
                            description = "要查询的城市，比如\"西安\"。若要查询多个城市，请用|分割，比如\"北京|西安"
                    )
            }
    )
    public String getStationsOfCity(String cityNames) {
        if (cityNames == null || cityNames.trim().isEmpty()) {
            throw new IllegalArgumentException("City names cannot be empty");
        }

        try {
            Map<String, StationData> stations = getStations();
            String[] cityList = cityNames.split("\\|");
            Map<String, List<StationData>> result = new HashMap<>();

            // Initialize result map
            for (String city : cityList) {
                result.put(city, new ArrayList<>());
            }

            // Add stations to corresponding cities
            for (StationData station : stations.values()) {
                String stationCity = station.getCity();
                if (stationCity != null) {
                    for (String city : cityList) {
                        if (stationCity.equals(city)) {
                            result.get(city).add(station);
                            break;
                        }
                    }
                }
            }
            // Convert result to JSON
            return JsonUtils.toJSONString(result);
        } catch (Exception e) {
            throw new RuntimeException("Error getting stations: " + e.getMessage(), e);
        }
    }

    @Tool(
            name = "get_tickets",
            description = "查询车票信息",
            paramMetas = {
                    @ParamMetaAuto(
                            name = "train_date",
                            type = "String",
                            description = "查询日期，格式为 yyyy-MM-dd。如果用户提供的是相对日期（如\"明天\"），请务必先调用 get_current_date 接口获取当前日期，并计算出目标日期。"
                    ),
                    @ParamMetaAuto(
                            name = "from_station_code",
                            type = "String",
                            description = "出发地的 station_code，必须是通过 get_stations_of_city 接口查询得到的车站代码，严禁直接使用中文地名。"
                    ),
                    @ParamMetaAuto(
                            name = "to_station_code",
                            type = "String",
                            description = "到达地的 station_code，必须是通过 get_stations_of_city 接口查询得到的车站代码，严禁直接使用中文地名。"
                    ),
                    @ParamMetaAuto(
                            name = "purpose_codes",
                            type = "String",
                            description = "乘客类型",
                            defaultValue = "ADULT"
                    )
            }
    )
    public String getTickets(String trainDate, String fromStationCode, String toStationCode,String purposeCodes) {
        if (trainDate == null || trainDate.trim().isEmpty()) {
            throw new IllegalArgumentException("Train date cannot be empty");
        }
        if (fromStationCode == null || fromStationCode.trim().isEmpty()) {
            throw new IllegalArgumentException("From station code cannot be empty");
        }
        if (toStationCode == null || toStationCode.trim().isEmpty()) {
            throw new IllegalArgumentException("To station code cannot be empty");
        }
        if (purposeCodes == null) {
            purposeCodes = "ADULT";
        }
        try {
            try (Playwright playwright = Playwright.create()) {
                // 创建 API 请求上下文，配置请求头
                Map<String, String> headers = new HashMap<>();
                headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
                headers.put("Accept", "application/json, text/plain, */*");
                headers.put("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
                headers.put("Accept-Encoding", "gzip, deflate, br");
                headers.put("Connection", "keep-alive");
                headers.put("Cache-Control", "no-cache");
                headers.put("Referer", "https://kyfw.12306.cn/otn/leftTicket/init");
                headers.put("X-Requested-With", "XMLHttpRequest");

                APIRequestContext request = playwright.request().newContext(
                        new APIRequest.NewContextOptions()
                                .setExtraHTTPHeaders(headers)
                                .setTimeout(30000)  // 30秒超时
                );

                // 先访问首页，自动获取必要的 Cookie
                request.get("https://kyfw.12306.cn/otn/leftTicket/init");

                try {
                    // 等待一下，让 Cookie 生效
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                // 发送查询请求 - 使用 RequestOptions 构建参数
                APIResponse queryResponse = request.get(
                        "https://kyfw.12306.cn/otn/leftTicket/queryG",
                        RequestOptions.create().setQueryParam("leftTicketDTO.train_date", trainDate)
                                .setQueryParam("leftTicketDTO.from_station",fromStationCode)
                                .setQueryParam("leftTicketDTO.to_station", toStationCode)
                                .setQueryParam("purpose_codes", purposeCodes != null ? purposeCodes : "ADULT")
                );

                // 获取响应内容
                String responseBody = queryResponse.text();
                // 解析 JSON
                JsonNode root = JsonUtils.readTree(responseBody);
                // 检查返回状态
                if (root.has("status") && !root.get("status").asBoolean()) {
                    return responseBody;
                }
                // 解析车次信息
                if (root.has("data") && root.get("data").has("result")) {
                    JsonNode resultArray = root.get("data").get("result");
                    List<Ticket> tickets = new ArrayList<>();
                    for (JsonNode trainNode : resultArray) {
                        String trainInfo = trainNode.asText();
                        String[] parts = trainInfo.split("\\|");
                        if (parts.length < 35) {
                            continue;
                        }
                        Ticket ticket = new Ticket(
                                parts[3],  // train_no
                                parts[6],  // from_station_name
                                parts[7],  // to_station_name
                                parts[8],  // start_time
                                parts[9],  // arrive_time
                                parts[10], // duration
                                parts[11], // can_web_buy
                                null,      // business_seat_price
                                null,      // first_class_price
                                null,      // second_class_price
                                null,      // soft_sleeper_price
                                null,      // hard_sleeper_price
                                null,      // soft_seat_price
                                null,      // hard_seat_price
                                null,      // no_seat_price
                                parts[32].isEmpty() ? null : parts[32], // business_seat_num
                                parts[31].isEmpty() ? null : parts[31], // first_class_num
                                parts[30].isEmpty() ? null : parts[30], // second_class_num
                                parts[23].isEmpty() ? null : parts[23], // soft_sleeper_num
                                parts[28].isEmpty() ? null : parts[28], // hard_sleeper_num
                                parts[24].isEmpty() ? null : parts[24], // soft_seat_num
                                parts[29].isEmpty() ? null : parts[29], // hard_seat_num
                                parts[26].isEmpty() ? null : parts[26]  // no_seat_num
                        );
                        tickets.add(ticket);
                    }
                    return JsonUtils.toJSONString(tickets);
                } else {
                    return responseBody;
                }
            }
        }catch(Exception e){
            throw new RuntimeException("Error getting tickets: " + e.getMessage(), e);
        }
    }

    @Tool(
            name = "get_current_date",
            description = "获取当前日期，以上海时区（Asia/Shanghai, UTC+8）为准，返回格式为 yyyy-MM-dd。主要用于解析用户提到的相对日期（如\"明天\"、\"下周三\"），为其他需要日期的接口提供准确的日期输入。"
    )
    public String getCurrentDate() {
        try {
            LocalDate now = LocalDate.now(ZoneId.of("Asia/Shanghai"));
            return now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception e) {
            throw new RuntimeException("Error getting current date: " + e.getMessage(), e);
        }
    }

    /**
     * 从12306网站获取并解析所有火车站数据
     * @return 以车站代码为键的车站数据字典
     * @throws Exception 当无法获取或解析车站数据时抛出异常
     */
    private Map<String, StationData> getStations() throws Exception {
        String mainPageUrl = "https://www.12306.cn/index/";
        String html = sendGetRequest(mainPageUrl);

        // Extract station name JS file path
        Pattern pattern = Pattern.compile("(/script/core/common/station_name.+?\\.js)");
        Matcher matcher = pattern.matcher(html);
        if (!matcher.find()) {
            throw new Exception("Get station name js file failed.");
        }

        String stationNameJsFilePath = matcher.group(0);
        String stationNameJs = sendGetRequest(mainPageUrl + stationNameJsFilePath);

        // Extract raw station data
        pattern = Pattern.compile("var station_names ='(.*?)'");
        matcher = pattern.matcher(stationNameJs);
        if (!matcher.find()) {
            throw new Exception("Extract station data failed.");
        }

        String rawData = matcher.group(1);
        return parseStationsData(rawData);
    }

    /**
     *     将原始车站数据字符串解析为结构化的字典
     * @param rawData 包含车站信息的竖线分隔字符串
     * @return 以车站代码为键，车站数据为值的字典
     */
    private Map<String, StationData> parseStationsData(String rawData) {
        Map<String, StationData> result = new HashMap<>();
        String[] dataArray = rawData.split("\\|");

        // Group data by 10 elements per station
        for (int i = 0; i < dataArray.length / 10; i++) {
            String[] group = new String[10];
            System.arraycopy(dataArray, i * 10, group, 0, 10);

            // Skip if station code is empty
            if (group[2] == null || group[2].isEmpty()) {
                continue;
            }

            StationData station = new StationData(
                    group[0],
                    group[1],
                    group[2],
                    group[3],
                    group[4],
                    group[5],
                    group[6],
                    group[7]
            );

            result.put(station.getStationCode(), station);
        }

        return result;
    }

    // Send GET request
    private String sendGetRequest(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        return response.toString();
    }


}
