-- Tạo database controller 
DROP DATABASE IF EXISTS controller;
CREATE DATABASE controller character set utf8;
USE controller;

-- Tạo table config 
DROP TABLE IF EXISTS config;
CREATE TABLE config(
	id INT PRIMARY KEY AUTO_INCREMENT,
	author VARCHAR(255) NULL DEFAULT NULL,
	email VARCHAR(255) NULL DEFAULT NULL,
	filename VARCHAR(255) NULL DEFAULT 'Test',
	directory_file VARCHAR(510) NULL DEFAULT 'D:\\',
	status_config VARCHAR(255) NULL DEFAULT 'OFF',
	detail_file_path VARCHAR(255) NULL DEFAULT NULL,
	flag bit(1) NULL DEFAULT 0,
	finish_at DATETIME NULL DEFAULT NULL,
	created_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
	update_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
	is_processing bit(1) null default 0
);

-- Tạo trigger khi thay đổi dữ liệu trong config thì sẽ cập nhật lại update_at = thời gian hiện tại
DROP TRIGGER IF EXISTS update_update_at_trigger;
CREATE TRIGGER update_update_at_trigger
BEFORE UPDATE ON config
FOR EACH ROW
BEGIN
    IF NEW.author != OLD.author
        OR NEW.email != OLD.email
        OR NEW.filename != OLD.filename
        OR NEW.directory_file != OLD.directory_file
        OR NEW.flag != OLD.flag
    THEN
        SET NEW.update_at = CURRENT_TIMESTAMP();
    END IF;
END;

-- Tạo table log
drop table if exists log;
create table log(
		id int primary key auto_increment,
		id_config int ,
		status varchar(100) null default null,
		description varchar(255) null default null,
		created_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
		FOREIGN KEY (id_config) REFERENCES config(id) ON DELETE RESTRICT ON UPDATE RESTRICT); 
		-- khóa chính của config(id) là khóa ngoại(id_config) của log

-- Tạo procedure thêm dữ liệu vào log với tham số id_config, status, description
drop procedure if EXISTS InsertLog;
create procedure InsertLog(
	id_config int,
	status varchar(100),
	description varchar(255)
)
BEGIN
	insert into log(id_config, status, description)
	values (id_config, status, description);
end;

-- Tạo procedure thay đổi status của config có id là input_id và status = input_status
DROP PROCEDURE IF EXISTS UpdateStatus;
CREATE PROCEDURE UpdateStatus(
    IN input_id INT,
    IN input_status VARCHAR(255)
)
BEGIN
    UPDATE config
    SET status_config = input_status, finish_at = CURRENT_TIMESTAMP()
    WHERE id = input_id;
END;

-- Tạo procedure cập nhật giá trị của path file detail trong config có id là input_id và detail_file_path = input_pathfile
DROP PROCEDURE IF EXISTS UpdatePathFileDetail;
CREATE PROCEDURE UpdatePathFileDetail(
    IN input_id INT,
    IN input_pathfile VARCHAR(255)
)
BEGIN
    UPDATE config
		SET detail_file_path = input_pathfile
    WHERE id = input_id;
END;

-- Tạo procedure cập nhật giá trị của is_processing trong config có id là input_id và is_processing = input_is_processing
DROP PROCEDURE IF EXISTS UpdateIsProcessing;
CREATE PROCEDURE UpdateIsProcessing(
    IN input_id INT,
    IN input_is_processing bit(1)
)
BEGIN
    UPDATE config
		SET is_processing = input_is_processing
    WHERE id = input_id;
END;

-- Tạo procedure thay đổi giá trị flag = 0 trong config có id là input_id
DROP PROCEDURE IF EXISTS SetFlagIsZero;
CREATE PROCEDURE SetFlagIsZero(
    IN input_id INT
)
BEGIN
    UPDATE config
		SET flag = 0
    WHERE id = input_id;
END;

-- Tạo procedure truncate dữ liệu của table staging trong database staging 
DROP PROCEDURE IF EXISTS truncate_staging_table;
CREATE PROCEDURE truncate_staging_table()
BEGIN
    TRUNCATE table staging.staging;
END;

-- Tạo procedure TransformData call các procedure của database staging để transform dữ liệu 
DROP PROCEDURE IF EXISTS TransformData;
CREATE PROCEDURE TransformData()
BEGIN
		-- transform date
    call staging.TransformDate();
		-- transform time
		call staging.TransformTime();
		-- transform city
		call staging.TransformCity();
		-- transform weather
		call staging.TransformWeather();
END;

