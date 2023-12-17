package controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.opencsv.CSVWriter;
import dao.ForecastResultsDao;
import database.DBConnection;
import entity.Config;
import util.SendMail;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static util.CreateFileLog.createFIleLog;

public class Controller {
    // Configuration file path
    private static final String FILE_CONFIG = "\\config.properties";

    // API Key, URL, and list of cities
    static String apiKey;
    static String url;
    static List<String> cities;
    // Load attributes from the configuration file
    public static void loadAttribute(){
        Properties properties = new Properties();
        InputStream inputStream = null;
        try {
            String currentDir = System.getProperty("user.dir");
            inputStream = new FileInputStream(currentDir + FILE_CONFIG);
            // load properties from file
            properties.load(inputStream);
            // get property by name
            apiKey = properties.getProperty("apiKey"); //key của account trên openweather
            url = properties.getProperty("url"); //url lấy dữ liệu
            cities = convertCities(properties.getProperty("cities")); //danh sách các khu vực muốn lấy dữ liệu
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // close objects
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public void getData(Connection connection, Config config) {
        //(Extract)12. Load các thuộc tính để lấy dữ liệu từ API
        loadAttribute();
        ForecastResultsDao dao = new ForecastResultsDao();
        //(Extract)13. Cập nhật  trạng thái của config là đang xử lý (isProcessing=true)
        dao.updateIsProcessing(connection, config.getId(), true);
        //(Extract)14. Cập nhật status của config thành CRAWLING (status=CRAWLING)
        dao.updateStatus(connection, config.getId(), "CRAWLING");
        //(Extract)15. Thêm thông tin đang craw dữ liệu vào log
        dao.insertLog(connection, config.getId(), "CRAWLING", "Start crawl data");

        //Create file datasource with pathSource
        DateTimeFormatter dtf_file = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
        DateTimeFormatter dt_now = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        String fileName = config.getFileName();
        String pathFileCsv = config.getPath();
        String pathSource = pathFileCsv + "\\" + fileName +dtf_file.format(now)+ ".csv";
        try {

            //(Extract)16. Tạo file csv dể lưu trữ dữ liệu lấy từ API
            CSVWriter writer = new CSVWriter(new FileWriter(pathSource));
            //Time now
            LocalDateTime dtf = LocalDateTime.now();
            //(Extract)17. Duyệt các thành phố có trong cities
            // loop i (city)
            Iterator<String> iterator = cities.iterator();
            while (iterator.hasNext()) {
                //(Extract)18. Kết nối URL với citi muốn lấy dữ liệu
                String city = iterator.next();
                //Connect URL API with city
                String urlCity = String.format(url, city.replace(" ", "%20"), apiKey);
                URL url = new URL(urlCity);
                HttpURLConnection connectionHTTP = (HttpURLConnection) url.openConnection();
                connectionHTTP.setRequestMethod("GET");
                int responseCode = connectionHTTP.getResponseCode();
                //Get ResponseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    //6. Get Data from response
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connectionHTTP.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    //Parse JSON response with Gson
                    JsonParser parser = new JsonParser();
                    JsonObject jsonResponse = parser.parse(response.toString()).getAsJsonObject();

                    //Loop through forecast data and write to CSV
                    JsonArray forecasts = jsonResponse.getAsJsonArray("list");
                    for (int i = 0; i < forecasts.size(); i++) {
                        //(Extract)19. Lấy dữ liệu Json từ API, thời gian lấy dữ liệu ghi vào file CSV

                        //Create an ArrayList to hold all the data for each forecast entry
                        List<String> data = new ArrayList<>();

                        //Add data des
                        data.add(jsonResponse.get("cod").getAsString());
                        data.add(jsonResponse.get("message").getAsString());
                        data.add(jsonResponse.get("cnt").getAsString());

                        //Add data of forecast to arraylist
                        JsonObject forecast = forecasts.get(i).getAsJsonObject();
                        JsonObject cityInfo = jsonResponse.getAsJsonObject("city");

                        // Add city information
                        data.add(cityInfo.get("id").getAsString());
                        data.add(cityInfo.get("name").getAsString());
                        data.add(cityInfo.getAsJsonObject("coord").get("lat").getAsString());
                        data.add(cityInfo.getAsJsonObject("coord").get("lon").getAsString());
                        data.add(cityInfo.get("country").getAsString());
                        data.add(cityInfo.get("population").getAsString());
                        data.add(cityInfo.get("timezone").getAsString());
                        data.add(cityInfo.get("sunrise").getAsString());
                        data.add(cityInfo.get("sunset").getAsString());

                        // Add forecast information
                        data.add(forecast.get("dt").getAsString());
                        data.add(forecast.get("dt_txt").getAsString());
                        JsonObject mainData = forecast.getAsJsonObject("main");
                        data.add(mainData.get("temp").getAsString());
                        data.add(mainData.get("feels_like").getAsString());
                        data.add(mainData.get("temp_min").getAsString());
                        data.add(mainData.get("temp_max").getAsString());
                        data.add(mainData.get("pressure").getAsString());
                        data.add(mainData.get("sea_level").getAsString());
                        data.add(mainData.get("grnd_level").getAsString());
                        data.add(mainData.get("humidity").getAsString());
                        data.add(mainData.get("temp_kf").getAsString());

                        JsonArray weatherArray = forecast.getAsJsonArray("weather");
                        JsonObject weatherData = weatherArray.get(0).getAsJsonObject();
                        data.add(weatherData.get("id").getAsString());
                        data.add(weatherData.get("main").getAsString());
                        data.add(weatherData.get("description").getAsString());
                        data.add(weatherData.get("icon").getAsString());

                        JsonObject cloudsData = forecast.getAsJsonObject("clouds");
                        data.add(cloudsData.get("all").getAsString());

                        JsonObject windData = forecast.getAsJsonObject("wind");
                        data.add(windData.get("speed").getAsString());
                        data.add(windData.get("deg").getAsString());
                        data.add(windData.get("gust").getAsString());

                        data.add(forecast.get("visibility").getAsString());
                        data.add(forecast.get("pop").getAsString());

                        JsonObject rainData = forecast.getAsJsonObject("rain");
                        if (rainData != null) {
                            data.add(rainData.get("3h").getAsString());
                        } else {
                            data.add(""); // If "rain" data is null, add an empty string
                        }

                        JsonObject sysData = forecast.getAsJsonObject("sys");
                        data.add(sysData.get("pod").getAsString());
                        //thời gian lấy dữ liệu
                        data.add(dtf.format(dt_now));

                        //Write data from arraylist to CSV
                        writer.writeNext(data.toArray(new String[0]));
                    }
                } else {
                    //(Extract)20. Thêm thông tin lỗi khi lấy dữ liệu của thành phố đó vào log
                    dao.insertLog(connection, config.getId(), "ERROR", "Error get Data with city: "+ city);
                    //(Extract)21. Send mail thông báo lỗi lấy dữ liệu của thành phố đó
                    String mail = config.getEmail();
                    DateTimeFormatter dt = DateTimeFormatter.ofPattern("hh:mm:ss dd/MM/yyyy");
                    LocalDateTime nowTime = LocalDateTime.now();
                    String timeNow = nowTime.format(dt);
                    String subject = "Error Date: " + timeNow;
                    String message = "Error getData with city: "+city;
                    SendMail.sendMail(mail, subject, message);
                }
            }
            writer.close();
            System.out.println("CRAWLED success");
            //(Extract)27. Cập nhật đường dẫn chi tiết của file CSV
            dao.updateDetailFilePath(connection, config.getId(), pathSource);
            config.setDetailPathFile(pathSource);
            //(Extract)28. Cập nhật status của config thành CRAWLED
            dao.updateStatus(connection, config.getId(), "CRAWLED");
            //(Extract)29. Thêm thông tin đã crawl dữ liệu vào log
            dao.insertLog(connection, config.getId(), "CRAWLED", "End crawl, data to "+pathSource);
            //
            extractToStaging(connection, config);
        } catch (IOException e) {
            //(Extract)22. Cập nhật status của config thành ERROR
            dao.updateStatus(connection, config.getId(), "ERROR");
            //(Extract)23. Thêm lỗi vào log
            dao.insertLog(connection, config.getId(), "ERROR", "Error with message: "+e.getMessage());
            //(Extract)24. Chỉnh Flag=0 cho config
            dao.setFlagIsZero(connection, config.getId());
            //(Extract)25. Cập nhật trạng thái của config là không xử lý (isProcessing=false)
            dao.updateIsProcessing(connection, config.getId(), false);
            String mail = config.getEmail();
            DateTimeFormatter dt = DateTimeFormatter.ofPattern("hh:mm:ss dd/MM/yyyy");
            LocalDateTime nowTime = LocalDateTime.now();
            String timeNow = nowTime.format(dt);
            String subject = "Error Date: " + timeNow;
            String message = "Error with message: "+e.getMessage();
            String pathLogs = createFIleLog(dao.getLogs(connection, config.getId()));
            if(pathLogs!=null){
                SendMail.sendMail(mail, subject, message, pathLogs);
            }
            else SendMail.sendMail(mail, subject, message);
        }
    }
    public static void extractToStaging(Connection connection, Config config){
        ForecastResultsDao dao = new ForecastResultsDao();
        //(Extract to Staging)12. Cập nhật  trạng thái của config là đang xử lý (isProcessing=true)
        dao.updateIsProcessing(connection, config.getId(), true);
        //(Extract to Staging)13. Cập nhật status của config thành EXTRACTING (status=EXTRACTING)
        dao.updateStatus(connection, config.getId(), "EXTRACTING");
        //(Extract to Staging)14. Thêm thông tin đang extract to staging vào log
        dao.insertLog(connection, config.getId(), "EXTRACTING", "Start extract data");
        //(Extract to Staging)15. Truncate table staging trong database staging
        //truncate table
        truncateTable(connection, config);
        //(Extract to Staging)16. Thêm thông tin đã truncate table staging vào log
        dao.insertLog(connection, config.getId(), "EXTRACTING", "Truncate table staging");
        //(Extract to Staging)17. Load dữ liệu file CSV vào table Staging bằng lệnh "Load Data Infile" trong mysql
        //load data to staging
        String sqlLoadData = "LOAD DATA INFILE ? INTO TABLE staging.staging FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '\"' LINES TERMINATED BY '\\n' IGNORE 0 LINES (\n" +
                "    cod, message, cnt, city_id, city_name, city_latitude, city_longitude, city_country_code, city_population,\n" +
                "    city_timezone, city_sunrise, city_sunset, dt, dt_txt, main_temp, main_feels_like, main_temp_min,\n" +
                "    main_temp_max, main_pressure, main_sea_level, main_grnd_level, main_humidity, main_temp_kf, weather_id,\n" +
                "    weather_main, weather_description, weather_icon, clouds_all, wind_speed, wind_deg, wind_gust, visibility,\n" +
                "    pop, rain_3h, sys_pod, created_date)";
        try {
            //Load data to staging
            PreparedStatement psLoadData = connection.prepareStatement(sqlLoadData);
            psLoadData.setString(1, config.getDetailPathFile());
            psLoadData.execute();
            //(Extract to Staging)18. Thêm thông tin đang load dữ liệu vào staging vào log
            dao.insertLog(connection, config.getId(), "EXTRACTING", "Load data to staging");
            System.out.println("Load staging success");
            //(Extract to Staging)19. Cập nhật status của config thành EXTRACTED
            dao.updateStatus(connection, config.getId(), "EXTRACTED");
            //(Extract to Staging)20. Thêm thông tin đã load dữ liệu vào staging vào log
            dao.insertLog(connection, config.getId(), "EXTRACTED", "Load data success");
            //
            transformData(connection, config);

        } catch (SQLException e) {
            e.printStackTrace();
            dao.updateStatus(connection, config.getId(), "ERROR");
            dao.insertLog(connection, config.getId(), "ERROR", "Error with message: "+e.getMessage());
            dao.setFlagIsZero(connection, config.getId());
            dao.updateIsProcessing(connection, config.getId(), false);
            String mail = config.getEmail();
            DateTimeFormatter dt = DateTimeFormatter.ofPattern("hh:mm:ss dd/MM/yyyy");
            LocalDateTime nowTime = LocalDateTime.now();
            String timeNow = nowTime.format(dt);
            String subject = "Error Date: " + timeNow;
            String message = "Error with message: "+e.getMessage();
            String pathLogs = createFIleLog(dao.getLogs(connection, config.getId()));
            if(pathLogs!=null){
                SendMail.sendMail(mail, subject, message, pathLogs);
            }
            else SendMail.sendMail(mail, subject, message);
        }
    }

    public static void truncateTable(Connection connection, Config config) {
        ForecastResultsDao dao = new ForecastResultsDao();
        try (CallableStatement callableStatement = connection.prepareCall("{CALL truncate_staging_table()}")) {
            callableStatement.execute();
            dao.insertLog(connection, config.getId(), "EXTRACTING", "Truncate success");
        } catch (SQLException e) {
            e.printStackTrace();
            dao.insertLog(connection, config.getId(), "ERROR", "Error with message: "+e.getMessage());
            String mail = config.getEmail();
            DateTimeFormatter dt = DateTimeFormatter.ofPattern("hh:mm:ss dd/MM/yyyy");
            LocalDateTime nowTime = LocalDateTime.now();
            String timeNow = nowTime.format(dt);
            String subject = "Error Date: " + timeNow;
            String message = "Error with message: "+e.getMessage();
            String pathLogs = createFIleLog(dao.getLogs(connection, config.getId()));
            if(pathLogs!=null){
                SendMail.sendMail(mail, subject, message, pathLogs);
            }
            else SendMail.sendMail(mail, subject, message);
        }
    }

        /**
     * Converts a string containing city names into a list of strings.
     *
     * @param cities A string containing city names, separated by commas.
     * @return A list of strings representing the city names after removing leading and trailing spaces from each string.
     *         Returns an empty list if the input string is null or doesn't contain any cities.
     * @throws NullPointerException If the input is null.
     */
    private static List<String> convertCities(String cities){
        // Split the string into an array of strings, trim each string, and then collect into a list
        return Arrays.stream(cities.split(",")).map(String::trim).collect(Collectors.toList());
    }

    public static void transformData(Connection connection, Config config){
        ForecastResultsDao dao = new ForecastResultsDao();
        dao.updateIsProcessing(connection, config.getId(), true);
        //(Transform Data)13. Cập nhật status của config thành TRANSFORMING (status=TRANSFORMING)
        dao.updateStatus(connection, config.getId(), "TRANSFORMING");
        //(Transform Data)14. Thêm thông tin đang transform vào log
        dao.insertLog(connection, config.getId(), "TRANSFORMING", "Start transform");
        //(Transform Data)15. Transform Data
        try (CallableStatement callableStatement = connection.prepareCall("{CALL TransformData()}")) {
            // Thực hiện stored procedure
            callableStatement.execute();
            //(Transform Data)16. Cập nhật status của config thành TRANSFORMED
            dao.updateStatus(connection, config.getId(), "TRANSFORMED");
            //(Transform Data)17. Thêm thông tin đã transform data vào log
            dao.insertLog(connection, config.getId(), "TRANSFORMED", "Transform success");
            System.out.println("transform success!");
            //(Transform Data)18. Gọi function loadToWH(connect, config)
            loadToWH(connection, config);
        } catch (SQLException e) {
            // Xử lý lỗi khi thực hiện stored procedure
            e.printStackTrace();
            //(Transform Data)19. Cập nhật status của config thành ERROR
            dao.updateStatus(connection, config.getId(), "ERROR");
            //(Transform Data)20. Thêm lỗi vào log
            dao.insertLog(connection, config.getId(), "ERROR", "Error with message: "+e.getMessage());
            //(Transform Data)21. Chỉnh Flag=0 cho config
            dao.setFlagIsZero(connection, config.getId());
            //(Transform Data)22. Cập nhật trạng thái của config là không xử lý (isProcessing=false)
            dao.updateIsProcessing(connection, config.getId(), false);
            //(Transform Data)23. Send mail thông báo lỗi cho email của author
            String mail = config.getEmail();
            DateTimeFormatter dt = DateTimeFormatter.ofPattern("hh:mm:ss dd/MM/yyyy");
            LocalDateTime nowTime = LocalDateTime.now();
            String timeNow = nowTime.format(dt);
            String subject = "Error Date: " + timeNow;
            String message = "Error with message: "+e.getMessage();
            String pathLogs = createFIleLog(dao.getLogs(connection, config.getId()));
            if(pathLogs!=null){
                SendMail.sendMail(mail, subject, message, pathLogs);
            }
            else SendMail.sendMail(mail, subject, message);
        }
    }

    public static void loadToWH(Connection connection, Config config){
        ForecastResultsDao dao = new ForecastResultsDao();
        //(Load To WH)12. Cập nhật  trạng thái của config là đang xử lý (isProcessing=true)
        dao.updateIsProcessing(connection, config.getId(), true);
        //(Load To WH)13. Cập nhật status của config thành WH_LOADING (status=WH_LOADING)
        dao.updateStatus(connection, config.getId(), "WH_LOADING");
        //(Load To WH)14. Thêm thông tin bắt đầu load to wh vào log
        dao.insertLog(connection, config.getId(), "WH_LOADING", "Start load data to Warehouse");
        //(Load To WH)15. Load Data To WH
        try (CallableStatement callableStatement = connection.prepareCall("{CALL LoadDataToWH()}")) {
            // Thực hiện stored procedure
            callableStatement.execute();
            //(Load To WH)16. Cập nhật status của config thành WH_LOADED
            dao.updateStatus(connection, config.getId(), "WH_LOADED");
            //(Load To WH)17. Thêm thông tin đã load data to WH vào log
            dao.insertLog(connection, config.getId(), "WH_LOADED", "Load to warehouse success");
            System.out.println("load to warehouse success!");
            //(Load To WH)18. Gọi function loadToAggregate(connect, config)
            loadToAggregate(connection, config);
        } catch (SQLException e) {
            // Xử lý lỗi khi thực hiện stored procedure
            e.printStackTrace();
            //(Load To WH)19. Cập nhật status của config thành ERROR
            dao.updateStatus(connection, config.getId(), "ERROR");
            //(Load To WH)20. Thêm lỗi vào log
            dao.insertLog(connection, config.getId(), "ERROR", "Error with message: "+e.getMessage());
            //(Load To WH)21. Chỉnh Flag=0 cho config
            dao.setFlagIsZero(connection, config.getId());
            //(Load To WH)22. Cập nhật trạng thái của config là không xử lý (isProcessing=false)
            dao.updateIsProcessing(connection, config.getId(), false);
            //(Load To WH)23. Send mail thông báo lỗi cho email của author
            //send mail
            String mail = config.getEmail();
            DateTimeFormatter dt = DateTimeFormatter.ofPattern("hh:mm:ss dd/MM/yyyy");
            LocalDateTime nowTime = LocalDateTime.now();
            String timeNow = nowTime.format(dt);
            String subject = "Error Date: " + timeNow;
            String message = "Error with message: "+e.getMessage();
            String pathLogs = createFIleLog(dao.getLogs(connection, config.getId()));
            if(pathLogs!=null){
                SendMail.sendMail(mail, subject, message, pathLogs);
            }
            else SendMail.sendMail(mail, subject, message);
        }
    }

    public static void loadToAggregate(Connection connection, Config config){
        ForecastResultsDao dao = new ForecastResultsDao();
        //(Load To Aggregate)12. Cập nhật  trạng thái của config là đang xử lý (isProcessing=true)
        dao.updateIsProcessing(connection, config.getId(), true);
        //(Load To Aggregate)13. Cập nhật status của config thành AGGREGATING (status=AGGREGATING)
        dao.updateStatus(connection, config.getId(), "AGGREGATING");
        //(Load To Aggregate)14. Thêm thông tin bắt đầu load to aggregate vào log
        dao.insertLog(connection, config.getId(), "AGGREGATING", "Start aggregate");
        //(Load To Aggregate)15. Load Data To Aggregate
        try(CallableStatement callableStatement = connection.prepareCall("{CALL LoadDataToAggregate()}")){
            callableStatement.execute();
            //(Load To Aggregate)16. Cập nhật status của config thành AGGREGATED
            dao.updateStatus(connection, config.getId(),"AGGREGATED");
            //(Load To Aggregate)17. Thêm thông tin đã load data to aggregate vào log
            dao.insertLog(connection, config.getId(), "AGGREGATED", "Load aggregate success");
            System.out.println("aggregate success!");
            //(Load To Aggregate)18. Gọi function loadToDataMart(connect, config)
            loadToDataMart(connection, config);
        }catch (SQLException e) {
            e.printStackTrace();
            //(Load To Aggregate)19. Cập nhật status của config thành ERROR
            dao.updateStatus(connection, config.getId(), "ERROR");
            //(Load To Aggregate)20. Thêm lỗi vào log
            dao.insertLog(connection, config.getId(), "ERROR", "Error with message: "+e.getMessage());
            //(Load To Aggregate)21. Chỉnh Flag=0 cho config
            dao.setFlagIsZero(connection, config.getId());
            //(Load To Aggregate)22. Cập nhật trạng thái của config là không xử lý (isProcessing=false)
            dao.updateIsProcessing(connection, config.getId(), false);
            //(Load To Aggregate)23. Send mail thông báo lỗi cho email của author
            //send mail
            String mail = config.getEmail();
            DateTimeFormatter dt = DateTimeFormatter.ofPattern("hh:mm:ss dd/MM/yyyy");
            LocalDateTime nowTime = LocalDateTime.now();
            String timeNow = nowTime.format(dt);
            String subject = "Error Date: " + timeNow;
            String message = "Error with message: "+e.getMessage();
            String pathLogs = createFIleLog(dao.getLogs(connection, config.getId()));
            if(pathLogs!=null){
                SendMail.sendMail(mail, subject, message, pathLogs);
            }
            else SendMail.sendMail(mail, subject, message);
        }
    }

    public static void loadToDataMart(Connection connection, Config config){
        ForecastResultsDao dao = new ForecastResultsDao();
        //(Load To DataMart)12. Cập nhật  trạng thái của config là đang xử lý (isProcessing=true)
        dao.updateIsProcessing(connection, config.getId(), true);
        //(Load To DataMart)13. Cập nhật status của config thành MLOADING (status=MLOADING)
        dao.updateStatus(connection, config.getId(), "MLOADING");
        //(Load To DataMart)14. Thêm thông tin bắt đầu load to datamart vào log
        dao.insertLog(connection, config.getId(), "MLOADING", "Start load data to DataMart");
        //(Load To DataMart)15. Load Data To DataMart
        try(CallableStatement callableStatement = connection.prepareCall("{CALL LoadToDM()}")){
            callableStatement.execute();
            //(Load To DataMart)16. Cập nhật status của config thành MLOADED
            dao.updateStatus(connection, config.getId(), "MLOADED");
            //(Load To DataMart)17. Thêm thông tin đã load data to datamart vào log
            dao.insertLog(connection, config.getId(), "MLOADED", "Load to mart success");
            System.out.println("load to mart success!");
            //finish
            //(Load To DataMart)18. Cập nhật status của config thành FINISHED
            dao.updateStatus(connection, config.getId(), "FINISHED");
            //(Load To DataMart)19. Thêm thông tin đã hoàn thành tiến trình vào log
            dao.insertLog(connection, config.getId(), "FINISHED", "Finished!");
            //(Load To DataMart)20. Chỉnh Flag=0 cho config
            dao.setFlagIsZero(connection, config.getId());
            //(Load To DataMart)21. Cập nhật trạng thái của config là không xử lý (isProcessing=false)
            dao.updateIsProcessing(connection, config.getId(), false);
            //(Load To DataMart)22. Send mail thông báo tiến trình hoàn tất cho email của author
            //send mail khi đã hoàn thành việc lấy data load vào warehouse
            String mail = config.getEmail();
            DateTimeFormatter dt = DateTimeFormatter.ofPattern("hh:mm:ss dd/MM/yyyy");
            LocalDateTime nowTime = LocalDateTime.now();
            String timeNow = nowTime.format(dt);
            String subject = "Success DataWarehouse Date: " + timeNow;
            String message = "Success";
            String pathLogs = createFIleLog(dao.getLogs(connection, config.getId()));
            if(pathLogs!=null){
                SendMail.sendMail(mail, subject, message, pathLogs);
            }
            else SendMail.sendMail(mail, subject, message);
        }catch (SQLException e){
            e.printStackTrace();
            //(Load To DataMart)23. Cập nhật status của config thành ERROR
            dao.updateStatus(connection, config.getId(), "ERROR");
            //(Load To DataMart)24. Thêm lỗi vào log
            dao.insertLog(connection, config.getId(), "ERROR", "Error with message: "+e.getMessage());
            //(Load To DataMart)25. Chỉnh Flag=0 cho config
            dao.setFlagIsZero(connection, config.getId());
            //(Load To DataMart)26. Cập nhật trạng thái của config là không xử lý (isProcessing=false)
            dao.updateIsProcessing(connection, config.getId(), false);
            //(Load To DataMart)27. Send mail thông báo lỗi cho email của author
            //send mail
            String mail = config.getEmail();
            DateTimeFormatter dt = DateTimeFormatter.ofPattern("hh:mm:ss dd/MM/yyyy");
            LocalDateTime nowTime = LocalDateTime.now();
            String timeNow = nowTime.format(dt);
            String subject = "Error Date: " + timeNow;
            String message = "Error with message: "+e.getMessage();
            String pathLogs = createFIleLog(dao.getLogs(connection, config.getId()));
            if(pathLogs!=null){
                SendMail.sendMail(mail, subject, message, pathLogs);
            }
            else SendMail.sendMail(mail, subject, message);
        }
    }

}
