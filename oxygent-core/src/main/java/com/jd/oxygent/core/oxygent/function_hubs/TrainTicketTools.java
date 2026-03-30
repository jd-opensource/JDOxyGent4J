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
 * Train ticket query tools
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
        // station_id
        private String stationId;
        // station_name
        private String stationName;
        // station_code
        private String stationCode;
        // pinyin
        private String stationPinyin;
        // pinyin abbreviation
        private String stationShort;
        // station_index
        private String stationIndex;
        // station number
        private String code;
        // city
        private String city;
    }

    // Ticket model
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Ticket {
        // train number
        private String trainNo;
        // departure station name
        private String fromStationName;
        // arrival station name
        private String toStationName;
        // departure time
        private String startTime;
        // arrival time
        private String arriveTime;
        // duration
        private String duration;
        // whether can be purchased
        private String canWebBuy;

        // Price information
        // business seat price
        private String businessSeatPrice;
        // first class seat price
        private String firstClassPrice;
        // second class seat price
        private String secondClassPrice;
        // soft sleeper price
        private String softSleeperPrice;
        // hard sleeper price
        private String hardSleeperPrice;
        // soft seat price
        private String softSeatPrice;
        // hard seat price
        private String hardSeatPrice;
        // no seat price
        private String noSeatPrice;

        // Ticket remaining information
        // business seat remaining
        private String businessSeatNum;
        // first class seat remaining
        private String firstClassNum;
        // second class seat remaining
        private String secondClassNum;
        // soft sleeper remaining
        private String softSleeperNum;
        // hard sleeper remaining
        private String hardSleeperNum;
        // soft seat remaining
        private String softSeatNum;
        // hard seat remaining
        private String hardSeatNum;
        // no seat remaining
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
                // Create API request context, configure request headers
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

                // First visit homepage, automatically get necessary cookies
                request.get("https://kyfw.12306.cn/otn/leftTicket/init");

                try {
                    // Wait a moment for cookies to take effect
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                // Send query request - use RequestOptions to build parameters
                APIResponse queryResponse = request.get(
                        "https://kyfw.12306.cn/otn/leftTicket/queryG",
                        RequestOptions.create().setQueryParam("leftTicketDTO.train_date", trainDate)
                                .setQueryParam("leftTicketDTO.from_station",fromStationCode)
                                .setQueryParam("leftTicketDTO.to_station", toStationCode)
                                .setQueryParam("purpose_codes", purposeCodes != null ? purposeCodes : "ADULT")
                );

                // Get response content
                String responseBody = queryResponse.text();
                // Parse JSON
                JsonNode root = JsonUtils.readTree(responseBody);
                // Check return status
                if (root.has("status") && !root.get("status").asBoolean()) {
                    return responseBody;
                }
                // Parse train information
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
     * Fetch and parse all train station data from 12306 website
     * @return Station data dictionary with station code as key
     * @throws Exception when unable to fetch or parse station data
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
     *     Parse raw station data string into structured dictionary
     * @param rawData Pipe-separated string containing station information
     * @return Dictionary with station code as key and station data as value
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
