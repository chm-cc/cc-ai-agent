package com.chm.aiagent.tools;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * 天气查询工具 —— 基于免费 wttr.in API，无需 API Key
 */
public class WeatherTool {

    private static final String WEATHER_API_URL = "https://wttr.in";

    @Tool(description = "Query the current weather and today's forecast for a given city")
    public String queryWeather(
            @ToolParam(description = "City name, supports Chinese and English, e.g. 北京, 上海, Tokyo, London") String city) {
        try {
            String url = WEATHER_API_URL + "/" + city + "?format=j1";
            String response = HttpUtil.get(url);

            JSONObject json = JSONUtil.parseObj(response);

            // 当前天气
            JSONArray currentArr = json.getJSONArray("current_condition");
            if (currentArr == null || currentArr.isEmpty()) {
                return "未找到城市「" + city + "」的天气信息，请检查城市名称是否正确";
            }
            JSONObject current = currentArr.getJSONObject(0);

            String tempC = current.getStr("temp_C");
            String feelsLikeC = current.getStr("FeelsLikeC");
            String humidity = current.getStr("humidity");
            String windSpeed = current.getStr("windspeedKmph");
            String windDir = current.getStr("winddir16Point");
            String weatherDesc = current.getJSONArray("weatherDesc").getJSONObject(0).getStr("value");
            String visibility = current.getStr("visibility");

            // 今日预报
            JSONArray weatherArr = json.getJSONArray("weather");
            String maxTemp = "N/A";
            String minTemp = "N/A";
            if (weatherArr != null && !weatherArr.isEmpty()) {
                JSONObject today = weatherArr.getJSONObject(0);
                maxTemp = today.getStr("maxtempC");
                minTemp = today.getStr("mintempC");
            }

            return String.format("""
                            城市：%s
                            天气：%s
                            当前温度：%s°C（体感温度：%s°C）
                            今日最高：%s°C / 最低：%s°C
                            湿度：%s%%
                            风速：%s km/h（%s）
                            能见度：%s km""",
                    city, weatherDesc, tempC, feelsLikeC,
                    maxTemp, minTemp, humidity, windSpeed, windDir, visibility);

        } catch (Exception e) {
            return "查询天气失败：" + e.getMessage();
        }
    }
}