-- Tạo procedure LoadDataToWH lấy các thuộc tính từ staging và các id của bảng dim của db staging load vào table fact trong database warehouse
DROP PROCEDURE IF EXISTS LoadDataToWH;
CREATE PROCEDURE LoadDataToWH()
BEGIN
		call warehouse.SetDataExpired();
		INSERT INTO warehouse.fact (
			id_city,
			id_time,
			id_date,
			id_weather,
			city_sunset,
			city_sunrise,
			main_temp,
			main_feels_like,
			main_temp_min,
			main_temp_max,
			main_pressure,
			main_grnd_level,
			main_humidity,
			main_temp_kf,
			clouds_all,
			wind_speed,
			wind_deg,
			wind_gust,
			visibility,
			pop,
			rain_3h,
			sys,
			dtChanged)
		SELECT staging._city, staging._time, staging._date, staging._weather, cast(city_sunset as int) , cast(city_sunrise as int), cast(main_temp as double), cast(main_feels_like as double), cast(main_temp_min as double), cast(main_temp_max as double), cast(main_pressure as int), cast(main_grnd_level as int), cast(main_humidity as int), cast(main_temp_kf as double), cast(clouds_all as int), cast(wind_speed as double), cast(wind_deg as int), cast(wind_gust as double), cast(visibility as int), cast(pop as double), cast(rain_3h as double), sys_pod, cast(created_date as datetime)
		FROM staging.staging;
END;

-- Tạo procedure LoadDataToAggregate để tổng hợp dữ liệu từ bảng dim và các bảng fact vào table forecast_results trong db warehouse 
drop procedure if exists LoadDataToAggregate;
create procedure LoadDataToAggregate()
BEGIN
	-- truncate dữ liệu hiện tại trong forecast_results để thêm dữ liệu mới vào
	truncate table warehouse.forecast_results;
	insert into warehouse.forecast_results(id, date_of_week, date_forecast, time_forecast, city_name, main_temp, main_pressure, main_humidity, clouds_all, wind_speed, visibility, rain_3h, weather_description, weather_icon)
	select 
		fact.id_fact,
		_date.day_of_week, 
		_date.full_date,
		_time.full_time,
		_city.city_name,
		fact.main_temp,
		fact.main_pressure,
		fact.main_humidity,
		fact.clouds_all,
		fact.wind_speed,
		fact.visibility,
		fact.rain_3h,
		_weather.weather_description,
		_weather.weather_icon
	from 
		warehouse.fact as fact
		join warehouse.date_dim as _date on fact.id_date = _date.date_sk
		join warehouse.time_dim as _time on fact.id_time = _time.time_sk
		join warehouse.city_dim as _city on fact.id_city = _city.id 
		join warehouse.weather_dim as _weather on fact.id_weather = _weather.id
	WHERE
		fact.dtExpired > now(); -- lấy các dữ liệu còn thời hạn để load vào table forecast_results
END;

-- Tạo procedure SwapForecastTables để rename giữa table forecast và forecast_temp trong database datamart
DROP PROCEDURE IF EXISTS SwapForecastTables;
CREATE PROCEDURE SwapForecastTables()
BEGIN
    -- Tạo một bảng tạm thời để lưu trữ tên bảng cũ
    CREATE TEMPORARY TABLE IF NOT EXISTS TempTableNames (
        old_table_name VARCHAR(50),
        new_table_name VARCHAR(50)
    );

    -- Đổi tên của các bảng trong datamart
    RENAME TABLE datamart.forecast TO TempTableNames,
                 datamart.forecast_temp TO datamart.forecast,
                 TempTableNames TO datamart.forecast_temp;
    -- Xóa bảng tạm thời
    DROP TEMPORARY TABLE IF EXISTS TempTableNames;
END;

-- Tạo procedure LoadToDM() để đưa dữ liệu từ table forecast_results của db warehouse sang table forecast_temp của datamart, sau đó đổi tên 2 table để report connect với dữ liệu mới nhất khi reconnect
drop procedure if exists LoadToDM;
create procedure LoadToDM()
BEGIN
	-- Truncate dữ liệu trong forecast_temp để thêm dữ liệu mới vào
	truncate table datamart.forecast_temp;
	insert into datamart.forecast_temp(id, date_of_week, date_forecast, time_forecast, city_name, main_temp, main_pressure, main_humidity,
	clouds_all, wind_speed, visibility, rain_3h, weather_description, weather_icon)
	select * from warehouse.forecast_results;
	-- Đổi tên 2 table
	call SwapForecastTables();
end;
